# BiomeTrck — Interactive Carbon Footprint Tracker 🌿

**Live Deployment:** [http://biometrck-prod-env.eba-gdpgjaa9.us-east-1.elasticbeanstalk.com/](http://biometrck-prod-env.eba-gdpgjaa9.us-east-1.elasticbeanstalk.com/)

BiomeTrck is a premium, state-of-the-art interactive carbon footprint tracker. It helps users trace daily emissions across transport, food, energy, and waste, commit to eco-challenges to earn carbon offsets, and participate in community leaderboards to challenge friends.

---

## 🏗️ Project Architecture

BiomeTrck is built on a modern, robust, and scalable technology stack:
- **Backend**: Spring Boot 3.2.5 (Java 21)
- **Database**: 
  - *Local*: File-based H2 Database for local persistence and easy testing.
  - *Production*: **AWS RDS PostgreSQL** instance for high-availability production storage.
- **ORM & Data Access**: Spring Data JPA / Hibernate
- **Security**: Spring Security (JWT Stateless Authentication)
- **Credentials & Secrets**: **AWS Secrets Manager** for dynamic, runtime credential injection (no plaintext credentials committed).
- **Hosting & Infrastructure**: **AWS Elastic Beanstalk** (Corretto 21 running on Amazon Linux 2023).
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
│   │   │   ├── config/             # Spring Security, JWT, Rate Limiting, AWS Secrets Manager Initialization
│   │   │   ├── controller/         # REST Controllers & Global Exceptions
│   │   │   ├── dto/                # Request & Response Data Transfer Objects
│   │   │   ├── model/              # JPA Entities (User, Action, ActivityLog, Goal, etc.)
│   │   │   ├── repository/         # JPA Repositories
│   │   │   └── service/            # Business Logic Services
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-prod.properties  # Production properties matching AWS config
│   │       └── static/             # Static files (index.html, css/, js/, images/)
│   └── test/                       # JUnit & Mockito Service tests
├── deploy-aws.ps1                  # AWS Beanstalk/RDS/Secrets deployment script
├── pom.xml
└── README.md
```

---

## ⚙️ Configuration & Setup

### Database & Environment Profiles
* **Local Development (Default Profile)**: Uses a local file-based H2 database stored at `./data/carbondb.mv.db`. Auto-Server mode is enabled (`AUTO_SERVER=true`) so tests and application can run concurrently.
* **Production Deployment (`prod` Profile)**: Connects to the AWS RDS PostgreSQL database. The application dynamically initializes credentials at startup via `AwsSecretsInitializer.java`, pulling the secrets securely from AWS Secrets Manager.

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

## 🚀 Running the Application Locally

### Prerequisites
- JDK 21 or higher
- Apache Maven 3.9+

### Startup
Compile and run the Spring Boot server using Maven:
```bash
mvn spring-boot:run
```
The server will start on [http://localhost:8080](http://localhost:8080).

### Accessing the H2 Console (Local Only)
The local database console is accessible at [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
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

## 🚀 Cloud Deployment

The application features a production-ready setup for deployment to Amazon Web Services (AWS) using the automated `deploy-aws.ps1` PowerShell script.

### AWS Infrastructure Provisioned:
1. **AWS Secrets Manager**: A secret named `biometrck/prod/credentials` containing database host, credentials, and JWT signing secrets.
2. **AWS RDS (PostgreSQL)**: A `db.t3.micro` engine instances hosting production persistence data.
3. **AWS Elastic Beanstalk**: A single-instance application environment running on Corretto 21.
4. **AWS IAM Profile**: Secure policy mappings permitting the EC2 instances to retrieve credentials from AWS Secrets Manager at runtime.

### Running the Deployment Script:
To deploy the latest release to your AWS environment, run the packaging and deployment script:
```powershell
# 1. Package the JAR file
mvn clean package -DskipTests

# 2. Deploy to AWS
.\deploy-aws.ps1
```

---

## 📷 Application Screenshots

BiomeTrck features a premium, modern UI with a responsive design. The entry point of the application is a public marketing landing page (`/`) featuring a dynamic, animated CO₂ counter and product highlights. Users access the application via separate standalone login (`/login`) and registration (`/register`) pages, which redirect authenticated sessions to the secured insights dashboard (`/dashboard`).

### 🎬 Video Walkthroughs

| Desktop Walkthrough | Mobile Walkthrough |
|---|---|
| [▶ Watch Desktop Demo (1280×800)](docs/videos/biometrck_desktop_walkthrough.webm) | [▶ Watch Mobile Demo (iPhone SE)](docs/videos/biometrck_mobile_walkthrough.webm) |

### Desktop Views

#### Login & Registration
| Login | Register | Register (Filled) | Terms & Conditions |
|---|---|---|---|
| <img src="./docs/screenshots/flow_1_login.png?raw=true" alt="Login" width="220" /> | <img src="./docs/screenshots/flow_2_register.png?raw=true" alt="Register" width="220" /> | <img src="./docs/screenshots/flow_2_register_filled.png?raw=true" alt="Register Filled" width="220" /> | <img src="./docs/screenshots/flow_2_tc_modal.png?raw=true" alt="T&C Modal" width="220" /> |

#### Dashboard & Activity Logging
| Empty Dashboard | Log Activity | Activity Filled | Dashboard with Data |
|---|---|---|---|
| <img src="./docs/screenshots/flow_3_dashboard_empty.png?raw=true" alt="Empty Dashboard" width="220" /> | <img src="./docs/screenshots/flow_4_log_activity_modal.png?raw=true" alt="Log Activity" width="220" /> | <img src="./docs/screenshots/flow_4_log_activity_filled.png?raw=true" alt="Activity Filled" width="220" /> | <img src="./docs/screenshots/flow_5_dashboard_with_activity.png?raw=true" alt="Dashboard" width="220" /> |

#### History, Challenges & Goals
| Log History | Challenges | Challenges Committed | Carbon Budgets |
|---|---|---|---|
| <img src="./docs/screenshots/flow_6_log_history.png?raw=true" alt="History" width="220" /> | <img src="./docs/screenshots/flow_7_challenges.png?raw=true" alt="Challenges" width="220" /> | <img src="./docs/screenshots/flow_8_challenges_committed.png?raw=true" alt="Committed" width="220" /> | <img src="./docs/screenshots/flow_9_budgets.png?raw=true" alt="Budgets" width="220" /> |

#### Budgets, Community & Settings
| Budgets Set | Community Leaderboard | Profile Settings | Dark Mode |
|---|---|---|---|
| <img src="./docs/screenshots/flow_10_budgets_set.png?raw=true" alt="Budgets Set" width="220" /> | <img src="./docs/screenshots/flow_11_community.png?raw=true" alt="Community" width="220" /> | <img src="./docs/screenshots/flow_12_profile_settings.png?raw=true" alt="Settings" width="220" /> | <img src="./docs/screenshots/flow_13_dark_mode_dashboard.png?raw=true" alt="Dark Mode" width="220" /> |

### Mobile Views

| Login | Dashboard | Sidebar Open | Sidebar Closed |
|---|---|---|---|
| <img src="./docs/screenshots/flow_mobile_1_login.png?raw=true" alt="Mobile Login" width="160" /> | <img src="./docs/screenshots/flow_mobile_2_dashboard.png?raw=true" alt="Mobile Dashboard" width="160" /> | <img src="./docs/screenshots/flow_mobile_3_sidebar_open.png?raw=true" alt="Sidebar Open" width="160" /> | <img src="./docs/screenshots/flow_mobile_4_sidebar_closed.png?raw=true" alt="Sidebar Closed" width="160" /> |
