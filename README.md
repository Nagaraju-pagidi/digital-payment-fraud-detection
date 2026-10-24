# NexaPay — FraudShield behavioural payment monitoring

NexaPay is a final-year demonstration banking app with a separate FraudShield fraud-monitoring console. It combines deterministic fraud rules with a Random Forest demonstration scorer and a persistent, isolated behaviour profile for each customer.

## Start the project

Requirements: Docker Desktop with Docker Compose, at least 8 GB RAM recommended, and Postman for API testing.

1. Extract the project ZIP.
2. Open a terminal in the folder containing `docker-compose.yml`.
3. Start all services:

   ```bash
   docker compose up -d --build
   docker compose ps
   ```

4. Open the apps:

   - NexaPay banking app: http://localhost:5173
   - FraudShield Control Center: http://localhost:5174
   - ML health: http://localhost:8091/health
   - Eureka: http://localhost:8761
   - Mailpit test inbox for OTP emails: http://localhost:8025

5. Follow `docs/DEMO_CREDENTIALS.md` for demo users and analyst credentials.
6. For behavioural scoring tests, import `docs/NexaPay_ML_Behaviour_Tests.postman_collection.json` into Postman and follow `docs/ML_BEHAVIOUR_ENGINE.md`.

Useful commands:

```bash
docker compose logs -f payment-service fraud-service ml-fraud-service
docker compose down
docker compose up -d --build
```

**Data warning:** `docker compose down` preserves named volumes. Do not run `docker compose down -v` unless you intentionally want to delete the demo database and ML behaviour history.

## Behavioural ML features

- Successful login location/time/device and approved payment location/time/amount/device/recipient are stored per customer in a persistent SQLite volume.
- Payment scoring compares the current event to that customer's own history, including unusual hours, transaction velocity, amount deviation, new devices/recipients, and impossible travel between coordinates and timestamps.
- The ML service updates its normal baseline only from approved login/payment events. Held or blocked payments are not learned as normal.
- The payment saga calls FraudShield before final approval. A suspicious result can hold/block a payment according to the existing rule thresholds and analyst workflow.
- No Kafka broker, Kafka UI, Ollama, or AI insight container is required. Service-to-service payment decisions use the transactional outbox and internal HTTP calls.

## Important limitations

The Random Forest is trained on generated synthetic teaching data, not a labelled banking dataset. The customer-specific history signals make the demo adaptive, but this is not validated for real banking decisions. Location is only an estimate (GPS/IP/VPN/device permissions can affect it); impossible travel is a review signal, not proof of fraud. Do not deploy this demo as a real payment service without security review, representative labelled data, model evaluation/calibration, privacy controls, and operational monitoring.

## Project documentation

- `docs/ARCHITECTURE.md` — service architecture
- `docs/ML_BEHAVIOUR_ENGINE.md` — behaviour history, thresholds, and Postman scenarios
- `docs/DEMO_SCRIPT.md` — presentation flow
- `docs/DEMO_CREDENTIALS.md` — demo account credentials
- `docs/VIVA_QA.md` — viva preparation
