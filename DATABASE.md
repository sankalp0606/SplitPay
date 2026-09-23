git a# SPLITPAY: Database Schema & Migration Guide

SPLITPAY utilizes PostgreSQL 16+ as its durable source of truth. All schema management is executed via Flyway versioned migrations.

---

## 1. Entity-Relationship (ER) Diagram

```
+------------------------------------+          +------------------------------------+
|               users                |          |           payment_orders           |
+------------------------------------+          +------------------------------------+
| PK  id               UUID          | 1      * | PK  id               UUID          |
|     email            VARCHAR(255)  |<---------| FK  user_id          UUID          |
|     password_hash    VARCHAR(255)  |          |     order_reference  VARCHAR(64)   |
|     full_name        VARCHAR(255)  |          |     recipient_name   VARCHAR(255)  |
|     role             VARCHAR(50)   |          |     upi_id           VARCHAR(255)  |
|     business_name    VARCHAR(255)  |          |     total_amount     NUMERIC(15,2) |
|     default_upi_id   VARCHAR(255)  |          |     status           VARCHAR(50)   |
|     enabled          BOOLEAN       |          |     splitting_str..  VARCHAR(50)   |
|     created_at       TIMESTAMPTZ   |          |     created_at       TIMESTAMPTZ   |
+------------------------------------+          +------------------------------------+
                                                                   | 1
                                                                   |
                                                                   | *
+------------------------------------+          +------------------------------------+
|        payment_transactions        |          |           payment_parts            |
+------------------------------------+          +------------------------------------+
| PK  id               UUID          | *      1 | PK  id               UUID          |
| FK  payment_part_id  UUID          |--------->| FK  payment_order_id UUID          |
|     payment_refer..  VARCHAR(64)   |          |     part_number      INTEGER       |
|     provider_txn_id  VARCHAR(128)  |          |     payment_refer..  VARCHAR(64)   |
|     provider_name    VARCHAR(64)   |          |     amount           NUMERIC(15,2) |
|     amount           NUMERIC(15,2) |          |     status           VARCHAR(50)   |
|     status           VARCHAR(50)   |          |     upi_uri          TEXT          |
|     created_at       TIMESTAMPTZ   |          |     qr_payload       TEXT          |
+------------------------------------+          +------------------------------------+

+------------------------------------+          +------------------------------------+
|           payment_events           |          |             audit_logs             |
+------------------------------------+          +------------------------------------+
| PK  id               UUID          |          | PK  id               UUID          |
|     event_id         VARCHAR(128)  |          |     user_id          UUID          |
|     event_type       VARCHAR(64)   |          |     action           VARCHAR(128)  |
|     provider_name    VARCHAR(64)   |          |     resource_type    VARCHAR(64)   |
|     payload          TEXT          |          |     resource_id      VARCHAR(64)   |
|     processed        BOOLEAN       |          |     ip_address       VARCHAR(64)   |
|     created_at       TIMESTAMPTZ   |          |     created_at       TIMESTAMPTZ   |
+------------------------------------+          +------------------------------------+
```

---

## 2. Table Specifications

### `users`
Stores user and merchant account profiles.
- `id` (UUID, Primary Key)
- `email` (VARCHAR(255), Unique, Not Null)
- `password_hash` (VARCHAR(255), Not Null) - BCrypt hash
- `role` (VARCHAR(50), Not Null) - `USER`, `MERCHANT`, `ADMIN`

### `payment_orders`
Stores top-level split payment requests.
- `order_reference` (VARCHAR(64), Unique, Not Null) - Human-readable identifier e.g. `ORD-1790078517-3914`
- `total_amount` (NUMERIC(15, 2), Not Null) - Strictly positive
- `status` (VARCHAR(50), Not Null) - `PENDING`, `PARTIALLY_PAID`, `COMPLETED`, `FAILED`, `EXPIRED`, `CANCELLED`

### `payment_parts`
Stores individual slices of an order.
- `payment_reference` (VARCHAR(64), Unique, Not Null) - Distinct per part e.g. `SP-17900-P1-492`
- `amount` (NUMERIC(15, 2), Not Null) - Strictly positive
- `status` (VARCHAR(50), Not Null) - `PENDING`, `SUCCESS`, `FAILED`, `EXPIRED`
- `upi_uri` (TEXT, Not Null) - Standards-compliant NPCI UPI deep link
- `qr_payload` (TEXT, Not Null) - Base64 Data URI of the generated QR code

### `payment_transactions`
Stores authoritative financial confirmation records received from payment providers.
- `payment_reference` (VARCHAR(64), Not Null)
- `provider_transaction_id` (VARCHAR(128)) - Gateway transaction identifier
- `status` (VARCHAR(50), Not Null) - `SUCCESS`, `FAILED`, `PENDING`

### `payment_events`
Inbound webhook idempotency store for deduplicating incoming events.
- `event_id` (VARCHAR(128), Unique, Not Null)
- `processed` (BOOLEAN, Not Null) - Flag indicating whether event has been reconciled

### `audit_logs`
Immutable record of security and administrative actions.
- `action` (VARCHAR(128), Not Null)
- `ip_address` (VARCHAR(64))
- `details` (TEXT)

---

## 3. Indexes & Constraints

- `idx_payment_orders_ref` on `payment_orders(order_reference)`
- `idx_payment_orders_user` on `payment_orders(user_id)`
- `idx_payment_parts_ref` on `payment_parts(payment_reference)`
- `idx_payment_parts_order` on `payment_parts(payment_order_id)`
- `idx_payment_tx_ref` on `payment_transactions(payment_reference)`
- `idx_payment_events_id` on `payment_events(event_id)` (Unique constraint enforces deduplication)
- Check constraints: `chk_order_amount_pos` (`total_amount > 0`) and `chk_part_amount_pos` (`amount > 0`).
