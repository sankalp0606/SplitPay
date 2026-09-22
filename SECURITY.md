# SPLITPAY: Security & Fintech Compliance Architecture

SPLITPAY treats security, privacy, and regulatory boundaries with the rigor required for commercial fintech infrastructure in India.

---

## 1. Zero Sensitive Credential Storage Policy

> [!CAUTION]
> **Strict Regulatory Prohibition**:
> Under RBI and NPCI guidelines, customer authentication factors must never touch third-party merchant applications.

SPLITPAY enforces strict architectural constraints:
- **NO UPI PIN Collection**: Customers authorize payments exclusively within their own bank-authorized UPI client (Google Pay, PhonePe, Paytm, BHIM, etc.).
- **NO Banking Passwords**: Never requested or processed.
- **NO Card Credentials**: No debit/credit card PAN, CVV, or expiry dates.
- **NO OTP Collection**: No SMS or device OTPs are ever intercepted or stored.
- **NO Private Keys in Code**: All cryptographic secrets are supplied via environment variables (`JWT_SECRET`, `SPLITPAY_WEBHOOK_SECRET`).

---

## 2. Authentication & Authorization

### JWT Token Design
- Stateless authentication using modern JJWT 0.12.x.
- Signed with HMAC-SHA256 using a minimum 256-bit cryptographically secure key.
- Claims include `subject` (UUID), `email`, `role`, and `name`.
- Configurable expiration (default 24 hours).

### Password Security
- Passwords hashed using `BCryptPasswordEncoder` with strength **12**.
- Salting is automatic and cryptographically random for every user.
- Password hashes are excluded from all API response DTOs.

### Role-Based Access Control (RBAC)
- **`ROLE_USER`**: Create payment orders, view own order history.
- **`ROLE_MERCHANT`**: Create merchant-branded orders, configure store UPI ID.
- **`ROLE_ADMIN`**: Read-only observability of metrics, transaction logs, and audit logs.
- Admin role **cannot** arbitrarily mark transactions as paid; settlement is restricted to authoritative provider webhooks.

---

## 3. Webhook Cryptographic Verification

Inbound webhooks at `POST /api/payments/webhook` are protected by:
1. **HMAC-SHA256 Signature Verification**: Inbound payloads are verified against the provider secret.
2. **Constant-Time Comparison**: Signatures are evaluated using `MessageDigest.isEqual()` to prevent timing attack vulnerabilities.
3. **Idempotency Deduplication**: Inbound event IDs are persisted in `payment_events` with unique constraints to reject replay attacks.
4. **Transaction Deduplication**: Transactions are keyed by `(payment_reference, provider_transaction_id)` to prevent double crediting.

---

## 4. Input Sanitization & Attack Mitigation

- **SQL Injection**: Prevented via Spring Data JPA and Hibernate parameterized queries. No raw SQL concatenation.
- **Cross-Site Scripting (XSS)**: All user inputs are sanitized; React escapes rendered DOM content by default.
- **Strict Bean Validation**:
  - UPI ID format: `^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$`
  - Amounts: Strictly positive decimals with maximum scale of 2.
- **Sanitized Error Responses**: Centralized `GlobalExceptionHandler` intercepts exceptions and returns consistent JSON errors without stack traces or class internals.

---

## 5. Security Audit Logging

Security-sensitive events are recorded asynchronously via `AuditService` in the `audit_logs` table:
- User registration and login attempts
- Payment order creation
- Inbound webhook processing
- Status transitions and reconciliation triggers
- Client IP addresses and correlation identifiers
