# COD Flow API Documentation

This document outlines the API endpoints and payloads used in the Cash on Delivery (COD) order creation flow.

## 1. Get Cart Details
**Endpoint:** `/carts/v2/getCartById/{user_id}`
**Method:** `GET`
**Description:** Retrieves the current cart details for the user to calculate totals and retrieve the Cart ID.

**Query Parameters:**
- `location`: `{lab_location_id}` (e.g., `64870066842708a0d5ae6c77`)
- `order_type`: `home`

**Response Key Fields:**
- `data.totalPrice`: Total amount to check COD eligibility (< 2500).
- `data.guid`: Cart ID used in subsequent requests.
- `data.slot_guid`: Slot ID associated with the cart.

---

## 2. Add Address
**Endpoint:** `/address/addAddress`
**Method:** `POST`
**Description:** Adds a delivery address for the user. This is required for Home Collection orders.

**Payload:**
```json
{
    "user_id": "807d0e97-ee14-4c97-98cd-c870f22912cf",
    "receiver_name": "Test User",
    "recipient_mobile_number": "9999999999",
    "address_line1": "Hyderabad",
    "name": "Ameerpet (HQ)",
    "type": "home",
    "country": "India",
    "state": "Telangana",
    "city": "Hyderabad",
    "postal_code": "500016",
    "country_code": "+91",
    "latitude": "17.4358447",
    "longitude": "78.452737"
}
```

**Response Key Fields:**
- `data.id`: Address ID (used in VerifyPayment).
- `data.guid`: Address GUID (used in Slot Selection).

---

## 3. Get Available Slots
**Endpoint:** `/slot/getSlotCountByTime`
**Method:** `POST`
**Description:** Searches for available time slots for a specific date and address.

**Payload:**
```json
{
    "slot_start_time": "2025-12-30",
    "page": 1,
    "limit": 100,
    "type": "home",
    "addressguid": "59635151-d9be-40dd-a42d-14d1c42d00f5"
}
```

**Response Key Fields:**
- `data[].guid`: Slot GUID.
- `data[].starttime`: Start time of the slot.
- `data[].endtime`: End time of the slot.

---

## 4. Verify Payment (Create COD Order)
**Endpoint:** `/gateway/v2/VerifyPayment`
**Method:** `POST`
**Description:** Validates the payment and creates the order. For COD, this acts as the final order creation step.

**Payload:**
```json
{
    "cart_id": "c364e215-7b5d-483b-ae21-4f9f24048f23",
    "payment_mode": "cash",
    "source": "android",
    "user_id": "807d0e97-ee14-4c97-98cd-c870f22912cf",
    "address_id": "700",
    "slot_id": "ae8dd3ac-d07f-40e2-857c-cd1c59ae7e3d",
    "date": "2025-12-30",
    "time": "2025-12-30 18:00:00 - 2025-12-30 19:00:00",
    "total_amount": 1053,
    "order_type": "home"
}
```

**Notes:**
- `address_id`: Must be passed as a String.
- `time`: Format is `YYYY-MM-DD HH:mm:ss - YYYY-MM-DD HH:mm:ss`.
- `lab_location_id`: **Excluded** to ensure the order is treated as Home Collection.

---

## 5. Get Payment Details
**Endpoint:** `/gateway/getPaymentById`
**Method:** `POST`
**Description:** Retrieves details of the created payment/order to verify success.

**Payload:**
```json
{
    "id": "00894dd5-90fa-4c36-8133-26dfd4174073"
}
```

---

## 6. Get Order Details (Validation)
**Endpoint:** `/order/getOrderById/{order_id}`
**Method:** `GET`
**Description:** Fetches the full order object to verify fields like `home_sample`.

**Response Validation:**
- `home_sample`: Should be `true` for Home Collection orders (currently debugging this field).
