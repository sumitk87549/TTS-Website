# Words2Voice E2E Test Suite

**Stack**: TestNG + Cucumber 7 (BDD) + Selenium WebDriver 4 + REST Assured 5

---

## Directory Structure

```
e2e-tests/
├── pom.xml                                    # Maven project with all dependencies
├── testng.xml                                 # TestNG suite entry point
├── mvnw / .mvn/                               # Maven wrapper (no global Maven needed)
│
├── src/test/resources/features/
│   ├── register.feature                       # BDD scenarios for /signup page
│   ├── login.feature                          # BDD scenarios for /login page
│   └── auth_api.feature                       # REST API contract tests (no browser)
│
└── src/test/java/com/voisetu/e2e/
    ├── config/
    │   ├── TestConfig.java                    # URLs, timeouts, paths from System properties
    │   └── DriverFactory.java                 # Chrome / Firefox / Edge WebDriver factory
    ├── pages/                                 # Page Object Model
    │   ├── BasePage.java
    │   ├── SignupPage.java                    # /signup — all element interactions
    │   ├── LoginPage.java                     # /login — all element interactions
    │   └── StudioPage.java                    # /studio — redirect target + logout
    ├── steps/                                 # Cucumber Step Definitions
    │   ├── ScenarioContext.java               # PicoContainer shared state per scenario
    │   ├── Hooks.java                         # @Before / @After (driver lifecycle + screenshots)
    │   ├── RegisterSteps.java                 # Steps for register.feature
    │   ├── LoginSteps.java                    # Steps for login.feature
    │   └── AuthApiSteps.java                  # Steps for auth_api.feature (REST Assured)
    ├── utils/
    │   ├── WebDriverUtils.java                # Explicit waits, localStorage helpers
    │   └── ApiHelper.java                     # REST Assured: seed users, login, get JWT
    └── runner/
        ├── AuthUITestRunner.java              # TestNG runner → register + login UI tests
        └── AuthApiTestRunner.java             # TestNG runner → REST API tests
```

---

## Prerequisites

| Tool        | Version required | Notes                                       |
|-------------|------------------|---------------------------------------------|
| Java        | 17+              | Already installed (`/usr/bin/java`)         |
| Chrome      | Any modern       | WebDriverManager auto-downloads chromedriver|
| Spring Boot | Running on :8080 | `cd backend && ./mvnw spring-boot:run`      |
| Angular     | Running on :4200 | `cd frontend && ng serve`                   |

---

## How to Run

### Run all tests (UI + API)
```bash
cd e2e-tests
./mvnw test
```

### Run only smoke tests (fast)
```bash
./mvnw test -Dcucumber.filter.tags="@smoke"
```

### Run only UI register/login tests
```bash
./mvnw test -Dtest=AuthUITestRunner
```

### Run only API tests (no browser, just REST Assured)
```bash
./mvnw test -Dtest=AuthApiTestRunner
```

### Run in visible browser (non-headless)
```bash
./mvnw test -Dheadless=false
```

### Run against production
```bash
./mvnw test -PProd
```

### Run against custom URLs
```bash
./mvnw test \
  -Dbase.url=http://localhost:4200 \
  -Dapi.base.url=http://localhost:8080 \
  -Dbrowser=chrome \
  -Dheadless=true
```

---

## Available Cucumber Tags

| Tag              | Description                                              |
|------------------|----------------------------------------------------------|
| `@smoke`         | Critical path tests — fast regression set               |
| `@register` `@ui`| All signup page UI tests                                 |
| `@login` `@ui`   | All login page UI tests                                  |
| `@api`           | REST API tests — no browser required                     |
| `@validation`    | Client-side or server-side validation tests              |
| `@server-error`  | Tests that verify backend error responses                |
| `@navigation`    | Page navigation / redirect tests                        |
| `@token`         | JWT token structure and access control tests             |

---

## Test Reports

After running, reports are generated in `target/`:

| Report                                              | Format    |
|-----------------------------------------------------|-----------|
| `target/cucumber-reports/auth-ui-report.html`       | HTML      |
| `target/cucumber-reports/auth-api-report.html`      | HTML      |
| `target/cucumber-json/auth-ui.json`                 | JSON      |
| `target/cucumber-json/auth-api.json`                | JSON      |
| `target/surefire-reports/`                          | TestNG XML|

---

## Scenario Count

| Feature file       | Total Scenarios | Notes                                    |
|--------------------|-----------------|------------------------------------------|
| `register.feature` | 14              | Including Scenario Outline with 3 rows   |
| `login.feature`    | 10              | Including session persistence + logout   |
| `auth_api.feature` | 12              | Including Scenario Outline with 6+3 rows |
| **Total**          | **~36**         |                                          |

---

## Notes

- **Test isolation**: Each Cucumber scenario gets a fresh `WebDriver` + `ScenarioContext` instance.
- **User seeding**: API tests seed test users via `POST /api/auth/register` in `@Given` steps — idempotent, safe to re-run.
- **Screenshots on failure**: Automatically captured and embedded in the Cucumber HTML report.
- **No `Thread.sleep`**: All waits use `WebDriverWait` + `ExpectedConditions` for reliability.
