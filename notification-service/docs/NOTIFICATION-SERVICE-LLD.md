# Notification Service — Low-Level Design Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | LLD                           |
| **Scope**   | notification-service          |
| **Version** | 1.0                           |
| **Date**    | 2025-04-06                    |
| **Status**  | Draft                         |
| **Author**  | OMS Engineering Team          |

---

## Table of Contents

1. [Package Structure](#1-package-structure)
2. [Key Classes](#2-key-classes)
3. [Database Schema](#3-database-schema)
4. [Configuration Properties](#4-configuration-properties)
5. [Error Codes](#5-error-codes)

---

## 1. Package Structure

```
com.oms.notification/
  consumer/  — OrderEventConsumer, PaymentEventConsumer
  sender/    — EmailSender (interface), SmtpEmailSender, ConsoleEmailSender
  template/  — EmailTemplateService (Thymeleaf)
  config/    — MailConfig, KafkaConsumerConfig
```

---

## 2. Key Classes

| Class                  | Type                       | Responsibility                                                 |
|------------------------|----------------------------|----------------------------------------------------------------|
| `OrderEventConsumer`   | Kafka                      | `@KafkaListener` on `orders.placed`, `orders.shipped`, `orders.cancelled` |
| `PaymentEventConsumer` | Kafka                      | `@KafkaListener` on `payment.confirmed`                        |
| `EmailSender`          | Interface                  | `send(to, subject, body)` — implemented by SMTP or Console variant |
| `ConsoleEmailSender`   | Service (`@Profile("local")`) | Logs email content to SLF4J — no SMTP needed              |
| `SmtpEmailSender`      | Service (`@Profile("prod")`) | Sends real email via `JavaMailSender`                       |
| `EmailTemplateService` | Service                    | Renders Thymeleaf templates to HTML strings                    |

---

## 3. Database Schema

No database. If notification history is needed in future, add:

```sql
-- Future enhancement only
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    sent_at         TIMESTAMPTZ,
    status          VARCHAR(50)
);
```

---

## 4. Configuration Properties

| Property                         | Default                  | Description                              |
|----------------------------------|--------------------------|------------------------------------------|
| `spring.kafka.consumer.group-id` | `oms-notification-group` | Kafka consumer group                     |
| `spring.kafka.bootstrap-servers` | `localhost:9092`         | Kafka broker                             |
| `spring.mail.host`               | `smtp.example.com`       | SMTP host (prod profile only)            |
| `notification.from-email`        | `noreply@oms.com`        | Sender email address                     |

---

## 5. Error Codes

| Code    | HTTP Status | Description                                         |
|---------|-------------|-----------------------------------------------------|
| NOT-001 | N/A         | Email dispatch failed — logged, event requeued      |
| NOT-002 | N/A         | Unknown event type — logged and skipped             |
