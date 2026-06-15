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

## 🚀 Deployment

### Quick Deploy with Railway (Recommended for Hackathons)

1. Push this repo to GitHub
2. Go to [railway.app](https://railway.app) → Sign in with GitHub
3. **New Project** → **Deploy from GitHub repo** → Select this repo
4. Railway auto-detects Maven and deploys. Set env vars:
   - `PORT` = `8080`
   - `JAVA_VERSION` = `21`
5. Get your public URL instantly!

### Deploy with Docker

A production-ready `Dockerfile` is included:

```bash
# Build the image
docker build -t ecotrace .

# Run the container
docker run -d -p 8080:8080 --name ecotrace ecotrace
```

### Deploy to Any Cloud VM

```bash
# 1. Build the JAR
mvn clean package -DskipTests

# 2. Transfer to server
scp target/carbon-footprint-tracker-1.0.0.jar user@server:/opt/ecotrace/

# 3. Run on server (requires JDK 21)
java -jar carbon-footprint-tracker-1.0.0.jar
```

### ⚠️ Production Checklist

Before deploying to production, update `application.properties`:
- [ ] Generate a new JWT secret: `openssl rand -hex 32`
- [ ] Disable H2 Console: `spring.h2.console.enabled=false`
- [ ] Change the database password
- [ ] (Optional) Switch from H2 to PostgreSQL for persistent storage

---

## 📷 Application Screenshots

EcoTrace features a premium, modern UI with a responsive design that works beautifully on both desktop and mobile viewports. Key screens include:

- **Login & Registration** — Glassmorphic auth forms with animated banner
- **Dashboard** — Real-time carbon stats, category breakdown charts, daily trends
- **Log Activity** — Quick-action cards for transport, food, energy, and waste
- **Eco Challenges** — Join challenges, track progress, earn carbon offsets
- **Leaderboard** — Community rankings with friend comparisons
- **Insights** — AI-powered analytics with personalized recommendations
- **Account Settings** — Profile management, password updates, theme toggle

