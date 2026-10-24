-- One PostgreSQL container, one database per microservice.
-- Each service owns its own database and never reads another service's tables.
CREATE DATABASE payments;       -- FraudPay  : payment-service
CREATE DATABASE fraud;          -- FraudShield: fraud-service
CREATE DATABASE notifications;  -- FraudShield: notification-service
CREATE DATABASE insights;       -- FraudShield: ai-insight-service (uses pgvector)
