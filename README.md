# FinCore — Banking & Digital Payments Platform

A full-stack banking system built to demonstrate Core Java, REST Web Services, relational
Database design, and a React frontend — mirroring the kind of application Wissen Technology
builds for its BFSI clients (Morgan Stanley, Goldman Sachs, Capital One, Fidelity, etc.).

## Features

- Customer registration & JWT-based login
- Fund transfer between accounts with **thread-safe, deadlock-free locking** and **atomic
  rollback** (all-or-nothing transactions — no partial transfers ever persist)
- Rule-based **fraud detection**: flags an account if it makes 5+ transactions in 10 minutes
  (enforced both in the Java service layer and as a MySQL trigger — defense in depth)
- Admin panel: approve new accounts, freeze suspicious accounts, review flagged transactions
- Transaction history / statement view
- Interest calculation via a MySQL stored procedure (cursor-based, applies to all active
  savings accounts)
- Swagger/OpenAPI docs for the REST API

## Tech Stack

| Layer     | Technology                                              |
|-----------|----------------------------------------------------------|
| Backend   | Java 17, Spring Boot 3, Spring Data JPA, Spring Security |
| Auth      | JWT (jjwt)                                                |
| Database  | MySQL 8, stored procedures, triggers, indexes             |
| Frontend  | React 18, Axios, React Router                              |
| Build     | Maven (backend), npm (frontend)                            |

## Project Structure

```
fincore/
├── backend/                  # Spring Boot application
│   ├── pom.xml
│   └── src/main/java/com/fincore/
│       ├── config/            # JWT util, security config, auth filter
│       ├── controller/        # REST controllers
│       ├── dto/                # Request/response objects
│       ├── exception/         # Custom exceptions + global handler
│       ├── model/              # JPA entities (Customer, Account, Transaction)
│       ├── repository/        # Spring Data repositories
│       └── service/            # Business logic (AuthService, AccountService,
│                                 TransactionService — the core transfer logic lives here)
├── frontend/                 # React application
│   └── src/
│       ├── components/        # Login, Dashboard, TransferForm, TransactionHistory, AdminPanel
│       ├── api.js              # Axios client with JWT interceptor
│       └── App.js
└── database/
    └── schema.sql             # Tables, stored procedures, triggers, seed data
```

## How the Core Java Concepts Show Up

- **OOP & inheritance-friendly design:** `Account` models SAVINGS/CURRENT via an enum;
  entities use encapsulation with private fields + getters/setters.
- **Custom exceptions:** `InsufficientFundsException`, `AccountFrozenException`,
  `AccountNotFoundException`, `InvalidCredentialsException`, all handled centrally by
  `GlobalExceptionHandler`.
- **Multithreading / concurrency:** `TransactionService.transfer()` uses a per-account
  `ReentrantLock` (via `ConcurrentHashMap`) and always locks accounts in a **consistent order**
  to avoid deadlocks when two transfers happen in opposite directions at the same time.
- **Transactional integrity:** the whole transfer method is wrapped in `@Transactional` —
  if any step fails, Spring rolls back every change (balances + transaction record) together.

## Setup

### 1. Database
```bash
mysql -u root -p < database/schema.sql
```
This creates `fincore_db`, all tables, the two stored procedures, and the two triggers.

### 2. Backend
```bash
cd backend
# edit src/main/resources/application.properties with your MySQL username/password
mvn spring-boot:run
```
API runs on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend
```bash
cd frontend
npm install
npm start
```
App runs on `http://localhost:3000`.

### 4. Try it out
1. Register a new customer (creates a PENDING_APPROVAL account automatically).
2. Log in as `admin` (seeded in `schema.sql`) — **before first login, generate a real
   BCrypt hash** for the admin password and replace the placeholder hash in `schema.sql`
   (a two-line Java snippet using `BCryptPasswordEncoder` will do it, or use an online
   BCrypt generator for local testing only).
3. As admin, approve the new customer's account.
4. Log back in as the customer and transfer funds to another approved account.
5. Fire 5+ transfers quickly from the same account to see the fraud flag trigger.

## API Endpoints (summary)

| Method | Endpoint                              | Access  |
|--------|-----------------------------------------|---------|
| POST   | /api/auth/register                       | Public  |
| POST   | /api/auth/login                          | Public  |
| GET    | /api/accounts/my                          | Customer|
| GET    | /api/accounts/{accountNumber}             | Auth'd  |
| POST   | /api/transactions/transfer                | Auth'd  |
| GET    | /api/transactions/history/{accountNumber} | Auth'd  |
| GET    | /api/admin/accounts/pending               | Admin   |
| PUT    | /api/admin/accounts/{id}/approve          | Admin   |
| PUT    | /api/admin/accounts/{id}/freeze           | Admin   |
| GET    | /api/admin/transactions/flagged           | Admin   |

## Notes

This is a portfolio/demo project meant to showcase full-stack banking-domain engineering
(built with Wissen Technology's stated stack — Core Java, Web Services, Databases, React —
and BFSI domain in mind). It is not production-hardened: for real use you would add rate
limiting, refresh tokens, proper secrets management, input sanitization beyond basic
validation, and audit logging on every state change.
