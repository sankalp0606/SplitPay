# SPLITPAY: System Architecture Documentation

This document describes the high-level architecture, module decomposition, and interaction patterns of the SPLITPAY platform.

---

## 1. High-Level Architecture Diagram

```
+------------------------------------------------------------------------------------+
|                                  USER / CLIENT                                     |
|             Desktop Browser / Mobile Browser / Native UPI App Scanner              |
+------------------------------------------+-----------------------------------------+
                                           |
                                           | HTTPS / REST (JWT Auth)
                                           v
+------------------------------------------------------------------------------------+
|                               FRONTEND APPLICATION                                 |
|         React 18 + TypeScript + Vite + Tailwind CSS + TanStack Query + Axios       |
|                                                                                    |
|  - Create Payment (Minimal 3 fields: Recipient, Payee UPI ID, Total Amount)        |
|  - Live Interactive Split Preview Calculator                                       |
|  - Payment Order Overview (Parts, Status, QR Cards, UPI Intent Links)              |
|  - Reconciliation Banner (Total Expected, Total Paid, Remaining Balance)           |
|  - Dashboard, Order History, User Profile, and Admin Observability Panel           |
+------------------------------------------+-----------------------------------------+
                                           |
                                           | JSON / REST API (/api/*)
                                           v
+------------------------------------------------------------------------------------+
|                                BACKEND APPLICATION                                 |
|                       Spring Boot 3.3.x (Java 20 LTS Runtime)                      |
|                                                                                    |
|  +------------------------------------------------------------------------------+  |
|  | Security & Filter Layer                                                      |  |
|  | - JwtAuthenticationFilter (Stateless token resolution)                        |  |
|  | - SecurityConfig (Route authorization, CORS headers, Exception EntryPoints)  |  |
|  | - GlobalExceptionHandler (Sanitized JSON responses without stack traces)     |  |
|  +------------------------------------------------------------------------------+  |
|                                                                                    |
|  +------------------------------------------------------------------------------+  |
|  | Controller Layer                                                             |  |
|  | - AuthController (/api/auth/register, /api/auth/login, /api/auth/me)         |  |
|  | - PaymentOrderController (/api/payment-orders, /preview, /{id})              |  |
|  | - PaymentPartController (/api/payment-parts/{id}, /{id}/qr)                   |  |
|  | - WebhookController (/api/payments/webhook)                                  |  |
|  | - AdminController (/api/admin/metrics, /orders, /events, /audit-logs)        |  |
|  | - HealthController (/api/health)                                             |  |
|  +------------------------------------------------------------------------------+  |
|                                                                                    |
|  +------------------------------------------------------------------------------+  |
|  | Core Domain & Business Services                                              |  |
|  | - PaymentOrderService: Orchestrates order lifecycle and part generation      |  |
|  | - PaymentSplitService: Configurable strategy pattern splitting engine        |  |
|  | - QrPaymentService: NPCI UPI URI formatting and ZXing QR generation          |  |
|  | - PaymentReconciliationService: Multi-part balance & state reconciliation    |  |
|  | - WebhookService: Webhook verification, idempotency deduplication, settlement |  |
|  | - AuditService: Asynchronous security and audit event persistence            |  |
|  +------------------------------------------------------------------------------+  |
|                                                                                    |
|  +------------------------------------------------------------------------------+  |
|  | External Provider Abstraction Layer                                         |  |
|  | - PaymentProvider (Interface: createPaymentRequest, checkStatus, verify)     |  |
|  | - WebhookVerifier (Interface: HMAC-SHA256 signature verification)            |  |
|  | - MockPaymentProvider (Strictly isolated for automated tests/dev sandbox)   |  |
|  | - [Future Provider]: Razorpay, Cashfree, PayU, PhonePe PG, ICICI Eazypay     |  |
|  +------------------------------------------------------------------------------+  |
+------------------------------------------+-----------------------------------------+
                                           |
                                           v
+------------------------------------------------------------------------------------+
|                               PERSISTENCE & STORAGE                                |
|  - PostgreSQL 16+ (Durable source of truth via Flyway migrations)                  |
|    * users, payment_orders, payment_parts, payment_transactions,                   |
|      payment_events, audit_logs                                                    |
|  - Redis 7 (Caching, idempotency keys, rate limiting)                             |
|  - RabbitMQ 3.13 (Asynchronous webhook/event pipeline)                             |
+------------------------------------------------------------------------------------+
```

---

## 2. Package Architecture (Backend)

The backend follows a clean, layered, domain-centric package structure:

```
com.splitpay
├── SplitPayApplication.java         # Spring Boot main runner
├── config                           # Application configuration
│   ├── SecurityConfig.java          # Spring Security 6 filter chain & CORS
│   └── WebConfig.java               # Web MVC configurations
├── security                         # Authentication & authorization internals
│   ├── JwtTokenProvider.java        # JJWT token generation and validation
│   ├── JwtAuthenticationFilter.java  # HTTP Bearer token filter
│   ├── CustomUserDetailsService.java # User resolution from database
│   └── UserPrincipal.java           # UserDetails implementation
├── entity                           # JPA relational database entities
│   ├── User.java                    # System user / merchant entity
│   ├── Role.java                    # USER, MERCHANT, ADMIN
│   ├── PaymentOrder.java            # Master payment order
│   ├── PaymentOrderStatus.java      # PENDING, PARTIALLY_PAID, COMPLETED, FAILED, EXPIRED, CANCELLED
│   ├── PaymentPart.java             # Individual split part
│   ├── PaymentPartStatus.java       # PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED
│   ├── SplittingStrategyType.java   # MAX_PART_AMOUNT, EQUAL_SPLIT, CUSTOM_SPLIT
│   ├── PaymentTransaction.java      # Authoritative settlement transaction records
│   ├── TransactionStatus.java       # SUCCESS, FAILED, PENDING, REVERSED
│   ├── PaymentEvent.java            # Inbound webhook deduplication store
│   └── AuditLog.java                # Immutable audit log
├── repository                       # Spring Data JPA data access interfaces
│   ├── UserRepository.java
│   ├── PaymentOrderRepository.java
│   ├── PaymentPartRepository.java
│   ├── PaymentTransactionRepository.java
│   ├── PaymentEventRepository.java
│   └── AuditLogRepository.java
├── split                            # Configurable splitting engine
│   ├── PaymentSplitStrategy.java    # Strategy interface
│   ├── MaxPartAmountSplitStrategy.java
│   ├── EqualSplitStrategy.java
│   ├── CustomSplitStrategy.java
│   ├── SplitStrategyFactory.java    # Strategy resolver factory
│   └── PaymentSplitService.java     # Splitting facade
├── qr                               # UPI URI and QR Code engine
│   ├── UpiPayloadBuilder.java       # NPCI upi://pay URI generator
│   ├── QrCodeGenerator.java         # ZXing PNG and Data URI encoder
│   ├── QrPaymentResult.java         # Value object
│   ├── QrPaymentService.java        # Replaceable QR service interface
│   └── DefaultQrPaymentService.java # Standard implementation
├── provider                         # External Payment Gateway Abstraction
│   ├── PaymentProvider.java         # Gateway interface
│   ├── MockPaymentProvider.java     # Dev/test mock implementation
│   ├── PaymentProviderService.java  # Provider router
│   ├── ProviderPaymentRequest.java
│   ├── ProviderPaymentResponse.java
│   ├── PaymentStatusResponse.java
│   └── WebhookVerificationResult.java
├── webhook                          # Webhook ingestion pipeline
│   ├── WebhookVerifier.java         # Signature verification interface
│   ├── HmacWebhookVerifier.java     # Constant-time HMAC-SHA256 verifier
│   └── WebhookController.java       # Webhook endpoint POST /api/payments/webhook
├── audit                            # Audit service
│   └── AuditService.java            # Asynchronous audit event recorder
├── dto                              # Data Transfer Objects
│   ├── auth                         # Registration, Login, UserSummary
│   ├── payment                      # CreateOrder, PaymentOrderDto, PartDto, Preview
│   └── common                       # ErrorResponse, ApiResponse
├── exception                        # Centralized exception hierarchy
│   ├── AppException.java
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── UnauthorizedException.java
│   ├── InvalidUpiException.java
│   ├── PaymentValidationException.java
│   └── GlobalExceptionHandler.java  # Sanitized JSON error handler
└── controller                       # REST API controllers
    ├── AuthController.java
    ├── PaymentOrderController.java
    ├── PaymentPartController.java
    ├── AdminController.java
    └── HealthController.java
```

---

## 3. Splitting Engine Design

The splitting engine decouples mathematical partitioning from order storage:

```
                  +--------------------------------+
                  |     PaymentSplitService        |
                  +---------------+----------------+
                                  |
                                  v
                  +---------------+----------------+
                  |     SplitStrategyFactory       |
                  +---------------+----------------+
                                  |
          +-----------------------+-----------------------+
          |                                               |
          v                                               v
+-------------------------------+             +-------------------------------+
|  MaxPartAmountSplitStrategy   |             |      EqualSplitStrategy       |
|                               |             |                               |
| Splits amounts into slices    |             | Divides amount into N equal   |
| <= maxPartAmount (e.g. ₹1,990)|             | parts, distributing remainder |
| ₹5000 -> 1990, 1990, 1020     |             | cents evenly (zero loss/gain) |
+-------------------------------+             +-------------------------------+
```

### Core Invariants:
1. `Sum(parts) == TotalAmount` exactly, evaluated with `BigDecimal` (`scale = 2`, `RoundingMode.HALF_UP`).
2. Every part amount must be strictly greater than zero (`part > 0`).
3. Zero money created, zero money lost.
4. Non-positive total amounts are rejected with `PaymentValidationException`.

---

## 4. UPI QR & Deep Link Pipeline

```
+------------------+     +-----------------------+     +-----------------------+
|  Payee UPI ID    | --> |   UpiPayloadBuilder   | --> |      QrCodeGenerator  |
|  Payee Name      |     | (Validates UPI regex, |     |   (ZXing QRCodeWriter |
|  Part Amount     |     |  URL-encodes params,  |     |   outputs PNG bytes & |
|  Part Reference  |     |  formats upi://pay)   |     |   Base64 Data URI)    |
+------------------+     +-----------------------+     +-----------+-----------+
                                                                   |
                                                                   v
                                                       +-----------+-----------+
                                                       |   Rendered on Mobile  |
                                                       |   or Downloadable PNG |
                                                       +-----------------------+
```

---

## 5. Webhook Ingestion & Deduplication Pipeline

```
Webhook POST /api/payments/webhook
       │
       ▼
[Verify Signature] ──── (Invalid) ────► Return HTTP 400 (INVALID_WEBHOOK_SIGNATURE)
       │ (Valid)
       ▼
[Idempotency Check] ──── (Event Exists) ────► Return HTTP 200 (Already Processed)
       │ (New Event)
       ▼
[Persist PaymentEvent]
       │
       ▼
[Txn Deduplication] ──── (Txn Exists) ────► Return HTTP 200 (Prevent double credit)
       │ (New Txn)
       ▼
[Persist PaymentTransaction]
       │
       ▼
[Transition PaymentPart Status] (e.g. PENDING -> SUCCESS)
       │
       ▼
[Reconcile PaymentOrder] (e.g. PENDING -> PARTIALLY_PAID / COMPLETED)
       │
       ▼
[Asynchronous Security Audit Log]
       │
       ▼
Return HTTP 200 {"status": "ACCEPTED"}
```
