"""
Patches all price-related Rule/Description cells in generate_excel.py
to use exact formulas instead of generic null/range checks.
"""
import re

SRC = r"c:\Users\RANJITH\MrYoda\docs\generate_excel.py"

with open(SRC, encoding="utf-8") as f:
    code = f.read()

REPLACEMENTS = [
    # ── [07] AddToCart data.totalPrice ──────────────────────────────────────
    (
        '"Must be > 0; stored to RequestContext.setCurrentTotalPrice()"',
        '"= sum(unitPrice x qty) for each product across all cart items. Must be > 0. Stored to RequestContext.setCurrentTotalPrice()."'
    ),
    # ── [07] coupon_amount (percentage) ─────────────────────────────────────
    (
        '"min(subtotal \u00d7 pct/100, max_redeemable)"',
        '"FORMULA: coupon_amount = min(subtotal x pct / 100, max_redeemable). Stored to RequestContext.setCouponAmount()."'
    ),
    # ── [07] coupon_amount (flat) ────────────────────────────────────────────
    (
        '"flat discount value"',
        '"FORMULA: coupon_amount = flat_discount_value (fixed amount from coupon record). Stored to RequestContext.setCouponAmount()."'
    ),
    # ── [07] payable = subtotal - coupon_amount ──────────────────────────────
    (
        '"Stored to RequestContext.setCouponAmount()"',
        '"FORMULA: payable = totalPrice - coupon_amount. Stored to RequestContext.setCurrentDueAmount(). Must be > 0."'
    ),
    # ── [08] data.totalPrice ─────────────────────────────────────────────────
    (
        '"Parsed and stored; < 2500 enforced"',
        '"CROSS-STEP EXACT: must equal [07] AddToCart totalPrice (same cart re-fetched). Must be < 2500 (COD limit). Stored to RequestContext.setCurrentTotalPrice() for downstream price cross-checks."'
    ),
    # ── [08] due_amount / payable_amount ─────────────────────────────────────
    (
        '"Stored to RequestContext.setCurrentDueAmount()"',
        '"FORMULA: due_amount = totalPrice - coupon_amount. Stored to RequestContext.setCurrentDueAmount() — used in rewards_gain formula at steps [13] and [14]."'
    ),
    # ── [11] payment.amount vs cart totalPrice ───────────────────────────────
    (
        '"Cross-API consistency"',
        '"CROSS-API (hard, +-1.0): payment.amount ~= RequestContext.getCurrentTotalPrice() stored by [08] GetCart. |payment.amount - totalPrice| <= 1.0 required."'
    ),
    # ── [13] data[0].rewards_gain ────────────────────────────────────────────
    (
        '"Not null; >= 0; stored to RequestContext.setRewardsGain()"',
        '"Not null; >= 0. FORMULA: rewards_gain = ceil(dueAmount x rewards_pct / 100), where dueAmount = RequestContext.getCurrentDueAmount() from [08] and rewards_pct from GlobalSearch. Stored to RequestContext.setRewardsGain()."'
    ),
    # ── [14] actualGain formula ───────────────────────────────────────────────
    (
        '"Rewards proportional to amount paid"',
        '"FORMULA: actualGain = ceil(RequestContext.getCurrentDueAmount() x rewards_pct / 100). rewards_pct from GlobalSearch response. dueAmount = what user paid after coupon from [08]."'
    ),
    # ── [15] mathematical baseline ───────────────────────────────────────────
    (
        '"Mathematical baseline"',
        '"FORMULA: expected = RequestContext.getInitialTotalRewards() [step 12] + RequestContext.getRewardsGain() [step 13]. Fresh GET call must confirm actual == expected."'
    ),
    # ── [15] |actual - expected| < 0.1 ───────────────────────────────────────
    (
        '"Fresh API call vs mathematical expectation"',
        '"HARD: |getRewardsByMobile().total_rewards - (initialBalance + rewardsGain)| < 0.1. initialBalance = getInitialTotalRewards() [step 12], rewardsGain = getRewardsGain() [step 13]."'
    ),
    # ── [16] paid_amount -> memberDiscountAmt ────────────────────────────────
    (
        '">= 0. Semantic: membership DISCOUNT applied (NOT cash paid)"',
        '">= 0. SEMANTIC: This field is the membership DISCOUNT amount -- NOT cash paid by user. Formula role: actual_price - paid_amount(memberDiscountAmt) - couponSplit = canceled_amount.orderedByCash (actual cash)."'
    ),
    # ── [16] adjustedRefundAmount ────────────────────────────────────────────
    (
        '">= 0. Must equal canceled_amount.orderedByCash"',
        '"EXACT EQUALITY (hard): adjustedRefundAmount = canceled_amount.orderedByCash. Cash refund to user -- must be exactly equal, not merely >= 0."'
    ),
    # ── [16] canceled_amount.orderedByCash ───────────────────────────────────
    (
        '"> 0. Actual cash paid by user for this sub-order"',
        '"> 0. FORMULA: orderedByCash = actual_price - memberDiscountAmt(paid_amount) - couponSplitThisOrder. CROSS-API (single order hard): orderedByCash == RequestContext.getCurrentTotalPrice() from [08]."'
    ),
    # ── [16] actual_price - memberDiscountAmt >= orderedByCash ───────────────
    (
        '"Inequality allows coupon split"',
        '"FORMULA: actual_price - memberDiscountAmt >= orderedByCash. Gap = couponSplitThisOrder (0 when no coupon). Full derivation: couponSplit = (actual_price - memberDiscountAmt) - orderedByCash."'
    ),
    # ── [16] Cross-API: orderedByCash == storedCartTotal ─────────────────────
    (
        '"RequestContext.getCurrentTotalPrice() from step [08]"',
        '"CROSS-API EXACT (single order): canceled_amount.orderedByCash == RequestContext.getCurrentTotalPrice() stored by [08] GetCart. Multi-member: sum of sub-orders ~= storedCartTotal (+-1.0 soft)."'
    ),
    # ── [16] Cross-API: remaining_rewards == rewardsGainForOrder ─────────────
    (
        '"RequestContext.getRewardsGain() from step [13]"',
        '"CROSS-API EXACT (when stored > 0): outer.remaining_rewards == RequestContext.getRewardsGain() from step [13] approvepayment. This is order-specific rewards earn, not total wallet balance."'
    ),
    # ── [19] paid_amount ─────────────────────────────────────────────────────
    (
        '">= 0. Cash user actually paid (post-membership, post-coupon)"',
        '">= 0. FORMULA: paid_amount = total_price - membership_discount - coupon_discount. Exact cash paid after ALL discounts. Verified by: refund_amount = paid_amount (EXACT equality below)."'
    ),
    # ── [19] actual_discount formula ─────────────────────────────────────────
    (
        '"Must equal membership_discount + coupon_discount"',
        '"EXACT FORMULA (hard): actual_discount = membership_discount + coupon_discount. coupon_discount resolved via fallback chain: response.coupon_discount -> coupon_discount_amount -> RequestContext.getCouponAmount()."'
    ),
    # ── [19] refund_amount ───────────────────────────────────────────────────
    (
        '"Must exactly equal paid_amount (full refund)"',
        '"EXACT EQUALITY (hard): refund_amount = paid_amount. User receives back exactly what they paid post-discount. Coupon NOT refunded separately -- already reduced paid_amount at purchase."'
    ),
    # ── [19] rewards_gain cross-API ──────────────────────────────────────────
    (
        '">= 0; must equal getRewardsGain() when stored > 0"',
        '">= 0. CROSS-API EXACT (hard when stored > 0): rewards_gain = RequestContext.getRewardsGain() stored by step [13] approvepayment. Verifies order records same rewards as granted."'
    ),
    # ── [19] total_price - memDisc - coupon = paid_amount ────────────────────
    (
        '"Discount consistency formula"',
        '"EXACT FORMULA (hard): total_price - membership_discount - coupon_discount = paid_amount. ALSO (separate hard assert): membership_discount + coupon_discount = actual_discount. Coupon via fallback chain."'
    ),
    # ── [19] payment_id cross-API ────────────────────────────────────────────
    (
        '"Equals getCurrentPaymentId()"',
        '"CROSS-API EXACT: payment_id == RequestContext.getCurrentPaymentId() stored by step [10] VerifyPaymentPreCheck."'
    ),
    # ── [19] slot_guid cross-API ─────────────────────────────────────────────
    (
        '"Equals getCurrentSlotGuid()"',
        '"CROSS-API EXACT: slot_guid == RequestContext.getCurrentSlotGuid() stored by step [09] AddLabSlot."'
    ),
    # ── [19] per-item formula ────────────────────────────────────────────────
    (
        '"Per-item price formula (HARD ASSERT per item)"',
        '"EXACT FORMULA PER ITEM (hard): item.actual_price - item.membership_discount = item.final_price. Accumulate sumItemFinalPrices += final_price for post-loop sum check. Also: item.actual_discount = item.membership_discount (when present, hard)."'
    ),
    # ── [19] sum formula ─────────────────────────────────────────────────────
    (
        '"Post-loop sum formula"',
        '"EXACT SUM FORMULA (hard): sum(order_items[].final_price) - couponDiscThisOrder = paid_amount. Each item.final_price = actual_price - membership_discount (pre-coupon). couponDiscThisOrder from fallback chain: response.coupon_discount -> coupon_discount_amount -> RequestContext.getCouponAmount()."'
    ),
    # ── [20] guid cross-API ──────────────────────────────────────────────────
    (
        '"Equals getCurrentPaymentId()"',
        '"CROSS-API EXACT: data.payments.guid == RequestContext.getCurrentPaymentId() stored by step [10] VerifyPaymentPreCheck."'
    ),
    # ── [20] amount ──────────────────────────────────────────────────────────
    (
        '">= 0. Soft cross-compare with getCurrentTotalPrice()"',
        '">= 0. CROSS-API (info, +-1.0): amount ~= RequestContext.getCurrentTotalPrice() from [08]. If differs, difference = membership_discount applied. Price math (info): actual_price - membership_discount - coupon_discount ~= amount."'
    ),
    # ── [20] coupon_discount ─────────────────────────────────────────────────
    (
        '">= 0. Cross-API \u2248 getCouponAmount() (\u00b11.0); Soft on mismatch","POST /gateway/getPaymentById"',
        '">= 0. CROSS-API SOFT (+-1.0): coupon_discount ~= RequestContext.getCouponAmount() from [08]. Price math (info): actual_price - membership_discount - coupon_discount ~= amount (+-1.0). Soft on mismatch.","POST /gateway/getPaymentById"'
    ),
    # ── [21] total_rewards ───────────────────────────────────────────────────
    (
        '">= 0; stored as postCancelRewards"',
        '">= 0. Stored as postCancelRewards. Cross-checks: (1) postCancelRewards < finalRewards (hard); (2) finalRewards - postCancelRewards ~= rewardsGain (+-1.0 hard); (3) postCancelRewards ~= initialRewards (info full-reversal)."'
    ),
    # ── [21] reversal formula ────────────────────────────────────────────────
    (
        '"Reversal amount equals what was earned"',
        '"FORMULA (hard, +-1.0): finalRewards - postCancelRewards = rewardsGain. finalRewards = RequestContext.getFinalTotalRewards() [step 15], rewardsGain = RequestContext.getRewardsGain() [step 13]. Confirms exact reversal amount."'
    ),
    # ── [22] trnsc_amount ────────────────────────────────────────────────────
    (
        '">= 0 (parsed from string)"',
        '">= 0 (parsed from string). PRICE FORMULA (info, +-1.0): actual_price - membership_discount - coupon_discount ~= trnsc_amount. SUCCESS: trnsc_amount ~= getCurrentTotalPrice() (+-1.0, info). CANCELLED: same formula for reversal."'
    ),
    # ── [22] coupon_discount (transaction) ───────────────────────────────────
    (
        '">= 0. Cross-API \u2248 getCouponAmount() (\u00b11.0); Soft on mismatch","GET /transaction/getTransactionByMobile/{mobile}"',
        '">= 0. CROSS-API SOFT (+-1.0): coupon_discount ~= RequestContext.getCouponAmount() from [08]. Note: transaction API stores 0 for Success records by design -- coupon confirmed via order/payment records.","GET /transaction/getTransactionByMobile/{mobile}"'
    ),
    # ── [22] Success refund_amount = 0 ───────────────────────────────────────
    (
        '"Must be 0 \u2014 no refund at order placement"',
        '"EXACT VALUE (hard): refund_amount = 0 for Success/Successful records. No refund existed at order placement. Hard assert: verifyTrue(dRefund == 0.0)."'
    ),
    # ── [22] trnsc_amount vs totalPrice ──────────────────────────────────────
    (
        '"Compared (\u00b11.0 tolerance) \u2014 logged"',
        '"CROSS-API (info, +-1.0): Success.trnsc_amount ~= RequestContext.getCurrentTotalPrice() from [08]. Also: membership_discount = actual_price - trnsc_amount (info check)."'
    ),
    # ── [22] Cancelled trnsc vs totalPrice ───────────────────────────────────
    (
        '"Must represent full refund \u2014 logged"',
        '"CROSS-API (info, +-1.0): Cancelled.trnsc_amount ~= RequestContext.getCurrentTotalPrice() from [08]. Represents the refunded cash amount."'
    ),
    # ── [22] refund_amount vs trnsc_amount (Cancelled) ───────────────────────
    (
        '"Full refund expected \u2014 logged (\u00b11.0)"',
        '"FORMULA (info, +-1.0): Cancelled.refund_amount ~= Cancelled.trnsc_amount (full cash refund). Also: actual_price - membership_discount - coupon_discount ~= trnsc_amount (+-1.0 info)."'
    ),
    # ── [22] rewards_gain vs getRewardsGain ──────────────────────────────────
    (
        '"Should match original earn amount (reversal) \u2014 logged"',
        '"CROSS-API (info, +-1.0): Cancelled.rewards_gain ~= RequestContext.getRewardsGain() from [13]. Confirms rewards reversal matches the original earn amount."'
    ),
    # ── [22] net_paid_amount vs storedCartTotal ───────────────────────────────
    (
        '"Logged (\u00b11.0)"',
        '"CROSS-API (info, +-1.0): Cancelled.net_paid_amount ~= RequestContext.getCurrentTotalPrice() from [08]. Logged for refund tracking."'
    ),
]

count = 0
for old, new in REPLACEMENTS:
    if old in code:
        code = code.replace(old, new, 1)
        count += 1
    else:
        print(f"  [MISS] Not found: {old[:80]}")

with open(SRC, "w", encoding="utf-8") as f:
    f.write(code)

print(f"\nDone. {count}/{len(REPLACEMENTS)} replacements applied.")
