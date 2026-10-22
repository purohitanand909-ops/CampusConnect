# 🎓 CampusConnect — Campus Event Discovery & Registration System

CampusConnect is a campus event management and registration platform built with **Spring Boot 4.0.8**, **Java 21**, **Spring Security**, and **MySQL 8.4.9**. It features real-time capacity management, pessimistic concurrency locking to prevent overbooking, automated waitlist promotion, role-based authorization, and a responsive single-page web interface.

---

## 🚀 Key Features

* **Event Discovery & Filtering:** Search campus events by title/description and filter by category (Technology, Cultural, Career, Academic, Workshop).
* **Real-time Capacity & Spots Remaining:** Tracks confirmed seats vs. total capacity and reports `availableSpots` in real-time.
* **Overbooking Prevention:** Uses JPA pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on seat counts to safely handle concurrent registrations.
* **Automated FIFO Waitlist:** If an event is at maximum capacity, students are queued onto a numbered waitlist. When a confirmed attendee cancels, the earliest waitlisted student is automatically promoted to `CONFIRMED` and remaining positions are renumbered.
* **Re-registration Support:** Students who cancel and re-register have their record reactivated safely without violating unique key constraints.
* **Role-Based Security:** Real Spring Security authentication with BCrypt password hashing:
  * Public: Event browsing and search (`GET /api/v1/events`).
  * `ORGANIZER`: Create, update, and cancel events, view participant lists.
  * `STUDENT`: Register for events, cancel own registrations, view personal schedule.
* **Built-in Single Page Web UI:** Clean, responsive HTML/CSS/JavaScript dashboard served directly by Spring Boot at `http://localhost:8081`.

---

## 🛠️ Technology Stack

* **Java:** 21 (LTS)
* **Framework:** Spring Boot 4.0.8
* **Security:** Spring Security 7.x (BCrypt, Stateless HTTP Basic Authentication, CORS configured)
* **Data Access:** Spring Data JPA / Hibernate 7.x
* **Migrations:** Flyway (baseline at V2, V3 auth migration applied)
* **Database:** MySQL 8.4.9
* **Build Tool:** Maven Wrapper (`.\mvnw.cmd`)

---

## 👥 Seed Demo Accounts

All seed accounts are initialized with password: `password` (cost=10 BCrypt hash).

| Name | Email | Role | Permissions |
|---|---|---|---|
| **Dr. Priya Sharma** | `priya.sharma@campus.edu` | `ORGANIZER` | Create events, cancel events, view participants |
| **Anand Purohit** | `anand.purohit@student.edu` | `STUDENT` | Register for events, cancel registrations, view schedule |
| **Riya Mehta** | `riya.mehta@student.edu` | `STUDENT` | Register for events, cancel registrations, view schedule |
| **Karan Verma** | `karan.verma@student.edu` | `STUDENT` | Register for events, cancel registrations, view schedule |

---

## ⚙️ Prerequisites & Setup

### 1. Database Configuration
Ensure MySQL 8.4 is running locally with database `campusconnect_db`:
```sql
CREATE DATABASE IF NOT EXISTS campusconnect_db;
```

### 2. Local Credentials (Ignored in Git)
Create or check `src/main/resources/application-local.properties` (never committed to version control):
```properties
spring.datasource.password=YOUR_MYSQL_ROOT_PASSWORD
```

### 3. Server Port
By default, the server runs on port **8081** to avoid conflicts with background services on port 8080:
```properties
server.port=${SERVER_PORT:8081}
```

---

## 🏃 Launching the Application

### Run Automated Tests
```powershell
.\mvnw.cmd test
```
*Current test suite: 18 tests (0 failures, 0 errors) covering context bootstrap, `EventService`, `RegistrationService`, capacity limits, waitlist promotion, and `CampusUserDetailsService`.*

### Start Server
```powershell
.\mvnw.cmd spring-boot:run
```

Once started, open your browser and navigate to:
👉 **`http://localhost:8081`**

---

## 📡 REST API Reference

All responses follow a consistent `ApiResponse<T>` envelope:
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

### 1. Authentication
* **Get Current User Profile:**
  `GET /api/v1/auth/me` *(Requires HTTP Basic Auth)*
  ```bash
  curl -u anand.purohit@student.edu:password http://localhost:8081/api/v1/auth/me
  ```

### 2. Events
* **List / Search Events (Public):**
  `GET /api/v1/events?keyword={keyword}&category={category}`
  ```bash
  curl http://localhost:8081/api/v1/events?category=Technology
  ```

* **Get Event Details (Public):**
  `GET /api/v1/events/{id}`
  ```bash
  curl http://localhost:8081/api/v1/events/1
  ```

* **Create Event (ORGANIZER only):**
  `POST /api/v1/events`
  ```bash
  curl -u priya.sharma@campus.edu:password \
       -H "Content-Type: application/json" \
       -d '{"title":"Robotics Summit","description":"AI and robotics showcase","category":"Technology","location":"Hall A","startTime":"2026-11-20T10:00:00","registrationDeadline":"2026-11-18T10:00:00","capacity":50,"organizerId":1}' \
       http://localhost:8081/api/v1/events
  ```

* **Update Event (ORGANIZER only):**
  `PUT /api/v1/events/{id}`

* **Cancel Event (ORGANIZER only):**
  `DELETE /api/v1/events/{id}`

### 3. Registrations
* **Register for Event (STUDENT only):**
  `POST /api/v1/registrations`
  ```bash
  curl -u anand.purohit@student.edu:password \
       -H "Content-Type: application/json" \
       -d '{"studentId":2,"eventId":1}' \
       http://localhost:8081/api/v1/registrations
  ```
  *Response indicates whether registration is `CONFIRMED` or `WAITLISTED` (with waitlist position).*

* **Cancel Registration (STUDENT only):**
  `DELETE /api/v1/registrations/{id}`
  *Automatically promotes the next waitlisted student to `CONFIRMED` if a confirmed seat was freed.*

* **View Student Registrations (Authenticated):**
  `GET /api/v1/students/{studentId}/registrations`

* **View Event Participants & Waitlist (Authenticated):**
  `GET /api/v1/events/{eventId}/registrations`

---

## 🔒 Security Architecture

1. **Password Hashing:** Passwords are encrypted with `BCryptPasswordEncoder` (cost 10). Plaintext passwords are never stored.
2. **Stateless Session:** Configured with `SessionCreationPolicy.STATELESS` and HTTP Basic authentication for REST APIs.
3. **CORS:** Cross-Origin Resource Sharing is enabled for frontend development (`localhost:3000`, `localhost:8080`, `localhost:8081`).
4. **Role Enforcement:** Custom `CampusUserDetails` and `CampusUserDetailsService` map database roles to Spring Security granted authorities (`ROLE_STUDENT`, `ROLE_ORGANIZER`).

---

## 📂 Project Structure

```
campusconnect-backend/
├── src/
│   ├── main/
│   │   ├── java/com/campusconnect/
│   │   │   ├── controller/      # EventController, RegistrationController, AuthController
│   │   │   ├── dto/             # Request/Response DTOs and ApiResponse envelope
│   │   │   ├── entity/          # User, Event, Registration (JPA entities)
│   │   │   ├── exception/       # GlobalExceptionHandler, ResourceNotFound, BadRequest
│   │   │   ├── repository/      # Spring Data JPA repositories with custom queries & locks
│   │   │   ├── security/        # SecurityConfig, CampusUserDetails, CampusUserDetailsService
│   │   │   └── service/         # EventService, RegistrationService
│   │   └── resources/
│   │       ├── db/migration/    # Flyway SQL migrations (V1, V2, V3)
│   │       ├── static/          # Web UI (index.html, style.css, app.js)
│   │       ├── application.properties
│   │       └── application-local.properties (git-ignored)
│   └── test/
│       └── java/com/campusconnect/
│           ├── CampusconnectApplicationTests.java
│           ├── security/CampusUserDetailsServiceTest.java
│           └── service/
│               ├── EventServiceTest.java
│               └── RegistrationServiceTest.java
└── pom.xml
```
