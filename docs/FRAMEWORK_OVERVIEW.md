# Java Test Automation Framework — Architecture Overview

> A guide for new team members to understand how this framework is structured and how to work with it.

---

## 1. What Is This Framework?

This is a **Java-based test automation framework** that supports two types of testing under one Maven project:

| Layer | Technology | What It Tests |
|-------|-----------|---------------|
| **API Testing** | REST Assured + TestNG | Backend REST APIs — authentication, data flows, business logic |
| **UI Testing** | Selenium + Cucumber | Browser end-to-end user journeys |

Both layers share the same configuration, utilities, and reporting infrastructure.

---

## 2. Prerequisites

| Tool | Version | Why |
|------|---------|-----|
| Java JDK | 11 | Language version |
| Maven | 3.6+ | Build and dependency management |
| Chrome / ChromeDriver | Latest | UI tests |
| Git | Any | Source control |
| IntelliJ IDEA (recommended) | Any | IDE |

---

## 3. Project Structure

```
project-root/
│
├── pom.xml                          ← Maven build file — all dependencies declared here
│
├── test-suites/                     ← TestNG XML suite files (one per scenario/flow)
│   ├── testng_module_a_auth.xml
│   ├── testng_module_b_flow.xml
│   └── ... (one XML per test flow)
│
├── src/
│   ├── main/java/com/<company>/<project>/
│   │   ├── base/                    ← BaseTest.java — parent class for all test classes
│   │   ├── builders/                ← RequestBuilder.java — fluent HTTP request builder
│   │   ├── config/                  ← ConfigManager.java + ConfigLoader.java
│   │   ├── endpoints/               ← APIEndpoints.java — URL path constants
│   │   ├── payloads/                ← Shared/legacy payload builders
│   │   ├── services/                ← Service layer helpers
│   │   └── utils/                   ← RequestContext, LoggerUtil, TokenManager, etc.
│   │
│   ├── main/resources/
│   │   └── log4j2.xml               ← Logging configuration
│   │
│   ├── test/java/
│   │   │
│   │   ├── api/                     ← Reusable API client packages, one per domain module
│   │   │   ├── <moduleA>/           ← ModuleAClient.java, ModuleAEndpoints.java, ModuleAPayloads.java
│   │   │   ├── <moduleB>/
│   │   │   └── ...
│   │   │
│   │   ├── com/<company>/<project>/
│   │   │   ├── base/                ← BaseTest.java (same class, test-scoped view)
│   │   │   └── tests/               ← All @Test classes, organized by domain
│   │   │       ├── auth/
│   │   │       ├── orders/
│   │   │       ├── payments/
│   │   │       └── ...              ← One package per business domain
│   │   │
│   │   ├── pageObjects/             ← Selenium Page Object classes (UI layer)
│   │   │   ├── Locators.java        ← Element locators
│   │   │   └── LocatorsPage.java    ← Page interaction methods
│   │   │
│   │   ├── stepDefinition/          ← Cucumber step definitions (UI layer)
│   │   │   ├── HomePageSteps.java
│   │   │   ├── LoginSteps.java
│   │   │   └── ... (one file per feature area)
│   │   │
│   │   ├── testRunner/              ← Cucumber runner classes
│   │   │   └── RunnerTest.java
│   │   │
│   │   ├── hooks/                   ← Cucumber @Before / @After scenario hooks
│   │   │
│   │   └── utilities/               ← Shared utilities
│   │       ├── BaseClass.java
│   │       ├── DriverFactory.java
│   │       ├── ExtentManager.java
│   │       ├── TestNGExtentReportListener.java
│   │       ├── ExcelUtils.java
│   │       └── ...
│   │
│   └── test/resources/
│       ├── config.properties        ← ⭐ Central configuration file
│       ├── Feature/                 ← Cucumber .feature files (Gherkin BDD scenarios)
│       │   ├── TC_01_Login.feature
│       │   ├── TC_02_EndToEndFlow.feature
│       │   └── ...
│       └── TestData/
│           └── TestData.xlsx        ← Excel-driven test data
```

---

## 4. Central Configuration File

`src/test/resources/config.properties` — **the only file you need to edit** to switch environments or credentials.

```properties
# ── Base URLs ────────────────────────────────────────────────────────────────
base.url=https://staging-api.example.com
secondary.base.url=https://staging-other-service.example.com

# ── Test User Credentials ────────────────────────────────────────────────────
user.mobile=<test-user-mobile>
user.password=<test-user-password>
static.otp=<otp-value>

# ── Admin Credentials ────────────────────────────────────────────────────────
admin.identifier=<admin-email-or-mobile>
admin.password=<admin-password>

# ── Excel Test Data ──────────────────────────────────────────────────────────
excel.filePath=src/test/resources/TestData/TestData.xlsx
excel.sheetName=Sheet1
```

Add a new key here → add the matching method in `ConfigManager.java` → use it anywhere via `ConfigLoader.getConfig().yourMethod()`.

---

## 5. Core Building Blocks

### 5.1 BaseTest — Parent of Every Test Class

Every TestNG test class **extends `BaseTest`**. It provides three lifecycle hooks:

| Method | Annotation | Responsibility |
|--------|-----------|----------------|
| `beforeSuiteSetup()` | `@BeforeSuite` | Resets `RequestContext` — clears all shared state before a suite starts |
| `setUp()` | `@BeforeClass` | Sets `RestAssured.baseURI` from `config.properties` |
| `tearDown()` | `@AfterClass` | Prints a performance summary to the console |

```java
// Your test class only needs to extend BaseTest
public class SomeModuleTest extends BaseTest {

    // Override setUp() only when you need a non-default base URL
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        RestAssured.baseURI = ConfigLoader.getConfig().secondaryBaseUrl();
    }

    @Test(priority = 1)
    public void login() { ... }
}
```

---

### 5.2 RequestBuilder — Central HTTP Call Builder

All API calls are made through `RequestBuilder`. It follows the **fluent builder pattern** — chain methods and finish with the HTTP verb:

```java
// POST with a JSON body
Response response = new RequestBuilder()
    .setEndpoint("/api/v1/auth/login")
    .setRequestBody(payload)
    .post();

// POST with Authorization header
Response response = new RequestBuilder()
    .setEndpoint("/api/v1/orders/create")
    .addHeader("Authorization", "Bearer " + token)
    .setRequestBody(payloadMap)
    .post();

// GET with a path variable
Response response = new RequestBuilder()
    .setEndpoint("/api/v1/users/" + userId + "/profile")
    .addHeader("Authorization", "Bearer " + token)
    .get();
```

**Available methods:**
`.setEndpoint()` · `.setRequestBody()` · `.addHeader()` · `.addQueryParam()` · `.post()` · `.get()` · `.put()` · `.delete()`

No test class should ever build a raw `RestAssured` request — always go through `RequestBuilder`.

---

### 5.3 The Three-File Module Pattern

Every domain module in `src/test/java/api/<module>/` always has exactly **three files**:

```
api/
└── <moduleName>/
    ├── <ModuleName>Endpoints.java    ← URL path constants only
    ├── <ModuleName>Payloads.java     ← Request body builders only
    └── <ModuleName>Client.java       ← HTTP method wrappers only
```

#### `*Endpoints.java` — URL constants, nothing else
```java
public class OrderEndpoints {
    public static final String CREATE_ORDER = "/api/v1/orders/create";
    public static final String GET_ORDER    = "/api/v1/orders/{id}";
    public static final String CANCEL_ORDER = "/api/v1/orders/cancel";
}
```

#### `*Payloads.java` — Request body factory methods, nothing else
```java
public class OrderPayloads {
    public static JSONObject buildCreateOrderPayload(String itemId, String addressId) {
        JSONObject body = new JSONObject();
        body.put("item_id",    itemId);
        body.put("address_id", addressId);
        return body;
    }
}
```

#### `*Client.java` — HTTP wrappers that use Endpoints + Payloads
```java
public class OrderClient {
    public Response createOrder(JSONObject payload, String userToken) {
        return new RequestBuilder()
            .setEndpoint(OrderEndpoints.CREATE_ORDER)
            .addHeader("Authorization", "Bearer " + userToken)
            .setRequestBody(payload.toString())
            .post();
    }

    public Response getOrder(String orderId, String userToken) {
        return new RequestBuilder()
            .setEndpoint(OrderEndpoints.GET_ORDER.replace("{id}", orderId))
            .addHeader("Authorization", "Bearer " + userToken)
            .get();
    }
}
```

**Rule:** Test classes call `Client` methods only. They never use `RequestBuilder` directly.

---

### 5.4 RequestContext — Shared State Across Test Steps

A single test flow often spans multiple `@Test` methods (login → create resource → verify → cleanup). `RequestContext` is a **static state holder** that passes data between steps without method parameters:

```java
// In step 1 — store
RequestContext.setUserToken(token);
RequestContext.setCurrentResourceId(resourceId);

// In step 2 — retrieve
String token      = RequestContext.getUserToken();
String resourceId = RequestContext.getCurrentResourceId();
```

`RequestContext.clearFlowState()` is called automatically at `@BeforeSuite` so each XML suite starts with a clean slate.

---

### 5.5 ConfigLoader + ConfigManager — Type-Safe Configuration

Instead of raw `Properties.load()` calls, the framework uses the **Owner** library:

- `ConfigManager.java` — a Java interface with one method per `config.properties` key
- `ConfigLoader.java` — a singleton factory that returns the `ConfigManager` instance

```java
// Anywhere in your code
String baseUrl   = ConfigLoader.getConfig().baseUrl();
String userPhone = ConfigLoader.getConfig().userMobile();
String adminId   = ConfigLoader.getConfig().adminIdentifier();
```

To add a new config key:
1. Add `new.key=value` to `config.properties`
2. Add `@Key("new.key") String newKey();` to `ConfigManager.java`
3. Call `ConfigLoader.getConfig().newKey()` wherever needed

---

## 6. Writing a New Test Class

```java
package com.<company>.<project>.tests.<module>;

import api.<module>.<Module>Client;
import api.<module>.<Module>Payloads;
import com.<company>.<project>.base.BaseTest;
import com.<company>.<project>.config.ConfigLoader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class <Module>Test extends BaseTest {

    // Shared state between @Test methods in this class
    private static String authToken;
    private static String createdResourceId;

    private final <Module>Client client = new <Module>Client();

    // Override only when this module uses a non-default base URL
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        RestAssured.baseURI = ConfigLoader.getConfig().baseUrl();
    }

    @Test(priority = 1, description = "MODULE_01: Login and capture token")
    public void MODULE_01_Login() {
        Response response = client.login(<Module>Payloads.buildLoginPayload());

        Assert.assertEquals(response.getStatusCode(), 200, "Login should return 200");
        authToken = response.jsonPath().getString("data.access_token");
        Assert.assertNotNull(authToken, "Token must not be null");
    }

    @Test(priority = 2, dependsOnMethods = {"MODULE_01_Login"},
          description = "MODULE_02: Create a resource")
    public void MODULE_02_CreateResource() {
        Response response = client.createResource(
            <Module>Payloads.buildCreatePayload(), authToken
        );
        Assert.assertEquals(response.getStatusCode(), 201);
        createdResourceId = response.jsonPath().getString("data.id");
    }

    @Test(priority = 3, dependsOnMethods = {"MODULE_02_CreateResource"},
          description = "MODULE_03: Verify the created resource")
    public void MODULE_03_VerifyResource() {
        Response response = client.getResource(createdResourceId, authToken);
        Assert.assertEquals(response.getStatusCode(), 200);
    }
}
```

**Rules to follow:**
- Always `extends BaseTest`
- Use `priority` to enforce execution order within a class
- Use `dependsOnMethods` so later steps are skipped if a prerequisite fails
- Store tokens and IDs in `private static` fields — never hard-code them
- Assert with `org.testng.Assert`

---

## 7. Running Tests

### From the command line
```bash
mvn test -DsuiteXmlFile=test-suites/testng_<module>_<flow>.xml
```

### From IntelliJ
Right-click any XML file in `test-suites/` → **Run**

### TestNG XML structure
```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.9.8.dtd">
<suite name="Module Auth Suite" verbose="1">
    <test name="Module Authentication Tests">
        <classes>
            <class name="com.example.project.tests.module.ModuleAuthTest"/>
            <class name="com.example.project.tests.module.ModuleAuthNegativeTest"/>
        </classes>
    </test>
</suite>
```

---

## 8. Reporting

### ExtentReports (HTML — auto-generated)
- Generated automatically after every run
- Open `test-output/ExtentReport.html` in a browser
- Shows pass/fail per test with timestamps

### Allure (interactive dashboard)
```bash
# Generate report from results
mvn allure:report

# Open in browser (starts a local server)
mvn allure:serve
```
- Result data is written to `allure-results/` during the run

### Console Summary
The framework prints a table to the terminal at the end of every suite:
```
============================================================
             TEST EXECUTION SUMMARY
============================================================
ModuleAuthTest.MODULE_01_Login           | PASS
ModuleAuthTest.MODULE_02_CreateResource  | PASS
ModuleAuthTest.MODULE_03_VerifyResource  | FAIL
============================================================
```

---

## 9. The Two Testing Approaches

### Approach A — API Tests (TestNG + REST Assured)
- **Location:** `src/test/java/.../tests/`
- **No browser needed**
- **Run via:** TestNG XML suite files
- **Best for:** backend validation, business logic, regression

### Approach B — UI End-to-End Tests (Cucumber + Selenium)
- **Feature files:** `src/test/resources/Feature/*.feature` — written in plain English (Gherkin)
- **Step definitions:** `src/test/java/stepDefinition/` — Java methods that map to Gherkin sentences
- **Page Objects:** `src/test/java/pageObjects/` — element locators and page actions
- **Runners:** `src/test/java/testRunner/` — run right-click → Run
- **Best for:** validating user journeys through the UI

---

## 10. Library Reference

| Library | What It Does |
|---------|-------------|
| **REST Assured** | Makes HTTP API calls and parses responses fluently |
| **TestNG** | Test runner — `@Test`, `@BeforeClass`, `priority`, `dependsOnMethods` |
| **Cucumber** | BDD framework — maps Gherkin `.feature` files to Java step methods |
| **Selenium** | Controls the browser for UI tests |
| **ExtentReports** | Generates rich HTML test reports |
| **Allure** | Generates interactive, filterable test dashboards |
| **Log4j 2** | Structured application logging |
| **Jackson / org.json** | JSON construction and parsing |
| **Apache POI** | Reads `.xlsx` Excel files for data-driven tests |
| **Owner** | Maps `.properties` file keys to type-safe Java interface methods |
| **JavaFaker** | Generates realistic random test data (names, phones, emails) |
| **AssertJ** | Fluent assertions — `assertThat(value).isEqualTo(expected)` |
| **Lombok** | Eliminates boilerplate (auto-generates getters, setters, constructors) |

---

## 11. Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Test class (positive) | `<Module>Test.java` | `OrderTest.java` |
| Test class (negative) | `<Module>NegativeTest.java` | `OrderNegativeTest.java` |
| Test method | `MODULE_NN_Description()` | `ORDER_01_CreateOrder()` |
| Client class | `<Module>Client.java` | `OrderClient.java` |
| Payload class | `<Module>Payloads.java` | `OrderPayloads.java` |
| Endpoints class | `<Module>Endpoints.java` | `OrderEndpoints.java` |
| Suite XML file | `testng_<module>_<flow>.xml` | `testng_order_cod_flow.xml` |
| Feature file | `TC_NN_Description.feature` | `TC_01_Login.feature` |

---

## 12. Adding a New Module — Step by Step

1. **Get the API contract** — endpoint URL, HTTP method, request body, expected response (from Postman or Swagger)
2. **Create `<Module>Endpoints.java`** in `src/test/java/api/<module>/` — add the URL as a constant
3. **Create `<Module>Payloads.java`** in the same package — add a static method that builds the request body
4. **Create `<Module>Client.java`** in the same package — add a method that calls `RequestBuilder` using the endpoint and payload
5. **Create `<Module>Test.java`** in `src/test/java/.../tests/<module>/` — extends `BaseTest`, uses the client
6. **Create `testng_<module>_<flow>.xml`** in `test-suites/` — lists your test class
7. **Run:** `mvn test -DsuiteXmlFile=test-suites/testng_<module>_<flow>.xml`

---

## 13. Architecture Diagram

```
config.properties
      │
      ▼
ConfigLoader ──► ConfigManager  (type-safe property access)
                      │
                      ▼
                 BaseTest  ◄──────── (all test classes extend this)
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
  TestNG @Test methods      Cucumber Steps
          │                       │
          ▼                       ▼
    <Module>Client          Page Objects
          │
          ▼
   RequestBuilder  ────────► HTTP Request (REST Assured)
          │                       │
          ▼                       ▼
   <Module>Endpoints       <Module>Payloads
                                  │
                            RequestContext  (shared state store)
```

Data flows from configuration → base setup → test logic → HTTP layer → assertion → reports.

