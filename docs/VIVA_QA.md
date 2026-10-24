# Viva Q&A — NexaPay and FraudShield

**1. What is the project?**  
A demonstration banking application with a separate fraud-monitoring service that assesses payment risk before a payment is completed.

**2. Why separate payment and fraud services?**  
Payment-service owns accounts, balances, and the ledger. Fraud-service owns rules, risk cases, and analyst decisions. This separates business responsibilities.

**3. How are payment events delivered without Kafka?**  
Payment-service writes the payment and an outbox row in one database transaction. A scheduled dispatcher sends the event to FraudShield over internal HTTP and retries failures.

**4. What happens if FraudShield is slow or unavailable?**  
The payment remains processing temporarily; a saga timeout parks it on hold, leaving the funds safely held instead of crediting the receiver prematurely.

**5. Which machine-learning model is used?**  
A Random Forest classifier trained on generated synthetic teaching data. The output is a demonstration risk score, not a claim of real-world banking accuracy.

**6. How is customer behaviour personalised?**  
Events are keyed by customer ID. Successful login context and approved payment events build that customer's history; the scorer does not borrow another customer's history.

**7. What behaviour signals are used?**  
Personal payment amount deviation, transaction velocity, new device, new recipient, unusual hours, and distance travelled between location events over elapsed time.

**8. How is impossible travel detected?**  
Coordinates are compared with Haversine distance, and the distance is divided by elapsed time. Very large distances in implausibly short periods raise risk. It is only a signal because location can be wrong.

**9. Does a new recipient or large payment automatically mean fraud?**  
No. Signals are combined. A high payment to a familiar recipient from a familiar device and location can be legitimate and should be evaluated in context.

**10. What is the difference between a hold and a block?**  
A hold keeps funds pending review. A block stops the payment and triggers a refund/compensating action.

**11. When is history updated?**  
Successful logins and approved payments can be recorded as normal events. Held or blocked payments are not learned as normal. Confirmed fraud labels and a governed retraining pipeline are future extensions.

**12. Is the model production-ready?**  
No. It needs representative labelled data, proper train/validation/test separation, calibration, false-positive analysis, privacy/security review, and monitoring before real financial use.

**13. Why is location not proof of fraud?**  
GPS permissions, IP geolocation, travel, VPNs, and data quality can make location estimates inaccurate. Suspicious location patterns should trigger verification or review.

**14. How do you test behavioural features?**  
Use the provided Postman collection to seed approved events and test impossible travel, customer isolation, unusual hours, new device/recipient, and blocked-event exclusion.

**15. Which technologies are used?**  
Java 21/Spring Boot services, PostgreSQL, React/Vite frontends, Docker Compose, Python/FastAPI, scikit-learn Random Forest, and SQLite for the ML service's per-customer event history.
