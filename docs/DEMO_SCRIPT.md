# NexaPay + FraudShield demo script

1. Start with `docker compose up -d --build` and confirm services with `docker compose ps`.
2. Open NexaPay at http://localhost:5173 and the analyst console at http://localhost:5174.
3. Sign in with one of the demo accounts in `DEMO_CREDENTIALS.md`. Mailpit at http://localhost:8025 shows demo OTP emails.
4. Make a normal payment using a familiar recipient and device. Explain that payment-service writes an outbox event, FraudShield checks rules and ML, and only an approval credits the receiver.
5. Open Control Center and show the fraud case, score, reasons, and decision.
6. Demonstrate personal behaviour in Postman using `NexaPay_ML_Behaviour_Tests.postman_collection.json`: seed a successful Hyderabad login, then score a Delhi payment five minutes later. Expected: impossible-travel signal, about 1,255 km in five minutes.
7. Score the same event under a different customer ID and show that the first customer's history is not reused.
8. Demonstrate unusual hours, new device/recipient, and large payments with familiar context. Explain that one high amount is not proof of fraud.
9. Open http://localhost:8091/health to show the model and stored history count.

## Explain the limitations clearly

The Random Forest is trained on synthetic demo data. It is not proven accurate for real banking. Location can be inaccurate because of GPS, IP geolocation, or VPNs. A production version needs labelled data, model evaluation, security hardening, privacy controls, and operational monitoring.
