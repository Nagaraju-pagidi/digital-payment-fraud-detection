# syntax=docker/dockerfile:1.6
# One Dockerfile for every Java microservice.
# docker-compose passes MODULE (for example: fraudpay/payment-service).

# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY pom.xml .
COPY platform platform
COPY fraudpay fraudpay
COPY fraudshield fraudshield
# The Maven cache is shared between all service builds, so libraries download only once.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -pl ${MODULE} -am -DskipTests package \
 && cp ${MODULE}/target/*.jar /app.jar

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
