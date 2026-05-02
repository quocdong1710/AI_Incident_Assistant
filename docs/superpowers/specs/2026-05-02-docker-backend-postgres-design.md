# Docker Compose for Spring Boot + PostgreSQL + CSV Seed

## Mục tiêu
Thiết lập hệ thống Docker Compose gồm 1 backend (Spring Boot) và 1 database (PostgreSQL), có cơ chế tự động seed dữ liệu nhân viên từ file CSV `team_members_9_nhan_vien_3_nhom.csv` chỉ trong lần chạy đầu tiên.

## Kiến trúc
- **PostgreSQL Service:** Database engine, lưu trữ volume dữ liệu trên host (`postgres_data`), nạp script SQL + dữ liệu CSV lúc init database.
- **Spring Boot Service:** Build từ source code Maven, kết nối vào PostgreSQL, expose port ra host (8080).
- **Network:** Bridge nội bộ trong Docker Compose.
- **Migration:** Flyway (hoặc auto ddl Spring) có thể chạy, phần seed data CSV được tách ra chạy ở bước init của PostgreSQL (`/docker-entrypoint-initdb.d/`).

## Thành phần

### 1. `docker-compose.yml`
Quản lý 2 dịch vụ:
- `db`: image `postgres:15-alpine` (hoặc 16).
  - Environment: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.
  - Volumes: bind script SQL (`init.sql`) và file CSV (`team_members.csv`) vào `/docker-entrypoint-initdb.d/`.
  - Healthcheck: ping `pg_isready` để đảm bảo db đã lên.
- `app`: image build từ `Dockerfile`.
  - Depends_on: `db` (condition: service_healthy).
  - Environment variables map qua file properties của Spring.

### 2. `Dockerfile`
Sử dụng Multi-stage build:
- Stage 1 (builder): Dùng `maven:3.9-eclipse-temurin-17` (hoặc bản JDK phù hợp project) để chạy `mvn clean package -DskipTests`.
- Stage 2 (runtime): Dùng `eclipse-temurin:17-jre-alpine` để chạy file `.jar` đã build.

### 3. Database Init Script
Tạo file `docker/postgres/init.sql`:
- Tạo bảng `team_member` (nếu Flyway chưa tạo).
- Dùng lệnh `COPY` của Postgres để đọc file CSV được mount trong container, đẩy thẳng vào bảng.
- **Cơ chế 1 lần**: Khi mount vào `/docker-entrypoint-initdb.d/`, Postgres Image tự động nhận diện nếu chưa có thư mục database data, nó mới chạy script. Nghĩa là việc seed chỉ diễn ra 1 lần duy nhất khi volume trống.

### 4. Code & Configuration
- Tùy biến `application.yml` (hoặc `application-docker.yml`) nhận tham số môi trường: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- Tạo Entity `TeamMember` và `TeamMemberRepository` (nếu chưa có).
