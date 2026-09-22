# SPLITPAY: Production-Grade UPI Payment-Request Splitting Platform

![License](https://img.shields.io/badge/License-Proprietary-blue.svg)
![Java](https://img.shields.io/badge/Java-20%20LTS-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)
![React](https://img.shields.io/badge/React-18.3-blue.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5.5-blue.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-blue.svg)
![Tests](https://img.shields.io/badge/Tests-47%20Passed-brightgreen.svg)

**SPLITPAY** is a production-oriented full-stack payment request splitting platform for India. It enables merchants and individuals to split large or multi-part transactions into compliant, bite-sized Unified Payments Interface (UPI) payment requests (e.g., ₹5,000 into ₹1,990 + ₹1,990 + ₹1,020), generate verifiable UPI deep links and QR codes for each part, track individual payment statuses, and reconcile settlements through an isolated, pluggable payment-provider architecture.

---

## Important Regulatory & Architecture Notice

> [!IMPORTANT]
> **Fintech Compliance Boundaries**:
> 1. **Payment Request Generation vs. Settlement**: Generating an NPCI-compliant UPI deep link (`upi://pay`) or QR code represents the creation of a *payment request*. It does **NOT** constitute bank settlement or confirmation of receipt.
> 2. **No Fake Settlement**: Payment parts remain strictly `PENDING` until authoritative settlement confirmation is delivered via verified provider webhooks or bank reconciliation feeds.
> 3. **Zero Sensitive Credential Storage**: SplitPay strictly **NEVER** requests, collects, or stores UPI PINs, banking passwords, debit card PINs, or OTPs. All payment authorizations happen within the customer's own UPI application (e.g., Google Pay, PhonePe, Paytm, BHIM).
> 4. **Replaceable Provider Architecture**: External payment gateway and aggregator integrations are isolated behind the `PaymentProvider` and `WebhookVerifier` abstractions. The application ships with a clearly marked `MockPaymentProvider` for automated testing and local developer sandbox use.

---

## Core Product Workflow

1. **Enter Details**: The user inputs Recipient Name, Payee UPI ID (e.g., `abcelectronics@upi`), and Total Amount (e.g., `5000`).
2. **Calculate Split Plan**: The configurable splitting engine automatically breaks down the amount into compliant parts below the configured ceiling (default ₹1,990.00):
   - **Part 1**: ₹1,990.00
   - **Part 2**: ₹1,990.00
   - **Part 3**: ₹1,020.00
3. **Generate QR Payloads**: Each part is assigned a distinct cryptographically random reference and an authentic NPCI-compliant QR code + deep link (`upi://pay?pa=...&pn=...&am=...&cu=INR&tr=...`).
4. **Scan & Pay**: The customer scans each part using their preferred UPI application.
5. **Authoritative Reconciliation**: Real-time status tracking calculates Expected, Paid, and Remaining balances idempotently.

---

## Technology Stack

### Backend
- **Runtime**: Java 20 LTS (bytecode target 20)
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security 6 with stateless JJWT 0.12.6 & BCrypt password hashing (strength 12)
- **Persistence**: Spring Data JPA & Hibernate 6.5
- **Database Migrations**: Flyway Core & Flyway PostgreSQL
- **Database Drivers**: PostgreSQL 16+ & H2 (for isolated dev/test profiles)
- **QR Engine**: ZXing (Zebra Crossing) 3.5.3 (core & javase)
- **Messaging & Cache**: Spring Data Redis & Spring AMQP (RabbitMQ)
- **Observability**: Spring Boot Actuator, Micrometer Prometheus metrics
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc, AssertJ

### Frontend
- **Framework**: React 18 + TypeScript + Vite 5
- **Styling**: Tailwind CSS with custom fintech theme and sleek dark aesthetics
- **Routing**: React Router DOM 6
- **HTTP Client**: Axios with JWT request & response interceptors
- **Icons**: Lucide React
- **Testing**: Vitest + React Testing Library + JSDOM

### Infrastructure
- **Containerization**: Multi-stage `Dockerfile` for backend and frontend
- **Orchestration**: `docker-compose.yml` for local multi-service stack (PostgreSQL, Redis, RabbitMQ, Backend, Frontend)
- **CI/CD**: GitHub Actions workflow (`.github/workflows/ci.yml`)

---

## Quickstart & Local Setup

### Prerequisites
- Java 20+ JDK
- Apache Maven 3.9+
- Node.js 20+ and npm
- (Optional) Docker & Docker Compose

### 1. Clone & Configure
```bash
git clone https://github.com/your-org/splitpay.git
cd splitpay
cp .env.example .env
```

### 2. Run Backend
```bash
cd backend
mvn clean spring-boot:run -Dspring-boot.run.profiles=local
```
The backend starts at `http://localhost:8080`.
- API Health: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend dev server opens at `http://localhost:5173`.

---

## Running with Docker Compose

Run the entire full-stack application (PostgreSQL, Redis, RabbitMQ, Backend, Frontend) in one command:
```bash
docker compose up --build
```
Services available:
- **Frontend Web UI**: `http://localhost:3000`
- **Backend API**: `http://localhost:8080`
- **RabbitMQ Management Console**: `http://localhost:15672` (guest / guest)
- **PostgreSQL**: `localhost:5432`

---

## Running the Automated Test Suite

### Backend Tests (40 automated tests)
```bash
mvn test --file backend/pom.xml
```
Covers:
- `PaymentSplitServiceTest`: Exact split math, invariant checks (₹1 to ₹125,432.75), decimals, equal splits, custom splits.
- `QrPaymentServiceTest`: NPCI UPI URI generation, regex verification, PNG binary magic bytes.
- `PaymentOrderControllerTest`: End-to-end payment creation, preview calculation, part verification, initial PENDING statuses.
- `AuthControllerTest`: User registration, duplicate conflict prevention, login, JWT issuance, protected endpoint checks.
- `WebhookServiceTest`: Inbound webhook settlement, idempotency check (duplicate event safety), multi-part reconciliation to COMPLETED.

### Frontend Tests (7 component tests)
```bash
npm test --prefix frontend
```
Covers:
- `StatusBadge` states (PENDING, SUCCESS, PARTIALLY_PAID, COMPLETED, FAILED).
- `ReconciliationBanner` calculations and progress percentage.
- `QrCard` render, UPI links, and copy actions.

---

## Documentation Index

- [Architecture Guide](file:///d:/REACT/SplitPay/ARCHITECTURE.md): Layered system design, package structure, and data flows.
- [Payment Flow](file:///d:/REACT/SplitPay/PAYMENT_FLOW.md): Step-by-step payment lifecycle and splitting rules.
- [Fintech Security](file:///d:/REACT/SplitPay/SECURITY.md): Security controls, rate limiting, and audit specifications.
- [Provider Integration Guide](file:///d:/REACT/SplitPay/PROVIDER_INTEGRATION.md): Detailed steps to connect authorized payment gateways.
- [Database Schema](file:///d:/REACT/SplitPay/DATABASE.md): Relational tables, Flyway migrations, and indexing strategies.
- [API Catalog](file:///d:/REACT/SplitPay/API.md): Complete REST endpoint catalog with schemas.
- [Build Progress Log](file:///d:/REACT/SplitPay/docs/BUILD_PROGRESS.md): Autonomous checkpoint build history.
- [Final Report](file:///d:/REACT/SplitPay/FINAL_REPORT.md): Executive summary and production launch checklist.
