# FinCore — Enterprise Banking & Digital Payments Platform

A full-stack core banking system built with **Java, Spring Boot, MySQL, and React**, simulating real-world banking operations — fund transfers, fraud detection, role-based access control, and transaction integrity — the kind of system used by BFSI (Banking, Financial Services & Insurance) organizations.
<img width="1072" height="496" alt="image" src="https://github.com/user-attachments/assets/c3f41c21-9646-475e-bfbd-f56587ebdf98" />

## Overview

FinCore models core banking workflows end-to-end:

- Customers can register, get their account approved by an admin, and transfer funds securely
- Every transfer is **thread-safe** and **atomic** — no partial transactions are ever persisted
- A **rule-based fraud detection engine** flags suspicious transaction patterns in real time
- Admins can approve new accounts, freeze suspicious ones, and review flagged transactions

This project was built to demonstrate backend engineering depth (concurrency, transactional integrity, secure API design) alongside full-stack delivery (REST APIs + database design + React UI).

---

## Tech Stack

| Layer          | Technology                                              |
|----------------|-----------------------------------------------------------|
| Backend        | Java 17, Spring Boot 3, Spring Data JPA, Spring Security   |
| Authentication | JWT (JSON Web Tokens)                                       |
| Database       | MySQL 8 — normalized schema, stored procedures, triggers    |
| Frontend       | React 18, Axios, React Router                                |
| Build Tools    | Maven (backend), npm (frontend)                                |
| API Docs       | Swagger / OpenAPI                                                |

---

## Key Engineering Highlights

### 1. Thread-Safe Concurrent Fund Transfers
Two customers can attempt to transfer money into or out of the same account simultaneously. FinCore uses **per-account `ReentrantLock`s**, always acquired in a **consistent order** (alphabetical by account number) to prevent deadlocks when two transfers happen in opposite directions at once.

### 2. Atomic Transactions (ACID Compliance)
Every transfer is wrapped in a `@Transactional` boundary. If any step fails — insufficient funds, a frozen account, a system error — **all changes roll back together**. No half-completed transfer is ever saved.

### 3. Rule-Based Fraud Detection (defense in depth)
Implemented at **two layers** for redundancy:
- **Application layer:** flags an account if it makes 5+ transactions within a 10-minute rolling window
- **Database layer:** a MySQL trigger performs the same check independently, so fraud detection doesn't rely solely on the application tier

### 4. Role-Based Access Control (RBAC)
JWT tokens carry a `role` claim (`CUSTOMER` / `ADMIN`). Spring Security enforces route-level access — admin endpoints (`/api/admin/**`) reject any non-admin token at the filter level, before it ever reaches business logic.

### 5. Database Design
- 3NF-normalized schema across `customers`, `accounts`, `transactions`, and `audit_log`
- Stored procedures for monthly interest calculation and account statement generation
- Triggers for fraud flagging and blocking transactions on frozen/closed accounts
- Indexes on frequently queried columns for performance

---

## Project Structure

```
fincore/
├── backend/                          # Spring Boot application
│   ├── pom.xml
│   └── src/main/java/com/fincore/
│       ├── config/                    # JWT utility, security config, auth filter
│       ├── controller/                # REST controllers (Auth, Account, Transaction, Admin)
│       ├── dto/                        # Request/response objects
│       ├── exception/                 # Custom exceptions + centralized error handling
│       ├── model/                      # JPA entities — Customer, Account, Transaction
│       ├── repository/                # Spring Data JPA repositories
│       └── service/                    # Business logic layer (core transfer logic lives here)
│
├── frontend/                         # React application
│   └── src/
│       ├── components/                 # Login, Dashboard, TransferForm, TransactionHistory, AdminPanel
│       ├── api.js                       # Axios client with JWT interceptor
│       └── App.js
│
└── database/
    └── schema.sql                      # Tables, stored procedures, triggers, seed data
```

---

## Getting Started

### Prerequisites
- Java 17+
- Maven
- MySQL 8+
- Node.js 18+

### 1. Set up the database
```bash
mysql -u root -p < database/schema.sql
```

### 2. Configure the backend
Edit `backend/src/main/resources/application.properties` with your MySQL credentials:
```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Run the backend
```bash
cd backend
mvn spring-boot:run
```
API available at `http://localhost:8080` · Swagger docs at `/swagger-ui.html`

### 4. Run the frontend
```bash
cd frontend
npm install
npm start
```

---

## API Reference

| Method | Endpoint                                   | Access     |
|--------|-----------------------------------------------|------------|
| POST   | `/api/auth/register`                            | Public     |
| POST   | `/api/auth/login`                                | Public     |
| GET    | `/api/accounts/my`                                | Customer   |
| GET    | `/api/accounts/{accountNumber}`                   | Authenticated |
| POST   | `/api/transactions/transfer`                       | Authenticated |
| GET    | `/api/transactions/history/{accountNumber}`         | Authenticated |
| GET    | `/api/admin/accounts/pending`                       | Admin      |
| PUT    | `/api/admin/accounts/{id}/approve`                   | Admin      |
| PUT    | `/api/admin/accounts/{id}/freeze`                     | Admin      |
| GET    | `/api/admin/transactions/flagged`                       | Admin      |

---

## Future Enhancements

- [ ] Scheduled interest disbursement via Spring `@Scheduled` jobs
- [ ] Unit & integration tests (JUnit + Mockito) covering concurrent transfer scenarios
- [ ] Redis caching for account balance reads
- [ ] Rate limiting on transfer endpoints
- [ ] Dockerized deployment (backend + frontend + MySQL via Docker Compose)

---

## Disclaimer

This is a portfolio/demonstration project and is **not production-hardened**. It does not include production-grade secrets management, refresh token rotation, or comprehensive input sanitization beyond basic validation.

---

## Author

**Garima Upadhyay**
B.Tech Computer Science, JECRC University
[LinkedIn](#) · [GitHub](https://github.com/garimaupadhyayy)
