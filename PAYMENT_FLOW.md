# SPLITPAY: Payment Flow & Splitting Engine Lifecycle

This document provides a detailed specification of the payment lifecycle, splitting algorithm, status state machine, and reconciliation logic in SPLITPAY.

---

## 1. End-to-End Payment Request Lifecycle

```
[Merchant / User]
       │
       │ 1. Enter: Recipient Name ("ABC Electronics"), UPI ID ("abcelectronics@upi"), Amount (5000.00)
       ▼
[PaymentOrderService]
       │
       │ 2. Validates UPI format: ^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$
       │ 3. Invokes PaymentSplitService: splits ₹5,000 into ₹1,990 + ₹1,990 + ₹1,020
       │ 4. Generates unique order reference: ORD-1790078517-3914
       │ 5. Generates unique part references: SP-1790078517-3914-P1-140, etc.
       │ 6. Builds NPCI UPI deep links: upi://pay?pa=abcelectronics@upi&pn=...&am=1990.00&tr=...
       │ 7. Generates ZXing QR PNG byte array and Base64 Data URI
       │ 8. Persists PaymentOrder and 3 PaymentParts with status = PENDING
       ▼
[Payment Order Details Page]
       │
       │ Displays Payment Summary:
       │ Total: ₹5,000 | Paid: ₹0.00 | Remaining: ₹5,000 | Status: PENDING
       │ Part 1: ₹1,990 [Show QR] [Pay via UPI App]
       │ Part 2: ₹1,990 [Show QR] [Pay via UPI App]
       │ Part 3: ₹1,020 [Show QR] [Pay via UPI App]
       ▼
[Customer Interaction]
       │ Customer opens their existing UPI App (GPay / PhonePe / Paytm / BHIM)
       │ Customer scans Part 1 QR or clicks deep link
       │ Customer authorizes ₹1,990 inside their bank-authorized UPI app
       ▼
[Payment Provider / Aggregator]
       │ Authoritative settlement occurs through banking rails
       │ Provider sends authoritative webhook:
       │ POST /api/payments/webhook
       │ Payload: {"eventId": "EVT-101", "paymentReference": "SP-...-P1-...", "amount": 1990.00, "status": "SUCCESS"}
       ▼
[WebhookService Pipeline]
       │ 1. Verifies HMAC-SHA256 signature
       │ 2. Idempotency check: eventId "EVT-101" is recorded
       │ 3. Deduplication: txnId "TXN-101" is recorded
       │ 4. Transitions Part 1 status: PENDING ──► SUCCESS
       │ 5. Invokes PaymentReconciliationService:
       │    Paid = ₹1,990 | Remaining = ₹3,010
       │    Transitions Order status: PENDING ──► PARTIALLY_PAID
       ▼
[Customer Scans & Pays Remaining Parts]
       │ Part 2 settled ──► Paid = ₹3,980 | Remaining = ₹1,020 (PARTIALLY_PAID)
       │ Part 3 settled ──► Paid = ₹5,000 | Remaining = ₹0.00 (COMPLETED)
       ▼
[Final State]
       All parts settled. Order status = COMPLETED.
```

---

## 2. Splitting Engine Rules & Examples

The splitting engine enforces mathematical exactness using `BigDecimal` arithmetic with `RoundingMode.HALF_UP`.

### Default Strategy: `MAX_PART_AMOUNT`
Divides the total amount into chunks less than or equal to `maxPartAmount` (configurable, default ₹1,990.00).

#### Example 1: ₹5,000.00
```
Total: ₹5,000.00
maxPartAmount: ₹1,990.00

Part 1: ₹1,990.00  (Remaining: ₹3,010.00)
Part 2: ₹1,990.00  (Remaining: ₹1,020.00)
Part 3: ₹1,020.00  (Remaining: ₹0.00)

Sum: 1990.00 + 1990.00 + 1020.00 = 5000.00 exactly.
Parts Count: 3
```

#### Example 2: ₹7,500.00
```
Total: ₹7,500.00
maxPartAmount: ₹1,990.00

Part 1: ₹1,990.00
Part 2: ₹1,990.00
Part 3: ₹1,990.00
Part 4: ₹1,530.00

Sum: 1990 + 1990 + 1990 + 1530 = 7500.00 exactly.
Parts Count: 4
```

#### Example 3: Decimal Amount ₹5,000.50
```
Total: ₹5,000.50
maxPartAmount: ₹1,990.00

Part 1: ₹1,990.00
Part 2: ₹1,990.00
Part 3: ₹1,020.50

Sum: 1990.00 + 1990.00 + 1020.50 = 5000.50 exactly.
```

#### Example 4: Amount less than maximum ₹1,000.00
```
Total: ₹1,000.00
maxPartAmount: ₹1,990.00

Part 1: ₹1,000.00
Parts Count: 1
```

---

## 3. Reconciliation State Machine

### PaymentPart Statuses:
- `PENDING`: Initial state upon creation.
- `SUCCESS`: Confirmed by verified provider webhook.
- `FAILED`: Explicit failure notification from payment provider.
- `EXPIRED`: Transaction window elapsed.
- `CANCELLED`: User or merchant cancelled request.

### PaymentOrder Statuses:
- `PENDING`: All parts are PENDING.
- `PARTIALLY_PAID`: At least one part is SUCCESS, but Total Paid < Total Amount.
- `COMPLETED`: Total Paid >= Total Amount.
- `FAILED`: All parts are FAILED.
- `EXPIRED`: All parts are EXPIRED.
- `CANCELLED`: Order was cancelled.

---

## 4. Idempotency & Edge Cases

| Scenario | System Behavior |
| :--- | :--- |
| **Duplicate Webhook Delivery** | Webhook is matched against existing `eventId` in `payment_events`. Handled idempotently, returning HTTP 200 without double crediting. |
| **Duplicate Provider Transaction** | Transaction is matched against `payment_transactions(payment_reference, provider_transaction_id)`. Discarded if already recorded. |
| **Partial Payments** | Order status reflects `PARTIALLY_PAID`; remaining balance is clearly calculated and displayed. |
| **Out-of-Order Webhooks** | Parts can settle in any order (e.g. Part 3 settled before Part 1). Reconciliation dynamically recalculates total paid. |
| **Invalid UPI Format** | Rejected with HTTP 400 `INVALID_UPI_ID` during order creation; no invalid QR codes are ever generated. |
| **Zero or Negative Amount** | Rejected with HTTP 400 `VALIDATION_FAILED` or HTTP 422 `PAYMENT_VALIDATION_ERROR`. |
