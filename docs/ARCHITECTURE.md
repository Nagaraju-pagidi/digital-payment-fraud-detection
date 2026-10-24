# NexaPay + FraudShield architecture

## Runtime services

| Service | Role | Local address |
|---|---|---|
| pay-app | Customer banking UI | http://localhost:5173 |
| control-center | Fraud analyst UI | http://localhost:5174 |
| api-gateway | Routes customer and analyst API calls | http://localhost:8060 |
| payment-service | Login, accounts, payees, payment saga and ledger | http://localhost:8081 |
| fraud-service | Rules, case management, analyst decisions | http://localhost:8082 |
| ml-fraud-service | Random Forest demo scorer + customer-isolated event history | http://localhost:8091 |
| eureka-server | Service registry | http://localhost:8761 |
| postgres | Payment and fraud persistence | localhost:5432 |
| mailpit | Local test email inbox | http://localhost:8025 |

Kafka, Kafka UI, Ollama, and the old AI insight service are not part of the runtime.

## Payment path

1. Customer signs in. Successful login time, location, and device are recorded in payment-service login history and submitted to the ML service's per-customer behaviour history.
2. Customer submits a payment. Payment-service records the payment and an outbox event in the same database transaction.
3. OutboxPublisher sends the event to FraudShield using internal HTTP and retries if FraudShield is temporarily unavailable.
4. Fraud-service builds a customer-specific context, evaluates deterministic rules, calls the ML service, combines the scores, and saves a case.
5. Fraud-service sends APPROVED, HOLD, or BLOCKED to payment-service over internal HTTP.
6. Payment-service credits the receiver only for APPROVED. HOLD leaves funds safely held for review; BLOCKED triggers a compensating refund.

## Personal behaviour model

The ML service receives customer ID, timestamp, amount, coordinates, city, device, and recipient. SQLite history is partitioned by customer ID and persisted in the `mlbehaviour` Docker volume. It stores successful logins and approved payments; held/blocked payment attempts are not learned as normal. The scorer compares payment amount and time to that customer's own history, checks velocity/new device/new recipient, and compares the current location with the most recent approved location event to detect implausible travel times.

The Random Forest uses generated synthetic teaching data. Personal-history signals are added at runtime; this is not a validated production banking model. Location estimates may be inaccurate, so location anomalies should trigger verification/review rather than be treated as proof.

## Risk decision

- Rules provide explainable signals (new device, new payee, unusual amount, night activity, velocity, distance, etc.).
- ML supplies a probabilistic demonstration score.
- Scores are combined before the payment decision. Amount alone should not automatically block a legitimate payment.
- Analysts can review held cases and make a final decision.

## Persistence and reset

PostgreSQL stores the banking/fraud data. SQLite behaviour history uses `mlbehaviour`. `docker compose down` keeps both named volumes. `docker compose down -v` deletes both, so use it only when you intentionally want a fresh demo.
