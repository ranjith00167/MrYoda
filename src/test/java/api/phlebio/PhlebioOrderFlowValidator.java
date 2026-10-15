package api.phlebio;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import api.phlebio.PhlebioIntegrityValidator.Stage;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Field-level and cross-API validations for the order part of the phlebo flow
 * (PHLEBO_MGMT_06 .. PHLEBO_MGMT_12). Each response is cross-checked against the earlier steps and against a
 * fresh GET /order/getOrderById/{id} read-back, and every mismatch is reported together.
 */
public final class PhlebioOrderFlowValidator {

    private static final String[] ORDER_ROW_FIELDS = {
            "order_number", "order_sample_number", "user_id", "address_id", "payment_id", "slot_guid",
            "total_price", "final_price", "paid_amount", "net_amount", "order_type", "payment_type", "brand_id"};

    private PhlebioOrderFlowValidator() {
    }

    /** Reads the order back via getOrderById using the phlebo JWT. */
    public static Response fetchOrder(String orderId, String phleboJwt) {
        return new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_ORDER_BY_ID + orderId)
                .addHeader("Authorization", "Bearer " + phleboJwt)
                .get();
    }

    // ── PHLEBO_MGMT_06 ──────────────────────────────────────────────────────

    public static void validateAssignOrder(Response assign, Response orderReadBack, String orderId,
                                           String phleboGuid, String phleboName, String visitOtp,
                                           String phleboZoneId, String shiftStart, String shiftEnd) {
        JsonPath a = assign.jsonPath();
        JsonPath o = orderReadBack.jsonPath();
        String order = orderRoot(o);
        String upd = "data.assignedOrders[0].updatedOrder";
        String trk = "data.assignedOrders[0].orderTracking";
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_06 assignOrder");

        c.isTrue("http status", assign.getStatusCode() == 200 || assign.getStatusCode() == 201,
                "expected 200/201 but was " + assign.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(a.get("success")), "success flag must be true");

        // updatedOrder vs the order that was placed
        c.eq(upd + ".guid == placed orderId", orderId, a.getString(upd + ".guid"));
        c.eq(upd + ".phlebo_id == login guid", phleboGuid, a.getString(upd + ".phlebo_id"));
        c.eq(upd + ".phlebo_otp == visit OTP", visitOtp, a.getString(upd + ".phlebo_otp"));
        c.notBlank(upd + ".phlebo_assigned_at", a.getString(upd + ".phlebo_assigned_at"));
        c.notBlank(upd + ".order_status", a.getString(upd + ".order_status"));
        c.eqIfKnown(upd + ".order_sample_number == order placement", RequestContext.getCurrentOrderSampleNumber(),
                a.getString(upd + ".order_sample_number"));
        c.eqIfKnown(upd + ".address_id == order placement", RequestContext.getCurrentAddressGuid(),
                a.getString(upd + ".address_id"));
        c.eqIfKnown(upd + ".slot_guid == order placement", RequestContext.getCurrentSlotGuid(),
                a.getString(upd + ".slot_guid"));

        // orderTracking row
        c.uuid(trk + ".guid", a.getString(trk + ".guid"));
        c.eq(trk + ".order_id == orderId", orderId, a.getString(trk + ".order_id"));
        c.eq(trk + ".phlebo_id == login guid", phleboGuid, a.getString(trk + ".phlebo_id"));
        c.eq(trk + ".address_id == order.address_id", a.getString(upd + ".address_id"), a.getString(trk + ".address_id"));
        c.notBlank(trk + ".assigned_date", a.getString(trk + ".assigned_date"));
        c.matches(trk + ".pickup_location", "^https://maps\\.google\\.com/\\?q=-?\\d+\\.\\d+,-?\\d+\\.\\d+$",
                a.getString(trk + ".pickup_location"));
        List<Map<String, Object>> statuses = a.getList(trk + ".status");
        c.eq(trk + ".status.size()", 1, statuses == null ? 0 : statuses.size());
        c.eq(trk + ".status[0].status", "open", a.getString(trk + ".status[0].status"));
        c.eq(trk + ".otp_verified", false, a.get(trk + ".otp_verified"));
        c.isNull(trk + ".barcode_id", a.getString(trk + ".barcode_id"));
        c.isTrue(trk + ".samples_picked is empty", listSize(a.getList(trk + ".samples_picked")) == 0,
                "no samples may be picked at assignment");

        // main_slot
        c.eq("data.assignedOrders[0].main_slot.phlebo_id", phleboGuid,
                a.getString("data.assignedOrders[0].main_slot.phlebo_id"));
        c.notBlank("data.assignedOrders[0].main_slot.assigned_at",
                a.getString("data.assignedOrders[0].main_slot.assigned_at"));

        // cross-API: assignOrder response vs getOrderById read-back
        c.eq("getOrderById.guid", orderId, o.getString(order + ".guid"));
        c.eq("getOrderById.phlebo_id == login guid", phleboGuid, o.getString(order + ".phlebo_id"));
        c.eq("getOrderById.phlebo_otp == assignOrder.phlebo_otp", a.getString(upd + ".phlebo_otp"),
                o.getString(order + ".phlebo_otp"));
        c.notBlank("getOrderById.phlebo_assigned_at", o.getString(order + ".phlebo_assigned_at"));
        c.eq("getOrderById.phlebo_details.guid", phleboGuid, o.getString(order + ".phlebo_details.guid"));
        c.eq("getOrderById.phlebo_details.name == login name", phleboName, o.getString(order + ".phlebo_details.name"));
        for (String field : ORDER_ROW_FIELDS) {
            Object assigned = a.get(upd + "." + field);
            if (assigned != null) {
                c.eq("assignOrder vs getOrderById: " + field, assigned, o.get(order + "." + field));
            }
        }
        c.eq("getOrderById.tracking_status[0].status", "open", o.getString(order + ".tracking_status[0].status"));
        c.eq("getOrderById.otp_verified", false, o.get(order + ".otp_verified"));

        // integrity: status / null / consistency rules across both APIs
        c.matches(upd + ".phlebo_otp is a 6-digit code", "^\\d{6}$", a.getString(upd + ".phlebo_otp"));
        PhlebioIntegrityValidator.checkEnvelope(c, assign);
        PhlebioIntegrityValidator.checkTrackingRow(c, a, trk, Stage.ASSIGNED, a.getString(upd + ".address_id"));
        PhlebioIntegrityValidator.checkOrder(c, o, order, Stage.ASSIGNED);
        PhlebioIntegrityValidator.checkAssignmentFit(c, o, order, phleboZoneId, shiftStart, shiftEnd);
        c.assertAll();
    }

    // ── PHLEBO_MGMT_07 ──────────────────────────────────────────────────────

    public static void validateInProgress(Response update, Response orderReadBack, String trackingId,
                                          String orderId, String phleboGuid, String lat, String lng) {
        JsonPath u = update.jsonPath();
        JsonPath o = orderReadBack.jsonPath();
        String order = orderRoot(o);
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_07 updateOrderTracking(inprogress)");

        c.isTrue("http status", update.getStatusCode() == 200 || update.getStatusCode() == 201,
                "expected 200/201 but was " + update.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(u.get("success")), "success flag must be true");
        c.eq("data[0].guid == order_tracking_id sent", trackingId, u.getString("data[0].guid"));
        c.eq("data[0].order_id == orderId", orderId, u.getString("data[0].order_id"));
        c.eq("data[0].phlebo_id == login guid", phleboGuid, u.getString("data[0].phlebo_id"));
        c.eq("data[0].pickup_location == sent coordinates", "https://maps.google.com/?q=" + lat + "," + lng,
                u.getString("data[0].pickup_location"));
        c.eq("data[0].otp_verified", false, u.get("data[0].otp_verified"));
        c.isNull("data[0].barcode_id", u.getString("data[0].barcode_id"));
        c.isTrue("data[0].samples_picked is empty", listSize(u.getList("data[0].samples_picked")) == 0,
                "no samples may be picked yet");
        statusChain(c, "data[0].status", u.getList("data[0].status"), "open", "inprogress");

        c.eq("getOrderById.phlebo_id", phleboGuid, o.getString(order + ".phlebo_id"));
        statusChain(c, "getOrderById.tracking_status", o.getList(order + ".tracking_status"), "open", "inprogress");
        c.sameInstant("getOrderById tracking time == updateOrderTracking time",
                u.getString("data[0].status[1].time"), o.getString(order + ".tracking_status[1].time"));
        c.note("order_status after inprogress = [" + o.getString(order + ".order_status") + "]");

        PhlebioIntegrityValidator.checkEnvelope(c, update);
        PhlebioIntegrityValidator.checkTrackingRow(c, u, "data[0]", Stage.IN_PROGRESS,
                o.getString(order + ".address_id"));
        PhlebioIntegrityValidator.checkOrder(c, o, order, Stage.IN_PROGRESS);
        c.assertAll();
    }

    // ── PHLEBO_MGMT_08 ──────────────────────────────────────────────────────

    public static void validateUpload(Response upload, File image, long startedAtMillis) {
        JsonPath u = upload.jsonPath();
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_08 upload image");
        c.isTrue("http status", upload.getStatusCode() == 200 || upload.getStatusCode() == 201,
                "expected 200/201 but was " + upload.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(u.get("success")), "success flag must be true");

        String url = u.getString("data.url");
        c.matches("data.url format",
                "^https://[\\w.-]+\\.amazonaws\\.com/phlebo/general/phlebo-image-\\d{13}-"
                        + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.jpg$", url);
        if (url != null) {
            String epoch = url.replaceAll(".*phlebo-image-(\\d{13})-.*", "$1");
            if (epoch.matches("\\d{13}")) {
                long uploadedAt = Long.parseLong(epoch);
                c.isTrue("data.url timestamp is current", Math.abs(uploadedAt - startedAtMillis) < 5 * 60_000L,
                        "embedded timestamp " + uploadedAt + " is not within 5 min of the upload time");
            }
            // cross-API: the stored object is reachable and is the same image that was sent
            Response stored = RestAssured.given().relaxedHTTPSValidation().get(url);
            c.eq("GET data.url http status", 200, stored.getStatusCode());
            c.eq("GET data.url Content-Length == uploaded file size", image.length(), stored.asByteArray().length);
            c.contains("GET data.url Content-Type", stored.getContentType(), "image/jpeg");
        }
        c.assertAll();
    }

    // ── PHLEBO_MGMT_09 ──────────────────────────────────────────────────────

    public static void validateVerifyOtp(Response verify, Response orderReadBack, String orderId, String phleboGuid) {
        JsonPath v = verify.jsonPath();
        JsonPath o = orderReadBack.jsonPath();
        String order = orderRoot(o);
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_09 verifyotp [" + orderId + "]");

        c.eq("http status", 200, verify.getStatusCode());
        c.eq("body.status", 200, v.get("status"));
        c.isTrue("success", Boolean.TRUE.equals(v.get("success")), "success flag must be true");
        c.contains("message", v.getString("message"), "verified");
        c.eq("msg == message", v.getString("message"), v.getString("msg"));
        c.eq("total_amount", 0, v.get("total_amount"));

        c.eq("getOrderById.guid", orderId, o.getString(order + ".guid"));
        c.eq("getOrderById.otp_verified turns true", true, o.get(order + ".otp_verified"));
        c.eq("getOrderById.phlebo_id", phleboGuid, o.getString(order + ".phlebo_id"));
        statusChain(c, "getOrderById.tracking_status (OTP does not change status)",
                o.getList(order + ".tracking_status"), "open", "inprogress");

        PhlebioIntegrityValidator.checkEnvelope(c, verify);
        PhlebioIntegrityValidator.checkOrder(c, o, order, Stage.OTP_VERIFIED);
        c.assertAll();
    }

    // ── PHLEBO_MGMT_10 ──────────────────────────────────────────────────────

    public static void validateSampleDetails(Response orderResp, String orderId, String phleboGuid, String phleboName,
                                             String visitOtp, String lat, String lng) {
        JsonPath o = orderResp.jsonPath();
        String r = orderRoot(o);
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_10 getOrderById (sample details)");

        c.eq("http status", 200, orderResp.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(o.get("success")), "success flag must be true");
        c.eq("guid == placed orderId", orderId, o.getString(r + ".guid"));
        c.eq("phlebo_id == login guid", phleboGuid, o.getString(r + ".phlebo_id"));
        c.eq("phlebo_details.guid", phleboGuid, o.getString(r + ".phlebo_details.guid"));
        c.eq("phlebo_details.name == login name", phleboName, o.getString(r + ".phlebo_details.name"));
        c.eq("phlebo_otp == visit OTP", visitOtp, o.getString(r + ".phlebo_otp"));
        c.eq("otp_verified (after PHLEBO_MGMT_09)", true, o.get(r + ".otp_verified"));
        c.eqIfKnown("order_sample_number == order placement", RequestContext.getCurrentOrderSampleNumber(),
                o.getString(r + ".order_sample_number"));
        c.eqIfKnown("address_id == order placement", RequestContext.getCurrentAddressGuid(), o.getString(r + ".address_id"));
        c.eq("user_details.guid == user_id", o.getString(r + ".user_id"), o.getString(r + ".user_details.guid"));
        c.eq("addresses.latitude == pickup latitude (PHLEBO_MGMT_07)", lat, o.getString(r + ".addresses.latitude"));
        c.eq("addresses.longitude == pickup longitude (PHLEBO_MGMT_07)", lng, o.getString(r + ".addresses.longitude"));
        statusChain(c, "tracking_status", o.getList(r + ".tracking_status"), "open", "inprogress");
        c.isTrue("samples_picked is empty before collection", listSize(o.getList(r + ".samples_picked")) == 0,
                "samples_picked must be empty until PHLEBO_MGMT_12");
        c.isNull("barcode_id before PHLEBO_MGMT_11", o.getString(r + ".barcode_id"));

        // real lab number generated by the backend on payment approval
        String visit = o.getString(r + ".visit_number");
        c.matches("visit_number is a real lab number (MYD#####)", "^MYD\\d+$", visit);
        String sampleStatusLab = sampleStatusLabNo(o, r);
        if (sampleStatusLab != null) {
            c.eq("sample_status.LabNo == visit_number", visit, sampleStatusLab);
        }

        validateOrderItems(c, o, r, orderId);
        PhlebioIntegrityValidator.checkOrder(c, o, r, Stage.OTP_VERIFIED);
        c.assertAll();
    }

    /** sample_status.LabNo is returned as a one-element array (e.g. ["MYD24192"]); accept a plain string too. */
    private static String sampleStatusLabNo(JsonPath o, String root) {
        Object raw = o.get(root + ".sample_status.LabNo");
        if (raw instanceof List) {
            List<?> values = (List<?>) raw;
            return values.isEmpty() || values.get(0) == null ? null : values.get(0).toString();
        }
        return raw == null ? null : raw.toString();
    }

    // ── PHLEBO_MGMT_11 ──────────────────────────────────────────────────────

    public static void validateBarcode(Response barcodeResp, String labno, String barcode, String orderVisitNumber) {
        JsonPath b = barcodeResp.jsonPath();
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_11 ChangeBarCodedata");
        c.isTrue("http status", barcodeResp.getStatusCode() == 200 || barcodeResp.getStatusCode() == 201,
                "expected 200/201 but was " + barcodeResp.getStatusCode());
        c.eq("status", true, b.get("status"));
        c.contains("message", b.getString("message"), "success");
        c.eq("LabNo == labno sent", labno, b.getString("LabNo"));
        c.eq("labno sent == order.visit_number", orderVisitNumber, labno);
        c.matches("labno format", "^MYD\\d+$", labno);
        c.matches("Barcodeno format (8 digits)", "^\\d{8}$", barcode);
        c.assertAll();
    }

    // ── PHLEBO_MGMT_12 ──────────────────────────────────────────────────────

    public static void validateSamplesCollected(Response update, Response orderReadBack, String trackingId,
                                                String orderId, String phleboGuid, List<Map<String, Object>> samplesSent,
                                                String barcode, String selfieUrl, String visitNumber) {
        JsonPath u = update.jsonPath();
        JsonPath o = orderReadBack.jsonPath();
        String r = orderRoot(o);
        String t = "data[0]";
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_12 updateOrderTracking(samples_collected)");

        c.isTrue("http status", update.getStatusCode() == 200 || update.getStatusCode() == 201,
                "expected 200/201 but was " + update.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(u.get("success")), "success flag must be true");
        c.eq(t + ".guid == order_tracking_id sent", trackingId, u.getString(t + ".guid"));
        c.eq(t + ".order_id == orderId", orderId, u.getString(t + ".order_id"));
        c.eq(t + ".phlebo_id == login guid", phleboGuid, u.getString(t + ".phlebo_id"));
        c.eq(t + ".otp_verified", true, u.get(t + ".otp_verified"));
        c.eq(t + ".barcode_id == barcode sent (PHLEBO_MGMT_11)", barcode, u.getString(t + ".barcode_id"));
        c.eq(t + ".phlebo_selfie_url == uploaded url (PHLEBO_MGMT_08)", selfieUrl, u.getString(t + ".phlebo_selfie_url"));
        c.isNull(t + ".consent_form_type", u.getString(t + ".consent_form_type"));
        c.isTrue(t + ".consent_form_images is empty", listSize(u.getList(t + ".consent_form_images")) == 0,
                "no consent images were sent");
        samplesMatch(c, t + ".samples_picked == samples_collected sent", samplesSent, u.getList(t + ".samples_picked"));
        statusChain(c, t + ".status", u.getList(t + ".status"), "open", "inprogress", "samples_collected");

        // cross-API read-back
        c.eq("getOrderById.guid", orderId, o.getString(r + ".guid"));
        c.eq("getOrderById.otp_verified", true, o.get(r + ".otp_verified"));
        c.eq("getOrderById.barcode_id", barcode, o.getString(r + ".barcode_id"));
        c.eq("getOrderById.phlebo_selfie_url", selfieUrl, o.getString(r + ".phlebo_selfie_url"));
        c.eq("getOrderById.visit_number == barcode-mapped labno", visitNumber, o.getString(r + ".visit_number"));
        c.eq("getOrderById.sample_status.LabNo", visitNumber, sampleStatusLabNo(o, r));
        c.eq("getOrderById.order_status", "Sample Collected", o.getString(r + ".order_status"));
        c.eq("getOrderById.it_dose_order_status", "Sample Collected", o.getString(r + ".it_dose_order_status"));
        samplesMatch(c, "getOrderById.samples_picked == samples_collected sent", samplesSent,
                o.getList(r + ".samples_picked"));
        statusChain(c, "getOrderById.tracking_status", o.getList(r + ".tracking_status"),
                "open", "inprogress", "samples_collected");
        c.sameInstant("getOrderById tracking time == updateOrderTracking time", u.getString(t + ".status[2].time"),
                o.getString(r + ".tracking_status[2].time"));
        List<Map<String, Object>> items = o.getList(r + ".order_items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                c.eq("order_item[" + item.get("order_item_number") + "].order_status", "Sample Collected",
                        item.get("order_status"));
            }
        }

        PhlebioIntegrityValidator.checkEnvelope(c, update);
        PhlebioIntegrityValidator.checkTrackingRow(c, u, t, Stage.SAMPLES_COLLECTED, o.getString(r + ".address_id"));
        PhlebioIntegrityValidator.checkOrder(c, o, r, Stage.SAMPLES_COLLECTED);
        PhlebioIntegrityValidator.checkSampleCoverage(c, o, r, samplesSent);
        c.assertAll();
    }

    // ── shared helpers ──────────────────────────────────────────────────────

    private static void validateOrderItems(FieldChecks c, JsonPath o, String r, String orderId) {
        List<Map<String, Object>> items = o.getList(r + ".order_items");
        c.isTrue("order_items present", items != null && !items.isEmpty(), "order_items must not be empty");
        if (items == null) {
            return;
        }
        String sampleNumber = o.getString(r + ".order_sample_number");
        double sumActual = 0;
        double sumFinal = 0;
        double sumDiscount = 0;
        for (Map<String, Object> item : items) {
            String label = "order_item[" + item.get("order_item_number") + "]";
            c.eq(label + ".order_id", orderId, item.get("order_id"));
            c.isTrue(label + ".order_item_number prefix", String.valueOf(item.get("order_item_number"))
                    .startsWith(sampleNumber + "-"), "must start with " + sampleNumber + "-");
            c.notBlank(label + ".product_code", item.get("product_code"));
            c.notBlank(label + ".product_name", item.get("product_name"));
            c.isTrue(label + ".sample_types has DefaultSampleType", hasSampleType(item.get("sample_types")),
                    "each item needs at least one DefaultSampleType");
            c.eq(label + ".final_price = actual_price - membership_discount",
                    num(item.get("actual_price")) - num(item.get("membership_discount")), num(item.get("final_price")));
            sumActual += num(item.get("actual_price")) * Math.max(1, num(item.get("quantity")));
            sumFinal += num(item.get("final_price")) * Math.max(1, num(item.get("quantity")));
            sumDiscount += num(item.get("membership_discount")) * Math.max(1, num(item.get("quantity")));
        }
        c.eq("sum(items.actual_price) == order.total_price", num(o.get(r + ".total_price")), sumActual);
        c.eq("sum(items.final_price) == order.paid_amount", num(o.get(r + ".paid_amount")), sumFinal);
        c.eq("sum(items.membership_discount) == order.total_discount", num(o.get(r + ".total_discount")), sumDiscount);
    }

    private static boolean hasSampleType(Object sampleTypes) {
        if (!(sampleTypes instanceof List)) {
            return false;
        }
        for (Object st : (List<?>) sampleTypes) {
            if (st instanceof Map && ((Map<?, ?>) st).get("DefaultSampleType") != null
                    && !String.valueOf(((Map<?, ?>) st).get("DefaultSampleType")).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Asserts the status history equals the expected ordered chain with non-decreasing timestamps. */
    private static void statusChain(FieldChecks c, String label, List<Map<String, Object>> actual, String... expected) {
        List<String> names = new ArrayList<>();
        List<Instant> times = new ArrayList<>();
        if (actual != null) {
            for (Map<String, Object> s : actual) {
                names.add(String.valueOf(s.get("status")));
                try {
                    times.add(Instant.parse(String.valueOf(s.get("time"))));
                } catch (RuntimeException e) {
                    times.add(null);
                }
            }
        }
        c.eq(label + " chain", String.join(">", expected), String.join(">", names));
        boolean ordered = !times.contains(null);
        for (int i = 1; ordered && i < times.size(); i++) {
            ordered = !times.get(i).isBefore(times.get(i - 1));
        }
        c.isTrue(label + " timestamps are valid & non-decreasing", ordered, "timestamps " + times);
    }

    private static void samplesMatch(FieldChecks c, String label, List<Map<String, Object>> sent,
                                     List<Map<String, Object>> actual) {
        c.eq(label, canonical(sent), canonical(actual));
    }

    private static String canonical(List<Map<String, Object>> samples) {
        List<String> keys = new ArrayList<>();
        if (samples != null) {
            for (Map<String, Object> s : samples) {
                keys.add(String.valueOf(s.get("sample_type")).trim().toUpperCase() + " x" + (int) num(s.get("quantity")));
            }
        }
        Collections.sort(keys);
        return keys.toString();
    }

    private static String orderRoot(JsonPath body) {
        return body.get("data") instanceof List ? "data[0]" : "data";
    }

    private static int listSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static double num(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? 0 : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
