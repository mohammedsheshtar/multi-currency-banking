# 💰 Multi-Currency Banking System

A secure Spring Boot application that simulates a real-world multi-currency banking system. Users can create accounts,
deposit, withdraw, transfer funds with currency conversion, earn tier points, and shop using their accumulated points.

---

## 🚀 How to Run the App

### Prerequisites
- Java 17+
- Maven
- PostgreSQL
- IntelliJ IDEA or any IDE
- ExchangeRate-API Key

### Setup

1. **Clone the repo**:
   ```bash
   git clone git@github.com:mohammedsheshtar/multi-currency-banking.git
2. **Open the project in IntelliJ**

3. **locate the main file thats called:**
4.    ```bash
      MultiCurrencyBankingApplication.kt
5. **Click the green run button on the top right corner of your screen**
6. **now use Postman to access the endpoints along with their expected request bodies which can be found below. an example of using an endpoint would be**
    ```bash
   localhost:9000/api/v1/users/kyc

## 📡 API Endpoints

> 🔐 All endpoints below (except for register and login) are protected by JWT authentication.

### 🔑 Authentication
- `POST /authentication/api/v1/authentication/register`
- `POST /api/v1/authentication/login`

### 👤 Users
- `POST /api/v1/users/kyc` — Create or update user KYC
- `GET /api/v1/users/kyc` — Get your KYC data

### 💳 Accounts
- `POST /api/v1/accounts/deposit` — Deposit into an account
- `POST /api/v1/accounts/withdraw` — Withdraw from an account
- `POST /api/v1/accounts/transfer` — Transfer between accounts (with currency conversion)
- `GET /api/v1/accounts/transactions/{accountId}` — View transaction history for an account
- `POST /api/v1/users/accounts/{accountNumber}` — Close a specific account
- `GET /api/v1/users/accounts` — List user bank accounts
- `POST /api/v1/users/accounts` — Create a bank account
- 
### 🎖️ Memberships
- `GET /api/v1/memberships` — Fetch all membership tiers
- `GET /api/v1/memberships/tier/{name}` — Get specific tier by name

### 🛍️ Shop
- `GET /api/v1/shop/items/{accountId}` — List items available for a user’s account tier
- `POST /api/v1/shop/buy` — Buy an item using tier points
- `GET /api/v1/shop/history/{accountId}` — View user's shopping history  

## 🎬 Demo Instructions

1. Register a new user via `/api/v1/authentication/register`
2. Login and copy the JWT token via `/api/v1/authentication/login`
3. Set KYC using `/api/v1/users/kyc`
4. Check you KYC data using `/api/v1/users/kyc` but make sure the request is GET
5. Create a bank account via `/api/v1/users/accounts`
6. List your bank account(s) `/api/v1/users/accounts` but make sure the request is GET
7. Perform deposit/withdraw/transfer operations
8. Earn tier points and auto-promote membership level by making transactions
9. Browse shop via `/api/v1/shop/items/{accountId}`
10. Buy item using points via `/api/v1/shop/buy`
11. View history using `/api/v1/shop/history/{accountId}`

- 📬 Postman Collection:  
  [👉 Download Postman Collection](link-to-your-postman-export)