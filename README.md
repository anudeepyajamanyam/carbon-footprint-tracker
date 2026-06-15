# EcoTrace — Interactive Carbon Footprint Tracker 🌿

EcoTrace is a premium, state-of-the-art interactive carbon footprint tracker. It helps users trace daily emissions across transport, food, energy, and waste, commit to eco-challenges to earn carbon offsets, and participate in community leaderboards to challenge friends.

---

## 🏗️ Project Architecture

EcoTrace is built on a modern, robust, and lightweight technology stack:
- **Backend**: Spring Boot 3.2.5 (Java 21)
- **Database**: H2 Database (File-based storage for persistence across restarts)
- **ORM & Data Access**: Spring Data JPA / Hibernate
- **Security**: Spring Security (JWT Stateless Authentication)
- **Frontend**: Vanilla CSS, Semantic HTML5, and Modern Vanilla JavaScript (Single Page Application structure)
- **Caching**: Spring Cache (Simple In-Memory provider)
- **Rate Limiting**: Custom IP-based token-bucket filter (no external Redis/Memcached required)

---

## 📂 Project Structure

```
carbon-footprint-tracker/
├── src/
│   ├── main/
│   │   ├── java/com/carbon/tracker/
│   │   │   ├── config/             # Spring Security, JWT, Rate Limiting, Seeding
│   │   │   ├── controller/         # REST Controllers & Global Exceptions
│   │   │   ├── dto/                # Request & Response Data Transfer Objects
│   │   │   ├── model/              # JPA Entities (User, Action, ActivityLog, Goal, etc.)
│   │   │   ├── repository/         # JPA Repositories
│   │   │   └── service/            # Business Logic Services
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/             # Static files (index.html, css/, js/, images/)
│   └── test/                       # JUnit & Mockito Service tests
├── pom.xml
└── README.md
```

---

## ⚙️ Configuration & Setup

### Database
We utilize an H2 File-based database to persist user data, goals, and logged activities across application restarts. The database is stored in `./data/carbondb.mv.db`.
- **Auto-Server Mode Enabled**: Running Maven tests (`mvn test`) concurrently with the running server task is fully supported by configuring `AUTO_SERVER=true` in `application.properties`.
- **Database Seeding**: The available eco-challenges catalog is seeded programmatically via `DatabaseSeeder` on application startup if the catalog is empty. This prevents primary key conflicts while ensuring files remain lightweight and self-initializing.

### Caching Strategy
To ensure high performance and low response times, the insights dashboard is cached per user (`@Cacheable(value = "dashboard", key = "#user.username")`).
To prevent staleness, the cache is evicted (`@CacheEvict`) whenever:
- An activity log is created or deleted
- A goal is created, updated, or deleted
- A user profile is updated or deleted
- A user opts in, completes, or abandons a challenge

### Rate Limiting
To secure the application against brute-force and spam requests:
- **Auth endpoints** (`/api/auth/**`): Limited to **15 attempts per IP per minute**.
- **All other API endpoints** (`/api/**`): Limited to **120 requests per IP per minute**.

---

## 🚀 Running the Application

### Prerequisites
- JDK 21 or higher
- Apache Maven 3.9+

### Startup
Compile and run the Spring Boot server using Maven:
```bash
mvn spring-boot:run
```
The server will start on [http://localhost:8080](http://localhost:8080).

### Accessing the H2 Console
The in-memory/file console is accessible at [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
- **JDBC URL**: `jdbc:h2:file:./data/carbondb`
- **Username**: `sa`
- **Password**: `password`

---

## 🧪 Testing

We have implemented unit tests using JUnit 5 and Mockito to validate core services.
Run the test suite using:
```bash
mvn clean test
```
The tests verify:
- User registration, login authentication, and validation constraints.
- Real-time carbon footprint math calculations.
- Walking offsets guard constraints (only one walking offset per day is allowed).
- Security checks for log deletions.

---

## 📷 Visual Flow Walkthroughs

For a detailed visual guide of all user interface screens and steps (both Desktop and Mobile layouts), please refer to the comprehensive [Flow Documentation](file:///C:/Users/anude/.gemini/antigravity-ide/brain/6c2b7732-afa3-4429-a81d-cbdac244f2cd/flows_documentation.md) artifact, which includes sequential step-by-step screenshots and embeds for the following automated walkthrough video recordings:

- 🎬 [Desktop Video Walkthrough (1280x800)](file:///C:/Users/anude/.gemini/antigravity-ide/brain/6c2b7732-afa3-4429-a81d-cbdac244f2cd/ecotrace_desktop_walkthrough.webm)
- 🎬 [Mobile Video Walkthrough (iPhone SE)](file:///C:/Users/anude/.gemini/antigravity-ide/brain/6c2b7732-afa3-4429-a81d-cbdac244f2cd/ecotrace_mobile_walkthrough.webm)

