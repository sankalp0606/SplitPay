# SPLITPAY: REST API Reference Manual

Complete documentation for SPLITPAY REST APIs. All endpoints return and accept JSON unless specified otherwise (e.g. QR PNG image download).

---

## Standard Error Response Format

All error responses return clean JSON without exposing stack traces:

```json
{
  "timestamp": "2026-09-22T12:00:00Z",
  "status": 400,
  "code": "INVALID_UPI_ID",
  "message": "The provided UPI ID is invalid",
  "path": "/api/payment-orders"
}
```

---

## 1. Authentication APIs

### Register Account
`POST /api/auth/register` (Public)

**Request Body:**
```json
{
  "email": "merchant@splitpay.in",
  "password": "StrongPassword123!",
  "fullName": "Rajesh Sharma",
  "role": "MERCHANT",
  "businessName": "Sharma Electronics",
  "defaultUpiId": "sharma@upi"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": "3cdc7760-3045-4e02-a4c8-ff166f3b27fe",
    "email": "merchant@splitpay.in",
    "fullName": "Rajesh Sharma",
    "role": "MERCHANT",
    "businessName": "Sharma Electronics",
    "defaultUpiId": "sharma@upi"
  }
}
```

### User Login
`POST /api/auth/login` (Public)

**Request Body:**
```json
{
  "email": "merchant@splitpay.in",
  "password": "StrongPassword123!"
}
```

### Get Current User Profile
`GET /api/auth/me` (Authenticated)
- Requires `Authorization: Bearer <token>` header.

---

## 2. Payment Order APIs

### Create Payment Order
`POST /api/payment-orders` (Public / Authenticated)

**Request Body:**
```json
{
  "recipientName": "ABC Electronics",
  "upiId": "abcelectronics@upi",
  "totalAmount": 5000.00,
  "splittingStrategy": "MAX_PART_AMOUNT",
  "maxPartAmount": 1990.00,
  "notes": "Store purchase order #9021"
}
```

**Response (201 Created):**
```json
{
  "id": "893c52e4-c11f-4ef6-bdf3-847e06c3a167",
  "orderReference": "ORD-1790078517-3914",
  "recipientName": "ABC Electronics",
  "upiId": "abcelectronics@upi",
  "totalAmount": 5000.00,
  "currency": "INR",
  "status": "PENDING",
  "splittingStrategy": "MAX_PART_AMOUNT",
  "maxPartAmount": 1990.00,
  "paidAmount": 0.00,
  "remainingAmount": 5000.00,
  "partCount": 3,
  "parts": [
    {
      "id": "11d1ef89-3221-4770-bc52-1678da4bca01",
      "partNumber": 1,
      "paymentReference": "SP-1790078517-3914-P1-140",
      "amount": 1990.00,
      "status": "PENDING",
      "upiUri": "upi://pay?pa=abcelectronics@upi&pn=ABC%20Electronics&am=1990.00&cu=INR&tr=SP-1790078517-3914-P1-140",
      "qrDataUri": "data:image/png;base64,iVBORw0KGgo..."
    },
    {
      "id": "22d1ef89-3221-4770-bc52-1678da4bca02",
      "partNumber": 2,
      "paymentReference": "SP-1790078517-3914-P2-291",
      "amount": 1990.00,
      "status": "PENDING",
      "upiUri": "upi://pay?pa=abcelectronics@upi&pn=ABC%20Electronics&am=1990.00&cu=INR&tr=SP-1790078517-3914-P2-291",
      "qrDataUri": "data:image/png;base64,iVBORw0KGgo..."
    },
    {
      "id": "33d1ef89-3221-4770-bc52-1678da4bca03",
      "partNumber": 3,
      "paymentReference": "SP-1790078517-3914-P3-832",
      "amount": 1020.00,
      "status": "PENDING",
      "upiUri": "upi://pay?pa=abcelectronics@upi&pn=ABC%20Electronics&am=1020.00&cu=INR&tr=SP-1790078517-3914-P3-832",
      "qrDataUri": "data:image/png;base64,iVBORw0KGgo..."
    }
  ],
  "createdAt": "2026-09-22T12:00:00Z"
}
```

### Preview Payment Split Plan
`POST /api/payment-orders/preview` (Public)

**Request Body:**
```json
{
  "totalAmount": 7500.00,
  "maxPartAmount": 1990.00
}
```

**Response (200 OK):**
```json
{
  "totalAmount": 7500.00,
  "partCount": 4,
  "parts": [1990.00, 1990.00, 1990.00, 1530.00],
  "splittingStrategy": "MAX_PART_AMOUNT",
  "maxPartAmount": 1990.00
}
```

### Get Order by ID
`GET /api/payment-orders/{id}` (Public)

### List User Orders
`GET /api/payment-orders?page=0&size=10` (Authenticated)

---

## 3. Payment Part APIs

### Get Payment Part Details
`GET /api/payment-parts/{id}` (Public)

### Download Part QR PNG
`GET /api/payment-parts/{id}/qr?width=300&height=300` (Public)
- Produces: `image/png` binary stream.

---

## 4. Webhook API

### Ingest Inbound Payment Event
`POST /api/payments/webhook` (Public / Provider Signed)

**Headers:**
- `X-Webhook-Signature`: `<HMAC-SHA256 signature>`

**Request Body:**
```json
{
  "eventId": "EVT-19028-101",
  "eventType": "PAYMENT.SUCCESS",
  "paymentReference": "SP-1790078517-3914-P1-140",
  "providerTransactionId": "TXN-BANK-10029",
  "amount": 1990.00,
  "status": "SUCCESS"
}
```

**Response (200 OK):**
```json
{
  "status": "ACCEPTED",
  "message": "Webhook received and processed"
}
```

---

## 5. Admin Observability APIs

All admin routes require `ROLE_ADMIN` authentication.

- `GET /api/admin/metrics`: Platform counts of orders by status.
- `GET /api/admin/orders`: Full list of all orders.
- `GET /api/admin/events`: Webhook deduplication log.
- `GET /api/admin/transactions`: All authoritative settled provider transactions.
- `GET /api/admin/audit-logs`: Complete security audit trail.
