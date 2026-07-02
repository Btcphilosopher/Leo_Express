# Spring Boot Backend & REST API Documentation

This directory houses the complete design and source definitions for the **Leo Express Enterprise Backend Service**.

The service is engineered using:
* **Java 17+**
* **Spring Boot 3.x**
* **Spring Security** (stateless JWT authentication)
* **Spring Data JPA** (PostgreSQL object-relational mapping)

---

## 1. Spring Boot Backend Source Code

### `src/main/resources/application.yml`
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/leo_express_db
    username: leo_user
    password: leo_secure_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: 3cfa76e2714a87e2814b87e2311f9f928e32714a87c12f9b872e41c09a832e8f1a
  expiration: 86400000 # 24 Hours
```

### `src/main/java/com/leoexpress/backend/security/JwtTokenProvider.java`
```java
package com.leoexpress.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
```

### `src/main/java/com/leoexpress/backend/controller/AuthController.java`
```java
package com.leoexpress.backend.controller;

import com.leoexpress.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        // Simple mock authentication for Central European passengers
        if ("jan.novak@leoexpress.cz".equals(email)) {
            String token = tokenProvider.generateToken(email, "ROLE_PASSENGER");
            return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "fullName", "Jan Novák",
                "tier", "Gold Tier"
            ));
        }
        
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
```

### `src/main/java/com/leoexpress/backend/controller/BookingController.java`
```java
package com.leoexpress.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @GetMapping
    public ResponseEntity<?> getPassengerBookings(@RequestHeader("Authorization") String token) {
        // Returns a pre-loaded sample active Central Europe ticket list matching database schema
        return ResponseEntity.ok(List.of(
            Map.of(
                "id", "LE-482103-CZ",
                "fromStation", "PRG",
                "toStation", "OSV",
                "departureDate", "2026-07-03",
                "departureTime", "06:12",
                "arrivalTime", "09:32",
                "type", "TRAIN",
                "seatNumber", "24B",
                "carriage", "Car 3 (Premium)",
                "passengerName", "Jan Novák",
                "pricePaidCzk", 329.00,
                "status", "ACTIVE",
                "qrCode", "LEOEXPRESS|LE-482103-CZ|PRG|OSV|2026-07-03|24B"
            )
        ));
    }

    @PostMapping("/book")
    public ResponseEntity<?> createBooking(
        @RequestHeader("Authorization") String token,
        @RequestBody Map<String, Object> request
    ) {
        String bookingId = "LE-" + (100000 + new Random().nextInt(900000)) + "-CZ";
        Map<String, Object> result = new HashMap<>(request);
        result.put("id", bookingId);
        result.put("status", "ACTIVE");
        result.put("qrCode", "LEOEXPRESS|" + bookingId + "|" + request.get("fromStationId") + "|" + request.get("toStationId") + "|24B");
        
        return ResponseEntity.ok(result);
    }
}
```

---

## 2. REST API Documentation (Specifications)

### Authentication Endpoint
* **URL:** `/api/auth/login`
* **Method:** `POST`
* **Headers:** `Content-Type: application/json`
* **Request Body:**
```json
{
  "email": "jan.novak@leoexpress.cz",
  "password": "passenger_password"
}
```
* **Response Body (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYW4ubm92YWs...",
  "tokenType": "Bearer",
  "fullName": "Jan Novák",
  "tier": "Gold Tier"
}
```

### Search Connections (Journey Planner)
* **URL:** `/api/routes/search`
* **Method:** `GET`
* **Params:** `fromStation=PRG&toStation=OSV&date=2026-07-03&passengers=1`
* **Response Body (200 OK):**
```json
[
  {
    "id": 1,
    "fromStationId": "PRG",
    "toStationId": "OSV",
    "departureTime": "06:12",
    "arrivalTime": "09:32",
    "type": "TRAIN",
    "priceCzk": 329.0,
    "carrierCode": "LE",
    "stops": "Pardubice, Olomouc"
  }
]
```

### Book Ticket & Payment Checkout
* **URL:** `/api/bookings/book`
* **Method:** `POST`
* **Headers:** `Authorization: Bearer <JWT_TOKEN>`
* **Request Body:**
```json
{
  "fromStationId": "PRG",
  "toStationId": "OSV",
  "departureDate": "2026-07-03",
  "departureTime": "06:12",
  "arrivalTime": "09:32",
  "type": "TRAIN",
  "passengerName": "Jan Novák",
  "seatNumber": "24B",
  "carriageNumber": "Car 3 (Premium)",
  "pricePaidCzk": 329.00
}
```
* **Response Body (200 OK):**
```json
{
  "id": "LE-482103-CZ",
  "status": "ACTIVE",
  "qrCodeData": "LEOEXPRESS|LE-482103-CZ|PRG|OSV|2026-07-03|24B",
  "pointsEarned": 32
}
```

### Staff Route Management (Admin Portal)
* **URL:** `/api/admin/routes`
* **Method:** `POST`
* **Headers:** `Authorization: Bearer <JWT_TOKEN>` (Must contain Role: ROLE_ADMIN)
* **Request Body:**
```json
{
  "fromStationId": "PRG",
  "toStationId": "MUC",
  "departureTime": "13:00",
  "arrivalTime": "19:00",
  "type": "COACH",
  "priceCzk": 480.0,
  "stops": "Plzeň, Regensburg"
}
```
* **Response Body (201 Created):**
```json
{
  "id": 14,
  "fromStationId": "PRG",
  "toStationId": "MUC",
  "departureTime": "13:00",
  "arrivalTime": "19:00",
  "type": "COACH",
  "priceCzk": 480.0,
  "stops": "Plzeň, Regensburg"
}
```
