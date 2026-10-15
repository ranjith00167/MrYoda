package api.phlebio;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stage-aware integrity checks for the phlebo order flow: status progression, null / not-null rules,
 * cross-field consistency (money, payment, slot, address, LIS) and chronology.
 *
 * Hard checks ({@code check}/{@code eq}) fail the step. Anything that is a confirmed inconsistency of the backend
 * itself (stale status / timestamps, fields never stamped, data outside the shift) goes through
 * {@link FieldChecks#defect} so it is reported as [DEFECT] without blocking the flow.
 */
public final class PhlebioIntegrityValidator {

    /** Point of the flow at which an order / tracking payload was read. */
    public enum Stage { ASSIGNED, IN_PROGRESS, OTP_VERIFIED, SAMPLES_COLLECTED }

    private static final ZoneOffset IST = ZoneOffset.ofHoursMinutes(5, 30);
    private static final DateTimeFormatter SLOT_LABEL = new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern("dd-MM-yyyy hh:mm a").toFormatter(Locale.ENGLISH);
    private static final long CLOCK_SKEW_SECONDS = 120;

    private static final List<String> REQUIRED_ORDER_FIELDS = Arrays.asList(
            "guid", "order_number", "order_sample_number", "user_id", "brand_id", "address_id", "payment_id",
            "slot_guid", "slot_start_time", "slot_end_time", "lab_location_id", "order_created_at", "created_at",
            "phlebo_id", "phlebo_otp", "phlebo_assigned_at", "order_status", "display_order_status", "status",
            "payment_type", "order_type");

    private static final List<String> IDENTITY_FIELDS = Arrays.asList(
            "guid", "user_id", "phlebo_id", "phlebo_otp", "phlebo_assigned_at", "order_status", "slot_start_time",
            "slot_end_time");

    private static final List<String> LIFECYCLE_MUST_BE_NULL = Arrays.asList(
            "cancelled_date", "cancelled_by", "cancel_order_remarks", "cancelled_remarks", "cancelled_by_user_at",
            "completed_date", "completed_by", "received_at", "deleted_at",
            "admin_approval_by", "admin_approval_at", "delivered_hub_id", "delivered_hub_at", "delivered_lab_id",
            "delivered_lab_at", "sample_delivered_image", "lab_delivery_image", "cash_collected_at",
            "cash_collected_amount", "report_url");

    private static final List<String> TRACKING_MUST_BE_NULL = Arrays.asList(
            "refusal_reason", "verifyOtpRemarks", "received_by", "cash_collected_at", "cash_collected_amount",
            "delivered_hub_id", "delivered_hub_at", "sample_delivered_image", "delivered_lab_id", "delivered_lab_at",
            "lab_delivery_image", "user_nearby_center", "deleted_at", "force_closure_details");

    private static final Set<String> AHEAD_OF_COLLECTION = new HashSet<>(Arrays.asList(
            "sample collected", "sample received", "sample delivered", "department recieved", "department received",
            "report generated", "report approved", "completed", "cancelled"));

    private static final Set<String> TRACKING_VOCABULARY = new HashSet<>(Arrays.asList(
            "open", "inprogress", "samples_collected"));

    private PhlebioIntegrityValidator() {
    }

    // ── response envelope ───────────────────────────────────────────────────

    /** body.status must mirror the HTTP status, msg must equal message, and no error marker may be present. */
    public static void checkEnvelope(FieldChecks c, Response resp) {
        JsonPath j = resp.jsonPath();
        Object bodyStatus = j.get("status");
        if (bodyStatus instanceof Number) {
            c.eq("envelope: body.status == HTTP status", resp.getStatusCode(), ((Number) bodyStatus).intValue());
        }
        String msg = j.getString("msg");
        String message = j.getString("message");
        if (msg != null && message != null) {
            c.eq("envelope: body.msg == body.message", message, msg);
        }
        String error = j.getString("error");
        c.check("envelope: body.error absent", error == null || "false".equalsIgnoreCase(error)
                || "{}".equals(error), "absent", "error marker present: " + error);
    }

    // ── tracking row (assignOrder.orderTracking / updateOrderTracking.data[0]) ──

    public static void checkTrackingRow(FieldChecks c, JsonPath j, String path, Stage stage, String orderAddressId) {
        Map<String, Object> t = j.getMap(path);
        if (t == null) {
            c.check("tracking row present", false, "", "no tracking object at " + path);
            return;
        }
        List<String> dirty = new ArrayList<>();
        for (String f : TRACKING_MUST_BE_NULL) {
            if (!isBlank(t.get(f))) {
                dirty.add(f + "=" + t.get(f));
            }
        }
        c.check("tracking: refusal/received/delivery/cash/force-closure fields are null", dirty.isEmpty(),
                "all null", "unexpectedly set: " + dirty);
        c.check("tracking: samples_submitted & samples_refused are empty",
                isBlank(t.get("samples_submitted")) && isBlank(t.get("samples_refused")), "empty",
                "submitted=" + t.get("samples_submitted") + " refused=" + t.get("samples_refused"));
        if (t.get("id") != null) {
            c.check("tracking: id is a positive number", num(t.get("id")) > 0, "id=" + t.get("id"),
                    "id must be > 0 but was " + t.get("id"));
        }
        c.eq("tracking: address_id == order.address_id", orderAddressId, t.get("address_id"));

        List<Map<String, Object>> statuses = j.getList(path + ".status");
        List<String> names = new ArrayList<>();
        boolean complete = statuses != null && !statuses.isEmpty();
        if (complete) {
            for (Map<String, Object> s : statuses) {
                String name = str(s.get("status"));
                complete &= name != null && instant(str(s.get("time"))) != null;
                names.add(name);
            }
        }
        c.check("tracking: every status entry has a status and a valid time", complete, "complete",
                "incomplete entries in " + statuses);
        c.check("tracking: status entries are unique", new HashSet<>(names).size() == names.size(),
                names.toString(), "duplicate statuses " + names);
        c.defect("tracking: status names are in the known vocabulary " + TRACKING_VOCABULARY,
                TRACKING_VOCABULARY.containsAll(names), "unknown statuses " + names);

        Instant first = complete ? instant(str(statuses.get(0).get("time"))) : null;
        Instant last = complete ? instant(str(statuses.get(statuses.size() - 1).get("time"))) : null;
        Instant assigned = instant(str(t.get("assigned_date")));
        Instant created = instant(str(t.get("created_at")));
        if (first != null && assigned != null) {
            c.check("tracking: assigned_date ~= status[0].time", within(assigned, first, 5_000), assigned.toString(),
                    "assigned_date " + assigned + " vs first status " + first);
        }
        if (first != null && created != null) {
            c.check("tracking: created_at ~= status[0].time", within(created, first, 5_000), created.toString(),
                    "created_at " + created + " vs first status " + first);
        }
        Instant updated = instant(str(t.get("updated_at")));
        if (updated != null && last != null) {
            c.defect("tracking: updated_at refreshed on each status change", !updated.isBefore(last),
                    "updated_at " + updated + " is older than the latest status change " + last);
        }

        boolean collected = stage == Stage.SAMPLES_COLLECTED;
        boolean otpDone = stage.ordinal() >= Stage.OTP_VERIFIED.ordinal();
        if (t.containsKey("otp_verified")) {
            c.eq("tracking: otp_verified for stage " + stage, otpDone, t.get("otp_verified"));
        }
        if (collected) {
            c.notBlank("tracking: barcode_id after collection", t.get("barcode_id"));
            c.notBlank("tracking: phlebo_selfie_url after collection", t.get("phlebo_selfie_url"));
            c.check("tracking: samples_picked non-empty after collection", !isBlank(t.get("samples_picked")),
                    "present", "samples_picked is empty");
        } else {
            c.check("tracking: no barcode / selfie / samples before collection",
                    isBlank(t.get("barcode_id")) && isBlank(t.get("phlebo_selfie_url"))
                            && isBlank(t.get("samples_picked")), "none set",
                    "barcode=" + t.get("barcode_id") + " selfie=" + t.get("phlebo_selfie_url")
                            + " samples=" + t.get("samples_picked"));
        }
    }

    // ── order payload (getOrderById) ────────────────────────────────────────

    public static void checkOrder(FieldChecks c, JsonPath o, String root, Stage stage) {
        Map<String, Object> m = o.getMap(root);
        if (m == null) {
            c.check("order payload present", false, "", "no order object at " + root);
            return;
        }
        Object bodyStatus = o.get("status");
        if (bodyStatus instanceof Number) {
            c.eq("envelope: getOrderById body.status", 200, ((Number) bodyStatus).intValue());
        }
        if (o.getString("message") != null && o.getString("msg") != null) {
            c.eq("envelope: getOrderById msg == message", o.getString("message"), o.getString("msg"));
        }

        checkRequiredAndNulls(c, m, stage);
        checkStatusProgression(c, m, stage);
        checkTimeline(c, m, stage);
        checkMoney(c, m);
        checkPayment(c, m);
        checkAmountSnapshot(c, m);
        checkSlotAndAddress(c, m);
        checkLis(c, m, stage);
    }

    /** Wrong-assignment detection: phlebo zone and shift window must fit the order. */
    public static void checkAssignmentFit(FieldChecks c, JsonPath o, String root, String phleboZoneId,
                                          String shiftStart, String shiftEnd) {
        Map<String, Object> m = o.getMap(root);
        if (m == null) {
            return;
        }
        String orderZone = str(map(m, "user_nearby_center").get("zoneId"));
        if (orderZone == null) {
            orderZone = str(m.get("lab_location_id"));
        }
        c.defect("assignment: phlebo zone == order zone", phleboZoneId == null || phleboZoneId.equals(orderZone),
                "phlebo zoneId [" + phleboZoneId + "] differs from order zone [" + orderZone + "]");

        String start = clock(str(m.get("slot_start_time")));
        String end = clock(str(m.get("slot_end_time")));
        if (start != null && end != null && shiftStart != null && shiftEnd != null) {
            c.defect("assignment: order slot inside phlebo shift " + shiftStart + "-" + shiftEnd,
                    start.compareTo(shiftStart) >= 0 && end.compareTo(shiftEnd) <= 0,
                    "slot " + start + "-" + end + " is outside the phlebo shift " + shiftStart + "-" + shiftEnd);
        }
    }

    /** The union of item sample types must equal the samples the phlebo reported as collected. */
    public static void checkSampleCoverage(FieldChecks c, JsonPath o, String root,
                                           List<Map<String, Object>> samplesSent) {
        Set<String> expected = new TreeSet<>();
        List<Map<String, Object>> items = o.getList(root + ".order_items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                Object types = item.get("sample_types");
                if (types instanceof List) {
                    for (Object t : (List<?>) types) {
                        if (t instanceof Map && ((Map<?, ?>) t).get("DefaultSampleType") != null) {
                            expected.add(normaliseType(((Map<?, ?>) t).get("DefaultSampleType")));
                        }
                    }
                }
            }
        }
        Set<String> sent = new TreeSet<>();
        boolean quantitiesOk = samplesSent != null && !samplesSent.isEmpty();
        if (samplesSent != null) {
            for (Map<String, Object> s : samplesSent) {
                sent.add(normaliseType(s.get("sample_type")));
                quantitiesOk &= num(s.get("quantity")) > 0;
            }
        }
        if (expected.isEmpty()) {
            c.note("sample coverage skipped - no item exposes DefaultSampleType");
        } else {
            c.eq("samples_collected types == union of item DefaultSampleType", expected.toString(), sent.toString());
        }
        c.check("samples_collected quantities are all > 0", quantitiesOk, "all > 0", "bad quantities in " + samplesSent);
    }

    // ── order sections ──────────────────────────────────────────────────────

    private static void checkRequiredAndNulls(FieldChecks c, Map<String, Object> m, Stage stage) {
        // payment_type (Online / Cash / ...) is only known once the payment is approved, so it is not required before.
        boolean paymentApproved = "Success".equalsIgnoreCase(str(map(m, "payment").get("payment_status")))
                || "Success".equalsIgnoreCase(str(m.get("payment_statuses")));
        List<String> missing = new ArrayList<>();
        for (String f : REQUIRED_ORDER_FIELDS) {
            if ("payment_type".equals(f) && !paymentApproved) {
                continue;
            }
            if (isBlank(m.get(f))) {
                missing.add(f);
            }
        }
        List<String> missingIdentity = new ArrayList<>(missing);
        missingIdentity.retainAll(IDENTITY_FIELDS);
        c.check("order: identity fields are non-null", missingIdentity.isEmpty(), IDENTITY_FIELDS.size() + " present",
                "missing/null: " + missingIdentity);
        c.defect("null: other required order fields are populated", missing.size() == missingIdentity.size(),
                "missing/null: " + missing);

        List<String> dirty = new ArrayList<>();
        for (String f : LIFECYCLE_MUST_BE_NULL) {
            if (!isBlank(m.get(f))) {
                dirty.add(f + "=" + m.get(f));
            }
        }
        c.check("order: cancel/complete/delivery/report fields are null", dirty.isEmpty(), "all null",
                "unexpectedly set: " + dirty);

        List<String> artefacts = new ArrayList<>();
        if (num(m.get("reschedule_count")) != 0) {
            artefacts.add("reschedule_count=" + m.get("reschedule_count"));
        }
        if (num(m.get("refund_amount")) != 0) {
            artefacts.add("refund_amount=" + m.get("refund_amount"));
        }
        if (num(m.get("actual_refund_amount")) != 0) {
            artefacts.add("actual_refund_amount=" + m.get("actual_refund_amount"));
        }
        if (Boolean.TRUE.equals(m.get("cancel_delivery_deducted"))) {
            artefacts.add("cancel_delivery_deducted=true");
        }
        if (!isBlank(m.get("samples_submitted"))) {
            artefacts.add("samples_submitted=" + m.get("samples_submitted"));
        }
        if (!isBlank(m.get("samples_refused"))) {
            artefacts.add("samples_refused=" + m.get("samples_refused"));
        }
        c.check("order: no reschedule / refund / submitted / refused artefacts", artefacts.isEmpty(), "clean",
                "unexpected: " + artefacts);

        boolean collected = stage == Stage.SAMPLES_COLLECTED;
        if (collected) {
            List<String> absent = new ArrayList<>();
            for (String f : Arrays.asList("barcode_id", "phlebo_selfie_url", "samples_picked", "sample_collection_date")) {
                if (isBlank(m.get(f))) {
                    absent.add(f);
                }
            }
            c.check("order: collection artefacts present after collection", absent.isEmpty(), "all present",
                    "missing: " + absent);
        } else {
            List<String> early = new ArrayList<>();
            for (String f : Arrays.asList("barcode_id", "phlebo_selfie_url", "samples_picked", "consent_form_type",
                    "consent_form_images")) {
                if (!isBlank(m.get(f))) {
                    early.add(f + "=" + m.get(f));
                }
            }
            c.check("order: no barcode / selfie / samples / consent before collection", early.isEmpty(), "none set",
                    "already set: " + early);
            if (stage.ordinal() < Stage.OTP_VERIFIED.ordinal()) {
                c.check("order: sample_collection_date is null before OTP verification",
                        isBlank(m.get("sample_collection_date")), "null",
                        "set to " + m.get("sample_collection_date"));
            }
        }
    }

    private static void checkStatusProgression(FieldChecks c, Map<String, Object> m, Stage stage) {
        String orderStatus = str(m.get("order_status"));
        String itDoseStatus = str(m.get("it_dose_order_status"));
        boolean collected = stage == Stage.SAMPLES_COLLECTED;

        if (!collected) {
            c.check("status: order_status not ahead of stage " + stage, !isAhead(orderStatus), "[" + orderStatus + "]",
                    "order_status [" + orderStatus + "] is already past collection");
            c.check("status: it_dose_order_status not ahead of stage " + stage, !isAhead(itDoseStatus),
                    "[" + itDoseStatus + "]", "it_dose_order_status [" + itDoseStatus + "] is already past collection");
        }
        if (stage == Stage.ASSIGNED) {
            c.contains("status: order_status right after assignment", orderStatus, "assigned");
        }

        List<String> badItems = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        List<String> unstamped = new ArrayList<>();
        for (Map<String, Object> item : maps(m.get("order_items"))) {
            String label = str(item.get("order_item_number"));
            String itemStatus = str(item.get("status"));
            if ((itemStatus != null && !"ACTIVE".equalsIgnoreCase(itemStatus)) || !isBlank(item.get("cancelled_at"))
                    || !isBlank(item.get("deleted_at"))) {
                badItems.add(label);
            }
            if (!collected && (isAhead(str(item.get("order_status")))
                    || isAhead(str(item.get("it_dose_order_items_status"))))) {
                badItems.add(label + " ahead");
            }
            String itemItDose = str(item.get("it_dose_order_items_status"));
            if (collected && itemItDose != null && !"Sample Collected".equalsIgnoreCase(itemItDose)) {
                stale.add(label + "=" + itemItDose);
            }
            if (collected && (isBlank(item.get("sample_collected_at")) || isBlank(item.get("phlebotomist_assigned_at")))) {
                unstamped.add(label);
            }
        }
        c.check("status: items are ACTIVE, not cancelled/deleted, and not ahead of stage", badItems.isEmpty(),
                "all ok", "bad items: " + badItems);
        if (collected) {
            c.check("status: every item it_dose_order_items_status == Sample Collected", stale.isEmpty(), "all ok",
                    "stale item statuses: " + stale);
            c.defect("status: display_order_status reflects collection",
                    !"Order booked".equalsIgnoreCase(str(m.get("display_order_status"))),
                    "display_order_status is still [" + m.get("display_order_status") + "] while order_status is ["
                            + orderStatus + "] and status [" + m.get("status") + "]");
            c.defect("null: items sample_collected_at / phlebotomist_assigned_at stamped", unstamped.isEmpty(),
                    "null on items " + unstamped);
            c.defect("null: sample_collection_by recorded", !isBlank(m.get("sample_collection_by")),
                    "sample_collection_by is null after collection");
            c.defect("null: reached_home_at recorded", !isBlank(m.get("reached_home_at")),
                    "reached_home_at is null although phlebo went inprogress and collected samples");
        }
    }

    private static void checkTimeline(FieldChecks c, Map<String, Object> m, Stage stage) {
        Instant created = instant(str(m.get("order_created_at")));
        Instant assigned = instant(str(m.get("phlebo_assigned_at")));
        Instant updated = instant(str(m.get("updated_at")));
        Instant deadline = Instant.now().plusSeconds(CLOCK_SKEW_SECONDS);
        List<Map<String, Object>> tracking = maps(m.get("tracking_status"));
        Instant firstTrack = tracking.isEmpty() ? null : instant(str(tracking.get(0).get("time")));
        Instant lastTrack = tracking.isEmpty() ? null : instant(str(tracking.get(tracking.size() - 1).get("time")));

        c.check("timeline: order_created_at <= phlebo_assigned_at", created != null && assigned != null
                && !created.isAfter(assigned), created + " <= " + assigned,
                "created " + created + " after assigned " + assigned);
        if (firstTrack != null && assigned != null) {
            long gap = Math.abs(firstTrack.toEpochMilli() - assigned.toEpochMilli());
            c.check("timeline: tracking open time within 60s of phlebo_assigned_at", gap <= 60_000, gap + " ms",
                    "gap of " + gap + " ms between assignment and tracking row");
        }
        c.defect("timeline: no timestamp is in the future", notAfter(deadline, created, assigned, lastTrack),
                "future timestamp among created=" + created + " assigned=" + assigned
                        + " lastTracking=" + lastTrack);
        c.defect("timeline: order.updated_at refreshed by phlebo assignment",
                updated != null && assigned != null && !updated.isBefore(assigned),
                "updated_at " + updated + " is older than phlebo_assigned_at " + assigned);
        if (stage == Stage.SAMPLES_COLLECTED) {
            c.defect("timeline: order.updated_at refreshed by sample collection",
                    updated != null && lastTrack != null && !updated.isBefore(lastTrack),
                    "updated_at " + updated + " is older than the samples_collected tracking time " + lastTrack);
            Instant collectedAt = instant(str(m.get("sample_collection_date")));
            c.defect("timeline: sample_collection_date not before the samples_collected event",
                    collectedAt != null && lastTrack != null && !collectedAt.isBefore(lastTrack),
                    "sample_collection_date " + collectedAt + " precedes samples_collected time " + lastTrack);
        }
    }

    private static void checkMoney(FieldChecks c, Map<String, Object> m) {
        double paid = num(m.get("paid_amount"));
        double expected = num(m.get("final_price")) - num(m.get("membership_discount"))
                - num(m.get("coupon_discount_amount")) - num(m.get("adminDiscount"))
                - num(m.get("doctors_discount")) - num(m.get("inaugural_discount")) - num(m.get("rewards_used"))
                + num(m.get("delivery_charge")) + num(m.get("extra_charges"));
        c.defect("money: final_price - discounts + charges == paid_amount", Math.abs(expected - paid) < 0.01,
                "computed " + expected + " but paid_amount is " + paid);
        c.defect("money: net_amount == paid_amount", Math.abs(num(m.get("net_amount")) - paid) < 0.01,
                "net_amount " + m.get("net_amount") + " vs paid_amount " + paid);
        c.defect("money: total_discount == actual_discount", Math.abs(num(m.get("total_discount"))
                - num(m.get("actual_discount"))) < 0.01,
                "total_discount " + m.get("total_discount") + " vs actual_discount " + m.get("actual_discount"));
        c.check("money: total_price >= final_price and paid_amount >= 0", num(m.get("total_price"))
                >= num(m.get("final_price")) && paid >= 0, "monotonic",
                "total " + m.get("total_price") + " final " + m.get("final_price") + " paid " + paid);
        c.defect("money: membership_discount > 0 only for a prime member",
                num(m.get("membership_discount")) == 0 || Boolean.TRUE.equals(m.get("is_prime_member")),
                "membership_discount " + m.get("membership_discount") + " but is_prime_member = "
                        + m.get("is_prime_member"));
    }

    private static void checkPayment(FieldChecks c, Map<String, Object> m) {
        Map<String, Object> p = map(m, "payment");
        if (p.isEmpty()) {
            c.note("payment block absent in the order payload - payment checks skipped");
            return;
        }
        boolean paymentApproved = "Success".equalsIgnoreCase(str(p.get("payment_status")));
        if (!paymentApproved) {
            c.note("payment not approved yet (payment_status [" + p.get("payment_status")
                    + "]): order-level payment_id / payment_type / payment_statuses may still be empty "
                    + "(pay-after-collection flow); they are compared only when present");
        }
        checkPaymentLink(c, "payment.guid == payment_id", m.get("payment_id"), p.get("guid"), paymentApproved);
        checkPaymentLink(c, "payment.payment_type == payment_type", m.get("payment_type"),
                p.get("payment_type"), paymentApproved);
        if (paymentApproved || !isBlank(m.get("payment_statuses"))) {
            c.eqIgnoreCase("payment: payment_statuses == payment.payment_status", p.get("payment_status"),
                    m.get("payment_statuses"));
        }
        for (Map<String, Object> d : maps(m.get("payment_details_list"))) {
            if (Boolean.TRUE.equals(d.get("is_primary"))) {
                c.eq("payment: primary payment_details_list.guid == payment_id", m.get("payment_id"), d.get("guid"));
                c.eq("payment: primary payment_details_list.payment_status", p.get("payment_status"),
                        d.get("payment_status"));
            }
        }
        double paid = num(m.get("paid_amount"));
        if ("Success".equalsIgnoreCase(str(p.get("payment_status")))
                && !"COD".equalsIgnoreCase(str(p.get("payment_type")))) {
            c.check("payment: due amounts are 0 once payment is Success", num(m.get("due_amount")) == 0
                    && num(m.get("order_due_amount")) == 0, "0 / 0", "due_amount " + m.get("due_amount")
                    + " order_due_amount " + m.get("order_due_amount"));
            c.check("payment: total_paid_amount == paid_amount once payment is Success",
                    Math.abs(num(m.get("total_paid_amount")) - paid) < 0.01, String.valueOf(paid),
                    "total_paid_amount " + m.get("total_paid_amount") + " vs paid_amount " + paid);
        }
        c.defect("null: top-level payment_status mirrors payment.payment_status",
                isBlank(p.get("payment_status")) || !isBlank(m.get("payment_status")),
                "payment_status is null while payment.payment_status is [" + p.get("payment_status") + "]");
    }

    /**
     * Order-level payment fields must match the payment record. They are mandatory once the payment is approved;
     * before approval (pay-after-collection flow) an empty order-level value is accepted, a different one is not.
     */
    private static void checkPaymentLink(FieldChecks c, String label, Object orderValue, Object paymentValue,
            boolean paymentApproved) {
        if (!paymentApproved && isBlank(orderValue)) {
            return;
        }
        c.eq("payment: " + label, orderValue, paymentValue);
    }

    private static void checkAmountSnapshot(FieldChecks c, Map<String, Object> m) {
        Map<String, Object> snap = map(m, "initial_order_amount_details");
        if (snap.isEmpty()) {
            return;
        }
        for (String f : Arrays.asList("total_price", "final_price", "paid_amount", "membership_discount")) {
            if (snap.get(f) != null) {
                c.check("snapshot: initial_order_amount_details." + f + " == order." + f,
                        Math.abs(num(snap.get(f)) - num(m.get(f))) < 0.01, String.valueOf(m.get(f)),
                        "snapshot " + snap.get(f) + " vs order " + m.get(f));
            }
        }
        Set<String> snapCodes = new TreeSet<>();
        for (Map<String, Object> t : maps(snap.get("tests"))) {
            snapCodes.add(str(t.get("product_code")));
        }
        Set<String> itemCodes = new TreeSet<>();
        for (Map<String, Object> i : maps(m.get("order_items"))) {
            itemCodes.add(str(i.get("product_code")));
        }
        if (!snapCodes.isEmpty()) {
            c.eq("snapshot: tests product codes == order_items product codes", itemCodes.toString(),
                    snapCodes.toString());
        }
    }

    private static void checkSlotAndAddress(FieldChecks c, Map<String, Object> m) {
        LocalDateTime start = literal(str(m.get("slot_start_time")));
        LocalDateTime end = literal(str(m.get("slot_end_time")));
        c.check("slot: slot_start_time < slot_end_time", start != null && end != null && start.isBefore(end),
                start + " < " + end, "start " + start + " end " + end);
        String label = str(m.get("slot_start_times"));
        if (label != null && start != null) {
            LocalDateTime parsed;
            try {
                parsed = LocalDateTime.parse(label, SLOT_LABEL);
            } catch (RuntimeException e) {
                parsed = null;
            }
            c.defect("slot: slot_start_times label matches slot_start_time", start.equals(parsed),
                    "label [" + label + "] vs slot_start_time " + start);
        }

        Map<String, Object> a = map(m, "addresses");
        double lat = num(a.get("latitude"));
        double lng = num(a.get("longitude"));
        c.check("address: latitude/longitude are valid coordinates", lat != 0 && lng != 0 && Math.abs(lat) <= 90
                && Math.abs(lng) <= 180, lat + "," + lng, "invalid coordinates " + a.get("latitude") + ","
                + a.get("longitude"));
        c.notBlank("address: address_name", a.get("address_name"));
        Object bookedMobile = map(m, "user_details").get("mobile");
        c.defect("address: addresses.mobile_number == user_details.mobile",
                isBlank(bookedMobile) || String.valueOf(bookedMobile).trim().equals(str(a.get("mobile_number"))),
                "address mobile [" + a.get("mobile_number") + "] vs user mobile [" + bookedMobile + "]");
        Map<String, Object> center = map(m, "user_nearby_center");
        if (center.get("zoneId") != null) {
            c.eq("zone: lab_location_id == user_nearby_center.zoneId", m.get("lab_location_id"), center.get("zoneId"));
        }
    }

    private static void checkLis(FieldChecks c, Map<String, Object> m, Stage stage) {
        List<Map<String, Object>> entries = sampleStatusEntries(m.get("sample_status"));
        if (entries.isEmpty()) {
            if (stage == Stage.SAMPLES_COLLECTED) {
                c.check("LIS: sample_status present", false, "", "sample_status is missing/empty");
            } else {
                c.note("LIS: sample_status not available yet at stage " + stage
                        + " (the lab number is only mapped later in the flow)");
            }
            return;
        }
        Set<String> labNos = new TreeSet<>();
        List<Map<String, Object>> tests = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            Object labNo = entry.get("LabNo");
            if (labNo instanceof List) {
                for (Object l : (List<?>) labNo) {
                    if (!isBlank(l)) {
                        labNos.add(String.valueOf(l).trim());
                    }
                }
            } else if (!isBlank(labNo)) {
                labNos.add(String.valueOf(labNo).trim());
            }
            tests.addAll(maps(entry.get("Test")));
        }
        c.check("LIS: exactly one distinct LabNo", labNos.size() == 1, labNos.toString(),
                "LabNo values: " + labNos);
        if (stage != Stage.SAMPLES_COLLECTED) {
            return;
        }

        Set<String> lisCodes = new HashSet<>();
        List<String> incomplete = new ArrayList<>();
        for (Map<String, Object> t : tests) {
            lisCodes.add(str(t.get("Test Code")));
            if (isBlank(t.get("Test Code")) || isBlank(t.get("Test Name")) || isBlank(t.get("Current Status"))) {
                incomplete.add(String.valueOf(t.get("Test Code")));
            }
        }
        c.check("LIS: every test has Test Code / Test Name / Current Status", !tests.isEmpty() && incomplete.isEmpty(),
                tests.size() + " tests", "incomplete or no tests: " + incomplete);

        List<String> absent = new ArrayList<>();
        List<Map<String, Object>> items = maps(m.get("order_items"));
        for (Map<String, Object> i : items) {
            if ("test".equalsIgnoreCase(str(i.get("product_type"))) && !lisCodes.contains(str(i.get("product_code")))) {
                absent.add(str(i.get("product_code")));
            }
        }
        c.check("LIS: every ordered test is present in sample_status", absent.isEmpty(), "all present",
                "missing from LIS: " + absent);
        c.check("LIS: at least as many LIS tests as order items", tests.size() >= items.size(),
                tests.size() + " >= " + items.size(), tests.size() + " LIS tests < " + items.size() + " order items");

        Instant created = instant(str(m.get("order_created_at")));
        List<String> beforeOrder = new ArrayList<>();
        for (Map<String, Object> t : tests) {
            Instant at = instant(str(t.get("Sample Collected Date")));
            if (at != null && created != null && at.isBefore(created)) {
                beforeOrder.add(t.get("Test Code") + "@" + t.get("Sample Collected Date"));
            }
        }
        c.defect("LIS: Sample Collected Date not earlier than order creation", beforeOrder.isEmpty(),
                "collected before the order existed (" + created + "): " + beforeOrder);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /** sample_status is a list of objects (one per lab number) or a single object depending on the order. */
    private static List<Map<String, Object>> sampleStatusEntries(Object raw) {
        if (raw instanceof Map) {
            return maps(java.util.Collections.singletonList(raw));
        }
        return maps(raw);
    }

    private static boolean within(Instant a, Instant b, long toleranceMillis) {
        return Math.abs(a.toEpochMilli() - b.toEpochMilli()) <= toleranceMillis;
    }

    private static boolean isAhead(String status) {
        return status != null && AHEAD_OF_COLLECTION.contains(status.trim().toLowerCase(Locale.ENGLISH));
    }

    private static boolean notAfter(Instant limit, Instant... times) {
        for (Instant t : times) {
            if (t != null && t.isAfter(limit)) {
                return false;
            }
        }
        return true;
    }

    private static String normaliseType(Object type) {
        return str(type) == null ? "null" : str(type).toUpperCase(Locale.ENGLISH).replaceAll("\\s+", " ");
    }

    /** Wall-clock HH:mm of an ISO literal such as 2026-10-13T15:37:00.000Z (the offset is not applied). */
    private static String clock(String iso) {
        return iso != null && iso.length() >= 16 ? iso.substring(11, 16) : null;
    }

    /** Local date-time of an ISO literal, ignoring the trailing Z / offset (slots are stored as wall-clock times). */
    private static LocalDateTime literal(String iso) {
        if (iso == null || iso.length() < 19) {
            return null;
        }
        try {
            return LocalDateTime.parse(iso.substring(0, 19).replace(' ', 'T'));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Parses ISO instants with an offset, or LIS-style "yyyy-MM-dd HH:mm:ss" strings (IST). */
    private static Instant instant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException ignored) {
            // fall through to offset-less formats
        }
        try {
            String v = value.trim();
            ZoneOffset zone = v.indexOf('T') >= 0 ? ZoneOffset.UTC : IST;
            return LocalDateTime.parse(v.replace(' ', 'T')).toInstant(zone);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> parent, String key) {
        Object v = parent.get(key);
        return v instanceof Map ? (Map<String, Object>) v : java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                if (o instanceof Map) {
                    out.add((Map<String, Object>) o);
                }
            }
        }
        return out;
    }

    private static boolean isBlank(Object v) {
        if (v == null) {
            return true;
        }
        if (v instanceof List) {
            return ((List<?>) v).isEmpty();
        }
        if (v instanceof Map) {
            return ((Map<?, ?>) v).isEmpty();
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equals(s) || "[]".equals(s);
    }

    private static String str(Object v) {
        return isBlank(v) ? null : String.valueOf(v).trim();
    }

    private static double num(Object v) {
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return v == null ? 0 : Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
