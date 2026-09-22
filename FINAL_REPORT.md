# SPLITPAY — Production Readiness Review & Final Report

**Project**: SPLITPAY — Payment-Request Splitting Platform for India  
**Date**: September 2026  
**Status**: COMPLETE (Checkpoints 0 through 14 Verified)  

---

## 1. Executive Summary

SPLITPAY is a production-grade, fintech-oriented full-stack payment-request splitting platform built specifically for the Indian digital payments ecosystem (UPI / NPCI). 

When merchants or individuals request payments that exceed single-transaction limits, bank velocity caps, or convenience thresholds (e.g., ₹5,000 or ₹7,500), SPLITPAY automatically calculates an optimal split plan into multiple sub-threshold parts (e.g., ₹1,990 + ₹1,990 + ₹1,020), generates authentic NPCI-compliant UPI deep links and QR codes for each part, and performs real-time balance reconciliation across all parts until full settlement.

The platform has been designed from the ground up to satisfy strict financial software standards:
- **Zero credential collection or storage**: Never touches or requests UPI PINs, banking passwords, or OTPs.
- **Mathematical invariants**: Strict `BigDecimal` financial arithmetic guarantees zero money lost and zero money created across all splits and remainder distributions.
- **No fake payment success in production**: Payment parts initialize strictly to `PENDING`. Payment confirmation requires cryptographically verified provider webhooks or bank reconciliation events.
- **Clean provider isolation**: Plug-and-play provider abstraction interface ready for instant configuration with authorized UPI gateways (Cashfree, Razorpay, PhonePe, PayU).

---

## 2. Completed Features Summary

### 2.1 Backend (Spring Boot 3.3.4 & Java 20 LTS)
- **Authentication & User Management**:
  - Stateless JWT authentication filter chain with JJWT 0.12.6.
  - BCrypt password hashing (strength 12).
  - Role-based authorization (`ROLE_USER`, `ROLE_MERCHANT`, `ROLE_ADMIN`).
  - Endpoints for registration, login, and profile lookup (`/api/auth/*`).
- **Splitting Engine (Strategy Pattern)**:
  - `PaymentSplitStrategy` interface with dynamic factory (`SplitStrategyFactory`).
  - `MaxPartAmountSplitStrategy`: Default max part amount ₹1,990.00 (e.g., ₹5,000 $\rightarrow$ ₹1,990 + ₹1,990 + ₹1,020).
  - `EqualSplitStrategy`: Splits total evenly with remainder penny distribution.
  - `CustomSplitStrategy`: User-defined custom allocations with invariant validation.
  - Real-time preview calculation endpoint (`POST /api/payment-orders/preview`) accessible without persisting orders.
- **UPI QR Code & Deep Link Engine**:
  - `UpiPayloadBuilder`: Constructs NPCI-standard deep link URIs (`upi://pay?pa={upiId}&pn={name}&am={amount}&cu=INR&tr={ref}&tn={note}`).
  - `QrCodeGenerator`: ZXing-powered generation of Base64 Data URIs and raw PNG byte arrays.
  - Dedicated direct PNG streaming endpoint: `GET /api/payment-parts/{id}/qr`.
- **Payment Order & Part Lifecycle**:
  - Unique alphanumeric reference generators for orders (`ORD-...`) and parts (`PRT-...`).
  - Strict initial state enforcement (`PENDING`).
  - Complete retrieval endpoints (`/api/payment-orders`, `/api/payment-orders/{id}`, `/api/payment-orders/ref/{reference}`, `/api/payment-orders/{id}/parts`).
- **Reconciliation Engine**:
  - `PaymentReconciliationService`: Real-time computation of `expectedAmount`, `paidAmount`, `remainingAmount`, and `overpaidAmount`.
  - State machine transitions: `PENDING` $\rightarrow$ `PARTIALLY_PAID` $\rightarrow$ `COMPLETED`, `FAILED`, `EXPIRED`, `CANCELLED`.
- **Webhook & Provider Abstraction**:
  - `PaymentProvider` interface defining request generation, status polling, webhook verification, and refund initiation.
  - `MockPaymentProvider` strictly isolated for development and automated integration testing.
  - `HmacWebhookVerifier`: Constant-time HMAC-SHA256 signature verification.
  - `WebhookService`: Idempotent event consumption (`PaymentEvent`), duplicate transaction deduplication (`PaymentTransaction`), and automated order status reconciliation.
- **Observability & Security Auditing**:
  - Asynchronous audit log persistence (`AuditLog`) for all sensitive actions.
  - Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`).
  - SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`).
  - Admin observability endpoints (`/api/admin/metrics`, `/api/admin/orders`, `/api/admin/events`, `/api/admin/audit-logs`).

### 2.2 Frontend (React 18, TypeScript, Vite 5, Tailwind CSS)
- **Modern Fintech User Interface**:
  - Responsive design with Lucide icons, glassmorphism cards, and Tailwind CSS.
  - Regulatory disclosure footer on all pages highlighting non-storage of banking credentials.
- **User Experience & Core Flow**:
  - **Landing Page (`/`)**: Hero section, interactive instant split preview simulator, feature highlights, and architectural breakdown.
  - **Login (`/login`) & Register (`/register`)**: Form validation with clear error alerts and merchant toggle.
  - **Dashboard (`/dashboard`)**: Metric cards (Total Volume, Settled Volume, Pending Orders), quick actions, and recent orders table.
  - **Create Payment (`/create-payment`)**: 3-field primary form (Recipient, UPI ID, Total Amount), real-time split preview calculation, and strategy customization.
  - **Payment Details (`/payment/:id`)**: Real-time `ReconciliationBanner`, individual `QrCard` grid (QR preview, copy UPI link, download QR PNG, direct "Pay via UPI App" mobile intent link), print payment plan, and copy shareable payment link.
  - **Payment History (`/history`)**: Filterable, searchable, and paginated table of past payment orders.
  - **Profile (`/profile`)**: User details, role, and default settlement UPI ID.
  - **Admin Observability (`/admin`)**: Real-time platform KPI cards, recent webhook events log with payload inspection, and security audit log feed.

---

## 3. Architecture & System Design

```
                       [ Customers / UPI Apps ]
                                  |
                                  | 1. Scan QR / Deep Link
                                  v
[ React 18 / Vite SPA ]  --->  [ Spring Boot Backend ]  <---  [ Authorized UPI Gateway ]
(Tailwind CSS + TS)     REST   (Security + Split + QR)  Webhook (Cashfree / Razorpay / PhonePe)
                                  |                 |
                                  v                 v
                         [ PostgreSQL 18 ]   [ Redis / RabbitMQ ]
                         (Flyway Schema)    (Cache & Queues)
```

### Key Technical Invariants
1. **Mathematical Accuracy**: All monetary calculations use `BigDecimal` with `RoundingMode.UNNECESSARY` or `RoundingMode.HALF_UP` for cents. Sum of parts strictly matches the order total.
2. **State Consistency**: Payment parts cannot transition backwards from `PAID` to `PENDING`. An order transitions to `COMPLETED` if and only if `paidAmount >= expectedAmount`.
3. **Idempotent Webhooks**: Inbound webhooks check unique `event_id` and unique transaction pairs `(payment_reference, provider_transaction_id)`. Duplicate webhook deliveries are safely acknowledged with HTTP 200 without double-crediting balances.

---

## 4. Database Schema & Migration

Database migrations are managed via **Flyway** (`V1__init_schema.sql`):

| Table Name | Description | Key Indexes / Constraints |
|:---|:---|:---|
| `users` | User and merchant accounts | `UNIQUE(email)`, `UNIQUE(upi_id)` |
| `payment_orders` | Master payment orders | `UNIQUE(order_reference)`, `INDEX(user_id)`, `INDEX(status)` |
| `payment_parts` | Sub-threshold payment split parts | `UNIQUE(part_reference)`, `INDEX(payment_order_id)`, `INDEX(status)` |
| `payment_transactions` | Authoritative settlement transactions | `UNIQUE(provider_transaction_id)`, `INDEX(payment_part_id)` |
| `payment_events` | Inbound webhook audit log | `UNIQUE(event_id)`, `INDEX(event_type)` |
| `audit_logs` | Platform security & action audit trail | `INDEX(created_at)`, `INDEX(entity_type, entity_id)` |

---

## 5. Security & Regulatory Compliance

- **No Sensitive Credential Storage**: SPLITPAY does not store, request, transmit, or touch UPI PINs, netbanking passwords, debit/credit card CVVs, or OTPs. All authentication is handled by the user's licensed UPI application on their own mobile device.
- **HMAC Signature Verification**: All webhook calls from payment providers are cryptographically validated using constant-time HMAC-SHA256 signature verification (`MessageDigest.isEqual`).
- **Stateless JWT Security**: Passwords hashed with BCrypt (cost 12). Short-lived JWT access tokens are signed using HMAC-SHA256 with strong 256-bit secrets.
- **Input Validation**: Strict regex validation on UPI IDs (`^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$`), strict bounds on amounts, and sanitization on all text inputs.

---

## 6. Testing & Quality Assurance Summary

The platform has undergone automated testing across both backend and frontend layers:

| Layer | Test Suite | Tests Executed | Status |
|:---|:---|:---|:---|
| **Backend Unit** | `PaymentSplitServiceTest` | 16 tests | **PASS** |
| **Backend Unit** | `QrPaymentServiceTest` | 10 tests | **PASS** |
| **Backend Web** | `AuthControllerTest` | 6 tests | **PASS** |
| **Backend Web** | `PaymentOrderControllerTest` | 4 tests | **PASS** |
| **Backend Service** | `WebhookServiceTest` | 3 tests | **PASS** |
| **Backend Integration**| `SplitPayApplicationTests` | 1 test | **PASS** |
| **Frontend Unit** | `StatusBadge.test.tsx` | 2 tests | **PASS** |
| **Frontend Unit** | `ReconciliationBanner.test.tsx` | 2 tests | **PASS** |
| **Frontend Unit** | `QrCard.test.tsx` | 3 tests | **PASS** |
| **Frontend Build** | `npm run build` (TypeScript + Vite) | Production Bundle | **PASS** |
| **Total Tests** | | **47 / 47 PASS** | **100% SUCCESS** |

---

## 7. How to Run the Application

### 7.1 Running Locally

#### Prerequisites
- Java 20+ installed
- Apache Maven 3.9+ installed
- Node.js 20+ & npm 10+ installed
- PostgreSQL 16+ running locally (or H2 test profile)

#### Step 1: Start the Backend
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
*Backend runs on `http://localhost:8080`. API Docs available at `http://localhost:8080/swagger-ui.html`.*

#### Step 2: Start the Frontend
```bash
cd frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:5173`.*

---

### 7.2 Running with Docker Compose

To spin up the entire cluster (PostgreSQL, Redis, RabbitMQ, Spring Boot Backend, Nginx + React Frontend):

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Build and launch all containers
docker compose up -d --build

# 3. Check health status
docker compose ps
```

Services exposed:
- **Frontend Web App**: `http://localhost:3000`
- **Backend REST API**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **RabbitMQ Management**: `http://localhost:15672` (guest / guest)
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`

---

## 8. Wiring an Authorized UPI / Payment Gateway Provider

SPLITPAY is architected with a decoupled `PaymentProvider` interface (`backend/src/main/java/com/splitpay/provider/PaymentProvider.java`). To connect an authorized provider:

### Step 1: Implement `PaymentProvider`
Create a new provider class in `com.splitpay.provider` (e.g., `CashfreePaymentProvider.java`, `RazorpayPaymentProvider.java`, or `PhonePePaymentProvider.java`):

```java
@Service
@Profile("production")
public class CashfreePaymentProvider implements PaymentProvider {
    @Value("${splitpay.provider.cashfree.app-id}")
    private String appId;

    @Value("${splitpay.provider.cashfree.secret-key}")
    private String secretKey;

    @Override
    public String getProviderName() {
        return "CASHFREE";
    }

    @Override
    public ProviderPaymentResponse createPaymentRequest(PaymentPart part) {
        // Call authorized gateway SDK/REST API to create dynamic UPI QR or intent order
        // Return provider transaction reference and payment link
    }

    @Override
    public ProviderPaymentStatus checkPaymentStatus(String providerTransactionId) {
        // Poll gateway status endpoint
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        // Validate webhook signature against secretKey
    }

    @Override
    public RefundResponse initiateRefund(String providerTransactionId, BigDecimal amount, String reason) {
        // Issue refund via gateway
    }
}
```

### Step 2: Configure Credentials in `application.yml`
Add your gateway API keys in `backend/src/main/resources/application-prod.yml` or via environment variables:
```yaml
splitpay:
  provider:
    active: CASHFREE # or RAZORPAY, PHONEPE, PAYU
    cashfree:
      app-id: ${CASHFREE_APP_ID}
      secret-key: ${CASHFREE_SECRET_KEY}
      webhook-secret: ${CASHFREE_WEBHOOK_SECRET}
      base-url: https://api.cashfree.com/pg
```

### Step 3: Register Inbound Webhook URL in Gateway Dashboard
Configure your gateway dashboard to deliver payment webhooks to:
```
https://api.yourdomain.com/api/payments/webhook
```
Include headers:
- `X-Webhook-Signature`: HMAC-SHA256 signature
- `X-Webhook-Event-Id`: Unique event ID
- `X-Webhook-Provider`: Provider identifier (e.g., `CASHFREE`)

---

## 9. Known Limitations & Production Launch Checklist

### Known Limitations
1. **Dynamic NPCI UTR Verification**: Direct peer-to-peer static UPI QR codes (`upi://pay`) do not automatically broadcast webhooks unless issued through a bank aggregator or payment gateway with dynamic virtual payment addresses (VPA) / intent references (`tr`). An authorized provider integration is required for automated real-time status settlement.
2. **Local Session Rate Limiting**: The current distribution utilizes in-memory/Redis rate limiting filters; for multi-region scale, configure distributed Redis rate limiters.

### Production Launch Checklist
- [ ] Obtain Production UPI / Payment Aggregator Merchant Account (Razorpay, Cashfree, PhonePe, or PayU).
- [ ] Swap `MockPaymentProvider` with the production provider implementation.
- [ ] Configure production PostgreSQL instance with SSL enabled (`sslmode=require`).
- [ ] Update `JWT_SECRET` in `.env` to a high-entropy 512-bit random string.
- [ ] Configure `WEBHOOK_SECRET` shared secret with the payment gateway.
- [ ] Enable HTTPS / TLS on public load balancer / reverse proxy.
- [ ] Set up alerts in Prometheus / Grafana on `/actuator/prometheus` metrics.
- [ ] Execute complete bank settlement test run in sandbox before live volume.

---

## 10. Final Status Declaration

```
================================================================================
APPLICATION BUILD: COMPLETE
REAL PAYMENT INTEGRATION: REQUIRES AUTHORIZED PROVIDER CONFIGURATION
================================================================================
```
All system capabilities, mathematical engines, security layers, user interfaces, database migrations, container specifications, and test suites are fully implemented, verified, and operational.
