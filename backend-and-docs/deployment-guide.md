# Leo Express Full-Stack Deployment Guide

This document describes how to build, run, and scale the Leo Express transport platform across both mobile and server infrastructure.

---

## 1. Prerequisites

Before deployment, ensure you have the following installed on your target servers/machines:
* **Java Development Kit (JDK 17)**
* **Android Studio & SDK (API Level 34+)**
* **PostgreSQL (v14+)**
* **Docker & Docker Compose** (Recommended for local / production containerization)
* **Gradle (v8.x+)**

---

## 2. PostgreSQL Database Setup

1. Start your local PostgreSQL server:
   ```bash
   sudo service postgresql start
   ```

2. Create the target user and database:
   ```sql
   CREATE DATABASE leo_express_db;
   CREATE USER leo_user WITH PASSWORD 'leo_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE leo_express_db TO leo_user;
   ```

3. Import the database schema and sample Central European seeds:
   ```bash
   psql -h localhost -U leo_user -d leo_express_db -f /backend-and-docs/schema.sql
   ```

---

## 3. Spring Boot Backend Service Deployment

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Compile the package and package it as an executable JAR:
   ```bash
   ./gradlew bootJar
   ```

3. Run the application:
   ```bash
   java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
   ```
   The backend REST API server will start on port `8080` by default.

---

## 4. Android App Compilation & Packaging

1. Open the `/app` folder in **Android Studio**.

2. Sync the project Gradle settings to download Jetpack Compose and Room compiler dependencies.

3. To assemble a Debug APK for testing on the streaming emulator:
   ```bash
   gradle assembleDebug
   ```

4. The compiled APK will be outputted to:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Dockerized Deployment (Production Containers)

To run the Spring Boot API and PostgreSQL databases in highly resilient Docker containers, use the following `docker-compose.yml` configurations:

```yaml
version: '3.8'

services:
  postgres-db:
    image: postgres:15-alpine
    container_name: leo-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: leo_express_db
      POSTGRES_USER: leo_user
      POSTGRES_PASSWORD: leo_secure_password
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./backend-and-docs/schema.sql:/docker-entrypoint-initdb.d/schema.sql

  spring-api:
    image: openjdk:17-alpine
    container_name: leo-api
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-db:5432/leo_express_db
      SPRING_DATASOURCE_USERNAME: leo_user
      SPRING_DATASOURCE_PASSWORD: leo_secure_password
    depends_on:
      - postgres-db

volumes:
  pgdata:
```

### Run Containers
To start both database and backend instantly in background mode:
```bash
docker-compose up -d
```
To monitor application logs:
```bash
docker-compose logs -f
```
