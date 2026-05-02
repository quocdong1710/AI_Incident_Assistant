# AI Incident Assistant

Backend Java Spring Boot triển khai theo tài liệu `docs/SRS_AI_Incident_Assistant_v1.0.md`.

## Công nghệ

- Java 21
- Spring Boot 3.3
- Spring Web, Validation, Actuator
- Spring Data JPA
- H2 cho môi trường dev, PostgreSQL runtime-ready
- Scheduler cho SLA monitoring

## Module đã triển khai

- F1 Intake & Detection: nhận webhook Telegram/simulate, log metadata, idempotency theo message.
- F2 AI Triage & Extraction: service phân loại rule-based, song ngữ cơ bản, trích xuất title/component/severity/impact/source language. Có thể thay bằng LLM provider sau.
- F3 Incident Matching: cosine similarity trên token vector để phát hiện trùng lặp incident đang mở.
- F4 Jira Sync: Jira adapter mock sinh issue key/link, hỗ trợ create/comment/update priority/assign và health check.
- F5 SLA Monitoring: tính deadline theo severity, cảnh báo 80%, ghi SLA breach log.
- F6 Resource Coordination: gợi ý tối đa 3 nhân sự theo skill, workload, online status, fatigue; assign sau xác nhận.

## Chạy ứng dụng

```bash
mvn spring-boot:run
```

Ứng dụng chạy tại `http://localhost:8080`.

## Endpoint chính

### Health

- `GET /health`
- `GET /actuator/health`

### Webhook / mô phỏng tin nhắn

- `POST /api/webhooks/telegram`
- `POST /api/webhooks/simulate`

Ví dụ payload cho simulate:

```json
{
  "messageId": "msg-001",
  "platform": "telegram",
  "groupId": "support-acme",
  "groupName": "Support ACME",
  "senderId": "user-123",
  "senderName": "Nguyen Van A",
  "text": "Anh ơi app bên em login không được, nhiều user bị từ sáng đến giờ @support_nguyen",
  "mentionedUserIds": ["support_nguyen"]
}
```

### Incident và command APIs

- `GET /api/incidents`
- `GET /api/incidents/{jiraIssueKey}`
- `POST /api/commands/severity?issueKey=PROJ-456&severity=High`
- `GET /api/commands/suggest/{issueKey}`
- `POST /api/commands/assign?issueKey=PROJ-456&username=le_thi_b`
- `GET /api/commands/report/sla?period=week`

## Cấu hình

Các cấu hình chính nằm trong `src/main/resources/application.yml`:

- `aia.confidence-threshold`
- `aia.similarity-threshold`
- `aia.telegram.bot-secret-token`
- `aia.jira.*`
- `aia.sla-minutes.*`
- `aia.working-hours.*`

Các secret nên truyền qua biến môi trường, không hard-code trong source code.

## Ghi chú

Phiên bản này là Spring Boot MVP để hiện thực hóa luồng nghiệp vụ trong SRS. Những tích hợp production như Telegram webhook thực tế, Jira REST thật, LLM structured output, Redis/RabbitMQ và PostgreSQL deployment có thể được bổ sung trên nền kiến trúc hiện tại.
