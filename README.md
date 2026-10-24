# Digital Payment Fraud Detection Using Machine Learning and Java

**NexaPay | FraudShield**

A full-stack digital payment demonstration application with a machine-learning-based fraud monitoring system. The project combines explainable fraud detection rules, a Random Forest demonstration model, and customer-specific behavioural analysis to identify unusual payment activity and support verification or analyst review.

## Overview

Digital payment systems must identify potentially fraudulent transactions without treating every unusual payment as fraud.

This project demonstrates an approach to payment fraud monitoring through **NexaPay**, a demonstration banking and payment application, and **FraudShield**, its fraud detection and analyst review component.

The system evaluates payment activity using deterministic rules, a Random Forest model trained on synthetic teaching data, and behavioural patterns derived from each customer's approved activity.

Based on the combined risk assessment, a payment can be approved, held for review, or blocked.

The project is developed for learning, experimentation, and demonstration. It is not intended for real banking or production payment processing.

## Key Features

- **Digital Payment Workflow:** Demonstrates customer login, accounts, payees, payment processing, and transaction records.
- **Rule-Based Fraud Detection:** Evaluates signals such as new devices, new recipients, unusual amounts, activity at unusual hours, and transaction velocity.
- **Machine Learning:** Uses a Random Forest demonstration model to generate a fraud-risk score.
- **Behavioural Anomaly Analysis:** Compares payment activity against the individual customer's historical behaviour.
- **Customer-Specific Profiles:** Maintains separate behavioural histories for individual customers.
- **Location-Based Anomaly Signals:** Uses available location and timestamp information to identify potentially implausible travel between transactions.
- **Fraud Case Management:** Creates cases for suspicious activity and supports analyst review.
- **Risk-Based Payment Decisions:** Supports approval, hold, and block outcomes based on the existing decision workflow.
- **Payment Consistency:** Uses a transactional outbox and internal service communication to support reliable fraud evaluation.
- **Microservices Architecture:** Separates payment processing, fraud analysis, API routing, and machine-learning functionality.
- **Local Development Environment:** Uses Docker Compose to run the application services and supporting infrastructure.

## System Architecture

The application consists of the following major components:

| Component | Responsibility |
|---|---|
| NexaPay | Customer-facing payment application |
| FraudShield Control Center | Fraud monitoring and analyst review interface |
| API Gateway | Routes requests to backend services |
| Payment Service | Manages login, accounts, payees, payments, and ledger operations |
| Fraud Service | Applies fraud rules, combines risk signals, and manages cases |
| ML Fraud Service | Generates machine-learning scores and analyses customer behaviour |
| Eureka Server | Provides service discovery |
| PostgreSQL | Stores payment and fraud-related application data |
| SQLite | Persists customer-specific behavioural history |
| Mailpit | Provides a local email testing environment |

### Payment Processing Flow

1. A customer signs in and initiates a payment.
2. The Payment Service records the payment and an outbox event.
3. The event is forwarded to the FraudShield service through internal HTTP communication.
4. FraudShield evaluates the transaction using fraud rules and the machine-learning service.
5. The combined risk assessment determines whether the payment is approved, held, or blocked.
6. Approved payments continue through the payment workflow. Held payments remain subject to review, while blocked payments follow the configured compensation workflow.
7. Suspicious transactions can be reviewed through the FraudShield Control Center.

## Fraud Detection Approach

### 1. Rule-Based Analysis

The rules engine evaluates transaction attributes and identifies explainable risk signals, including:

- Unfamiliar devices or recipients
- Unusual transaction amounts
- Activity during unusual hours
- High transaction frequency
- Suspicious location changes

### 2. Machine Learning

A Random Forest model provides a demonstration fraud-risk score using synthetic training data.

The model is used alongside the rules engine rather than as the sole decision-maker.

### 3. Behavioural Anomaly Analysis

The system maintains customer-specific behavioural history using successful logins and approved payments.

It compares new activity against that customer's history to identify deviations in amount, timing, device, recipient, transaction frequency, and location.

Held or blocked payment attempts are not automatically learned as normal behaviour.

### 4. Risk Assessment and Review

The rules engine, model score, and behavioural signals contribute to the final risk assessment. Unusual activity is treated as a reason for additional verification or review, not automatic proof of fraud.

## Technology Stack

**Backend**
- Java
- Spring Boot
- Spring Cloud
- Spring Data JPA
- Maven
- REST APIs

**Machine Learning**
- Python
- scikit-learn
- Random Forest
- Behavioural anomaly analysis

**Frontend**
- React
- TypeScript
- Vite

**Data Storage**
- PostgreSQL
- SQLite

**Infrastructure and Testing**
- Docker
- Docker Compose
- Eureka Service Discovery
- Transactional Outbox Pattern
- Postman
- Mailpit

## Running the Project Locally

### Prerequisites

- Docker Desktop with Docker Compose
- Git
- At least 8 GB RAM recommended
- Postman for API testing

Docker Compose builds and runs the application services, so a separate local Java or Python installation is not normally required for the containerized setup.

### Step 1: Clone the Repository

```bash
git clone https://github.com/YOUR-USERNAME/digital-payment-fraud-detection.git
cd digital-payment-fraud-detection
```

Replace `YOUR-USERNAME` with your GitHub username.

### Step 2: Configure Environment Variables

Review `.env.example` and the environment configuration in `docker-compose.yml`.

Create a local `.env` file if required by your configuration. Use appropriate local demo values and never commit real passwords, API keys, or production secrets.

### Step 3: Start the Application

```bash
docker compose up -d --build
```

Check the service status:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f payment-service fraud-service ml-fraud-service
```

### Step 4: Access the Application

| Service | Local URL |
|---|---|
| NexaPay Customer Application | http://localhost:5173 |
| FraudShield Control Center | http://localhost:5174 |
| API Gateway | http://localhost:8060 |
| Eureka Dashboard | http://localhost:8761 |
| ML Service Health Check | http://localhost:8091/health |
| Mailpit Test Inbox | http://localhost:8025 |

Follow the project's setup documentation for demo accounts, login instructions, and analyst access.

### Step 5: Test Fraud Detection

Import the Postman collection from:

`docs/NexaPay_ML_Behaviour_Tests.postman_collection.json`

Refer to `docs/ML_BEHAVIOUR_ENGINE.md` for behavioural scoring scenarios and testing instructions.

### Stopping the Application

```bash
docker compose down
```

Named volumes are preserved by this command. Avoid `docker compose down -v` unless you intentionally want to delete the persistent demo database and behavioural history.

## Project Documentation

Additional documentation is available in the `docs/` directory.

| Document | Purpose |
|---|---|
| `ARCHITECTURE.md` | System components and payment flow |
| `ML_BEHAVIOUR_ENGINE.md` | Behavioural analysis and scoring scenarios |
| `DEMO_SCRIPT.md` | Application demonstration flow |
| `VIVA_QA.md` | Project questions and explanations |
| `VALIDATION_REPORT.md` | Validation notes and test results |
| `PROJECT_PLAN.md` | Suggested future enhancements |

## Current Limitations

- The Random Forest model uses generated synthetic teaching data rather than a representative, labelled banking dataset.
- The risk scores and thresholds are for demonstration purposes and have not been validated for real financial decisions.
- Location information can be inaccurate because of device permissions, GPS, IP addresses, or VPN usage.
- An unusual payment does not necessarily indicate fraud.
- Production deployment would require security hardening, authentication and authorization reviews, representative data, model evaluation, privacy controls, audit logging, and operational monitoring.

## Future Enhancements

- Train and evaluate the model using an appropriate, consented, labelled dataset.
- Add model evaluation metrics, calibration, and drift monitoring.
- Improve risk explanations and analyst feedback workflows.
- Expand automated testing for customer isolation and behavioural edge cases.
- Strengthen access controls, privacy protections, and audit logging.
- Add model versioning and traceable explanations to fraud decisions.

## Purpose of This Project

This project demonstrates the design and implementation of a full-stack payment application with a separate fraud monitoring system. It explores the integration of Java-based backend services, machine learning, behavioural anomaly analysis, persistent storage, and containerized deployment.

**Note:** NexaPay and FraudShield are demonstration systems. This project must not be used to make real banking or payment decisions.

## License

No license has been specified yet. If you intend to allow others to reuse, modify, or distribute this project, consider adding an appropriate open-source license.
