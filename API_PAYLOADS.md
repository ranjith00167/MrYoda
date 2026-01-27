# API Endpoints and Payloads Documentation

This document lists the API endpoints and their corresponding request payloads used in the automation framework.

## 1. Authentication

### 1.1 Request OTP
**Endpoint:** `/otps/getOtp`  
**Method:** `POST`

**Payload:**
```json
{
  "mobile": "9003730394",
  "country_code": "+91"
}
```

### 1.2 Verify OTP
**Endpoint:** `/otps/getOtp`  
**Method:** `POST`

**Payload:**
```json
{
  "mobile": "9003730394",
  "country_code": "+91",
  "otp": "123456"
}
```

## 2. User Management

### 2.1 User Registration
**Endpoint:** `/users/addUser`  
**Method:** `POST`

**Payload:**
```json
{
  "first_name": "John",
  "last_name": "Doe",
  "middle_name": "M",
  "gender": "Male",
  "dob": "1990-01-01",
  "mobile": "9876543210",
  "country_code": "+91",
  "email": "john.doe@example.com",
  "profile_pic": "https://example.com/pic.jpg",
  "alt_mobile": "9876543211"
}
```

## 3. Address Management

### 3.1 Add Address
**Endpoint:** `/address/addAddress`  
**Method:** `POST`

**Payload:**
```json
{
  "user_id": "GUID-USER-ID",
  "address_line1": "Hyderabad",
  "receiver_name": "Test User",
  "recipient_mobile_number": "9999999999",
  "name": "Ameerpet (HQ)",
  "type": "home",
  "country_code": "+91",
  "state": "Telangana",
  "postal_code": "500016",
  "country": "India",
  "city": "Hyderabad",
  "latitude": "17.4358447",
  "longitude": "78.452737"
}
```

## 4. Cart Management

### 4.1 Add To Cart (Initial)
**Endpoint:** `/carts/v2/addCart`  
**Method:** `POST`

**Payload:**
```json
{
  "user_id": "GUID-USER-ID",
  "product_details": [
    {
      "product_id": "TEST-ID-1",
      "quantity": 1,
      "type": "home",
      "brand_id": "BRAND-ID",
      "location_id": "LOCATION-ID",
      "family_member_id": ["GUID-USER-ID"]
    }
  ],
  "order_type": "home"
}
```

### 4.2 Update Cart (Slot Selection)
**Endpoint:** `/carts/v2/addCart`  
**Method:** `POST`

**Payload:**
```json
{
  "user_id": "GUID-USER-ID",
  "product_details": [
    {
      "product_id": "TEST-ID-1",
      "quantity": 1,
      "type": "home",
      "brand_id": "BRAND-ID",
      "location_id": "LOCATION-ID",
      "family_member_id": ["GUID-USER-ID"]
    }
  ],
  "slot_guid": "SLOT-GUID",
  "lab_location_id": "LAB-LOCATION-ID",
  "order_type": "home"
}
```

## 5. Slot & Center Management

### 5.1 Get Centers By Address
**Endpoint:** `/slot/getCentersByadd`  
**Method:** `POST`

**Payload:**
```json
{
  "addressid": "ADDRESS-ID",
  "lab_id": "LAB-LOCATION-ID"
}
```

## 6. Order Management

### 6.1 Verify Payment (COD Pre-check)
**Endpoint:** `/gateway/v2/VerifyPayment`  
**Method:** `POST`

**Payload:**
```json
{
  "user_id": "GUID-USER-ID"
}
```

### 6.2 Create Order (Online Payment)
**Endpoint:** `/gateway/v2/CreateOrder`  
**Method:** `POST`

**Payload:**
```json
{
  "cart_id": "CART-GUID",
  "payment_mode": "online",
  "source": "android",
  "user_id": "GUID-USER-ID",
  "address_id": "ADDRESS-ID",
  "slot_id": "SLOT-GUID",
  "date": "2025-12-30",
  "time": "07:00 AM - 08:00 AM",
  "total_amount": 1053,
  "lab_location_id": "LAB-LOCATION-ID",
  "order_type": "home"
}
```

### 6.3 Get Payment By ID
**Endpoint:** `/gateway/getPaymentById`  
**Method:** `POST`

**Payload:**
```json
{
  "id": "PAYMENT-GUID"
}
```
