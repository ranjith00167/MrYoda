# Playwright with TypeScript â€” Complete Training Guide

> **Target Audience:** Beginners, Manual Testers transitioning to Automation, Mid-level Engineers preparing for interviews.
> **Prerequisite:** Basic JavaScript knowledge.

---

## Table of Contents

1. Introduction
2. Setup & Installation
3. TypeScript Basics for Playwright
4. Writing Your First Test
5. Locators (Very Detailed)
6. Handling UI Elements
7. Wait Mechanisms
8. Multiple Pages & Tabs
9. Test Organization (POM, Fixtures, Hooks)
10. Assertions (Deep Dive)
11. Screenshots, Videos, Traces
12. Test Data Handling
13. API Testing with Playwright
14. Cross-Browser & Mobile Testing
15. Parallel Execution
16. Reporting
17. CI/CD Integration
18. Debugging & Troubleshooting
19. Best Practices
20. Interview Questions & Scenarios

---

# Chapter 1: Introduction to Playwright

## 1.1 What is Playwright?

Playwright is an **open-source end-to-end (E2E) testing framework** developed by **Microsoft**. It lets you write automated tests for web applications in **TypeScript, JavaScript, Python, C#, and Java**.

> Think of Playwright as a robot that controls a real browser â€” clicking buttons, filling forms, reading text â€” exactly like a human tester, but faster and repeatable.

**Key Definition:**
- **End-to-End Testing** = Testing the complete flow of an application from the user's perspective (UI â†’ API â†’ Database â†’ back to UI).

## 1.2 Why Playwright Over Selenium and Cypress?

| Feature | Playwright | Selenium | Cypress |
|---|---|---|---|
| Language Support | TS, JS, Python, Java, C# | Java, Python, C#, JS, Ruby | JS/TS only |
| Browser Support | Chromium, Firefox, WebKit | All browsers via drivers | Chromium-based only |
| Auto-waiting | âœ… Built-in | âŒ Manual waits | âœ… Built-in |
| Parallel Execution | âœ… Native | âš ï¸ Via TestNG/JUnit | âš ï¸ Paid feature |
| API Testing | âœ… Built-in | âŒ Needs separate lib | âš ï¸ Limited |
| Multi-tab / Multi-window | âœ… Native | âš ï¸ Complex | âŒ Not supported |
| iframes | âœ… Easy | âš ï¸ Complex | âš ï¸ Limited |
| Trace Viewer | âœ… Built-in | âŒ | âŒ |
| Speed | âš¡ Fast | ðŸ¢ Slow | âš¡ Fast |
| Shadow DOM | âœ… Native | âŒ Manual | âš ï¸ Limited |
| Setup Complexity | Low | High | Low |
| CI/CD Integration | Excellent | Good | Good |

**Bottom Line:**
- Selenium is old and slow â€” requires browser drivers.
- Cypress is limited to Chrome-family and single-tab.
- Playwright is modern, fast, multi-browser, and multi-tab.

## 1.3 Key Features of Playwright

1. **Auto-waiting** â€” No need for `Thread.sleep()` or `.wait()`.
2. **Multiple browsers** â€” Chrome, Firefox, Safari (WebKit) from one codebase.
3. **Network Interception** â€” Mock API responses during tests.
4. **Trace Viewer** â€” Replay any test failure with screenshots and network logs.
5. **Codegen** â€” Record actions and generate test code automatically.
6. **Parallel Tests** â€” Run tests simultaneously across multiple workers.
7. **API Testing** â€” Test REST APIs without a browser.
8. **Mobile Emulation** â€” Simulate iPhone, Android devices.
9. **Screenshot & Video** â€” Capture evidence on failure.
10. **TypeScript First** â€” Full type safety and IntelliSense.

## 1.4 Companies Using Playwright

- Microsoft, GitHub, VS Code (internally)
- Adobe, SAP, Shopify
- Thousands of enterprises in BFSI, Healthcare, E-commerce

## 1.5 Playwright Architecture

```
Your Test Code (TypeScript)
        â†“
Playwright Test Runner
        â†“
Browser Driver (via WebSocket)
        â†“
Real Browser (Chromium / Firefox / WebKit)
        â†“
Your Web Application
```

**How it works:**
1. Your test code calls Playwright APIs.
2. Playwright communicates with the browser using the **Chrome DevTools Protocol (CDP)** or browser-specific protocols.
3. The browser performs actions on your web app.
4. Playwright captures results and reports pass/fail.

> **Interview Tip:** Playwright uses the CDP (Chrome DevTools Protocol) for Chromium, and its own protocol for Firefox and WebKit. This makes it faster than Selenium's WebDriver protocol.

---

# Chapter 2: Setup & Installation

## 2.1 Prerequisites

- **Node.js** (v18 or higher recommended)
- **npm** (comes with Node.js)
- **VS Code** (recommended editor)

### Verify Installation

```typescript
// Run these in your terminal (not TypeScript, just shell commands shown here)
// node --version   â†’ v18.x.x or higher
// npm --version    â†’ 9.x.x or higher
```

## 2.2 Creating a New Playwright Project

```typescript
// Step 1: Create a project folder
// mkdir my-playwright-project
// cd my-playwright-project

// Step 2: Initialize Playwright with TypeScript
// npm init playwright@latest

// This wizard asks:
// âœ” Where to put your end-to-end tests? â€º tests
// âœ” Add a GitHub Actions workflow? â€º false (for now)
// âœ” Install Playwright browsers? â€º true
```

## 2.3 Folder Structure

After initialization, your project will look like this:

```
my-playwright-project/
â”œâ”€â”€ tests/                          # Your test files go here
â”‚   â””â”€â”€ example.spec.ts             # Sample test created by Playwright
â”œâ”€â”€ playwright.config.ts            # Main configuration file
â”œâ”€â”€ package.json                    # Node.js project manifest
â”œâ”€â”€ package-lock.json               # Locked dependency versions
â”œâ”€â”€ tsconfig.json                   # TypeScript configuration
â””â”€â”€ node_modules/                   # Installed packages (auto-generated)
```

**Additional folders you will create:**
```
â”œâ”€â”€ tests/
â”‚   â”œâ”€â”€ e2e/                        # End-to-end tests
â”‚   â”œâ”€â”€ api/                        # API tests
â”‚   â””â”€â”€ utils/                      # Helper utilities
â”œâ”€â”€ pages/                          # Page Object Model files
â”œâ”€â”€ fixtures/                       # Custom fixtures
â”œâ”€â”€ test-data/                      # Test data files (JSON, CSV)
â””â”€â”€ docs/                           # Documentation
```

## 2.4 playwright.config.ts â€” Line by Line

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  // Directory where test files are located
  testDir: './tests',

  // Run all tests in parallel (faster execution)
  fullyParallel: true,

  // Fail the build if you accidentally left test.only() in source code
  forbidOnly: !!process.env.CI,

  // Retry failed tests: 2 times in CI, 0 times locally
  retries: process.env.CI ? 2 : 0,

  // Number of parallel workers: 1 in CI, automatic (CPU-based) locally
  workers: process.env.CI ? 1 : undefined,

  // Built-in HTML reporter â€” generates beautiful test reports
  reporter: 'html',

  // Shared settings for ALL tests
  use: {
    // Base URL so you can write page.goto('/login') instead of full URL
    baseURL: 'http://localhost:3000',

    // Save trace on first retry (useful for debugging)
    trace: 'on-first-retry',

    // Take screenshot on failure
    screenshot: 'only-on-failure',

    // Record video on failure
    video: 'retain-on-failure',

    // Run in headless mode (no visible browser window)
    headless: true,

    // Timeout for each action (default: 30 seconds)
    actionTimeout: 30000,

    // Timeout for navigation (default: 30 seconds)
    navigationTimeout: 30000,
  },

  // Define which browsers to test on
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    // Mobile testing
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
  ],

  // Run a local dev server before tests start (optional)
  // webServer: {
  //   command: 'npm run start',
  //   url: 'http://localhost:3000',
  //   reuseExistingServer: !process.env.CI,
  // },
});
```

## 2.5 Running Your First Test

```typescript
// Run all tests
// npx playwright test

// Run a specific file
// npx playwright test tests/example.spec.ts

// Run in headed mode (see the browser)
// npx playwright test --headed

// Open HTML report after test
// npx playwright show-report

// Run with specific browser
// npx playwright test --project=chromium

// Run tests matching a name pattern
// npx playwright test --grep "login"

// Run in debug mode (step through each action)
// npx playwright test --debug
```

> **Common Mistake:** Forgetting to install browsers after installing Playwright.
> Fix: Run `npx playwright install`

---

# Chapter 3: TypeScript Basics for Playwright

## 3.1 TypeScript vs JavaScript

TypeScript is JavaScript with **type safety**. It catches bugs at compile time before your tests run.

```typescript
// JavaScript â€” No type safety
function add(a, b) {
  return a + b;
}
add(5, "10"); // Returns "510" â€” a bug! No warning.

// TypeScript â€” Type safe
function add(a: number, b: number): number {
  return a + b;
}
add(5, "10"); // âŒ TypeScript ERROR: Argument of type 'string' is not assignable to type 'number'
```

## 3.2 tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",          // JavaScript version to compile to
    "module": "commonjs",         // Module system
    "strict": true,               // Enable all strict type checks
    "esModuleInterop": true,      // Better import compatibility
    "skipLibCheck": true,         // Skip type checking of node_modules
    "outDir": "./dist",           // Where compiled JS goes
    "rootDir": "./",              // Source root
    "sourceMap": true             // Helpful for debugging
  },
  "include": ["tests/**/*.ts", "pages/**/*.ts", "fixtures/**/*.ts"],
  "exclude": ["node_modules"]
}
```

## 3.3 Types and Interfaces

```typescript
// Basic types
let testName: string = "Login Test";
let testId: number = 101;
let isHeadless: boolean = true;
let browserList: string[] = ["chromium", "firefox", "webkit"];

// Interface â€” defines the shape of an object
interface User {
  username: string;
  password: string;
  role: "admin" | "viewer" | "editor"; // Union type
  email?: string;                        // Optional property (?)
}

// Using the interface
const adminUser: User = {
  username: "admin@example.com",
  password: "Admin@123",
  role: "admin",
};

// Function with typed parameters and return type
function createLoginPayload(user: User): Record<string, string> {
  return {
    username: user.username,
    password: user.password,
  };
}

// Enum â€” named constants
enum Environment {
  DEV = "https://dev.example.com",
  STAGING = "https://staging.example.com",
  PROD = "https://prod.example.com",
}

const baseUrl: string = Environment.STAGING;
```

## 3.4 Async / Await â€” Deep Explanation

Web interactions (clicking, navigating, waiting) are **asynchronous** â€” they take time. Without async/await, tests would finish before the browser even loads the page.

```typescript
import { test, expect } from '@playwright/test';

// WRONG â€” synchronous approach (does not work)
// page.goto('https://example.com'); // Returns a Promise, not the actual result

// CORRECT â€” async/await approach
test('login test', async ({ page }) => {
  // 'async' means this function returns a Promise
  // 'await' means "wait for this to finish before moving on"
  
  await page.goto('https://example.com');         // Wait for navigation
  await page.getByLabel('Username').fill('admin'); // Wait for element, then type
  await page.getByLabel('Password').fill('secret');
  await page.getByRole('button', { name: 'Login' }).click(); // Wait and click
  
  // await ensures each step finishes before the next starts
  await expect(page).toHaveURL('/dashboard');
});
```

**Promise analogy:**
> Ordering coffee at a cafÃ©: You place an order (call the function), get a receipt (Promise), wait for your name to be called (await), then pick up your coffee (result).

```typescript
// Understanding Promise chains vs async/await
// Old way (Promise chains â€” hard to read)
page.goto('https://example.com')
  .then(() => page.getByLabel('Username').fill('admin'))
  .then(() => page.click('button[type=submit]'))
  .then(() => console.log('Done'))
  .catch((err) => console.error(err));

// New way (async/await â€” clean and readable)
async function loginUser() {
  await page.goto('https://example.com');
  await page.getByLabel('Username').fill('admin');
  await page.click('button[type=submit]');
  console.log('Done');
}
```

---

# Chapter 4: Writing Your First Test

## 4.1 The test() Function

```typescript
import { test, expect } from '@playwright/test';

// Basic test structure
test('test description goes here', async ({ page }) => {
  // 'page' is the browser page (tab) provided by Playwright
  // Everything inside is the test body
  
  await page.goto('https://playwright.dev');
  await expect(page).toHaveTitle(/Playwright/);
});
```

**Anatomy of a test:**
- `test()` â€” registers a test with the runner
- First argument â€” test name (string)
- Second argument â€” async function with `{ page }` parameter
- `{ page }` â€” destructured from Playwright's built-in fixture

## 4.2 Browser, Context, and Page

Playwright has a 3-level hierarchy:

```
Browser
  â””â”€â”€ BrowserContext (like an incognito window)
        â””â”€â”€ Page (a browser tab)
```

```typescript
import { chromium, Browser, BrowserContext, Page } from '@playwright/test';

// Manual approach (usually you let Playwright handle this via fixtures)
const browser: Browser = await chromium.launch({ headless: false });

// BrowserContext = isolated session (cookies, storage)
const context: BrowserContext = await browser.newContext({
  // Each context is completely isolated â€” like a fresh browser profile
  viewport: { width: 1280, height: 720 },
  locale: 'en-US',
  timezoneId: 'Asia/Kolkata',
});

// Page = a browser tab
const page: Page = await context.newPage();

await page.goto('https://example.com');
await page.screenshot({ path: 'screenshot.png' });

await context.close();
await browser.close();
```

**Why BrowserContext matters:**
- Tests in different contexts don't share cookies or storage.
- You can create multiple contexts to simulate multiple users simultaneously.
- This is how Playwright achieves true test isolation.

## 4.3 Page Lifecycle

```typescript
test('understanding page lifecycle', async ({ page }) => {
  // 1. Navigate to a URL
  await page.goto('https://example.com');

  // 2. Wait for page to fully load
  await page.waitForLoadState('networkidle');

  // 3. Interact with elements
  await page.getByRole('link', { name: 'Learn More' }).click();

  // 4. Verify navigation
  await expect(page).toHaveURL(/about/);

  // 5. Go back
  await page.goBack();

  // 6. Reload page
  await page.reload();

  // 7. Get page title
  const title: string = await page.title();
  console.log('Page title:', title);

  // 8. Get current URL
  const url: string = page.url();
  console.log('Current URL:', url);
});
```

## 4.4 Basic Assertions with expect()

```typescript
import { test, expect } from '@playwright/test';

test('basic assertions', async ({ page }) => {
  await page.goto('https://example.com');

  // Assert page title
  await expect(page).toHaveTitle('Example Domain');

  // Assert URL
  await expect(page).toHaveURL('https://example.com/');

  // Assert element is visible
  await expect(page.getByRole('heading', { name: 'Example Domain' })).toBeVisible();

  // Assert element text
  await expect(page.getByRole('heading')).toHaveText('Example Domain');

  // Assert element is enabled
  await expect(page.getByRole('button')).toBeEnabled();

  // Assert element count
  await expect(page.getByRole('link')).toHaveCount(1);

  // Assert input value
  await expect(page.getByRole('textbox')).toHaveValue('');
});
```

---

# Chapter 5: Locators â€” Finding Elements

## 5.1 What is a Locator?

A **locator** is how Playwright finds an element on the page. Playwright's locators are **lazy** â€” they don't actually search for the element until you perform an action on them.

## 5.2 getByRole() â€” The Recommended Approach

`getByRole()` finds elements by their **ARIA role** â€” the semantic meaning of an element.

```typescript
import { test, expect } from '@playwright/test';

test('getByRole examples', async ({ page }) => {
  await page.goto('https://example.com');

  // Find a button by its accessible name
  await page.getByRole('button', { name: 'Submit' }).click();

  // Find a link
  await page.getByRole('link', { name: 'Home' }).click();

  // Find a text input
  await page.getByRole('textbox', { name: 'Username' }).fill('admin');

  // Find a checkbox
  await page.getByRole('checkbox', { name: 'Remember me' }).check();

  // Find a heading
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

  // Find a combobox (dropdown)
  await page.getByRole('combobox', { name: 'Country' }).selectOption('India');

  // Exact match (default is case-insensitive, partial match)
  await page.getByRole('button', { name: 'Sign In', exact: true }).click();
});
```

**Common ARIA Roles:**

| Role | HTML Element |
|------|-------------|
| `button` | `<button>`, `<input type="button">` |
| `link` | `<a href="...">` |
| `textbox` | `<input type="text">`, `<textarea>` |
| `checkbox` | `<input type="checkbox">` |
| `radio` | `<input type="radio">` |
| `combobox` | `<select>` |
| `heading` | `<h1>` to `<h6>` |
| `img` | `<img>` |
| `list` | `<ul>`, `<ol>` |
| `listitem` | `<li>` |
| `dialog` | Modal/dialog boxes |

## 5.3 getByText() â€” Find by Visible Text

```typescript
test('getByText examples', async ({ page }) => {
  await page.goto('https://example.com');

  // Find element containing this text (partial match by default)
  await page.getByText('Welcome back').click();

  // Exact text match
  await page.getByText('Sign Out', { exact: true }).click();

  // Find in a specific container
  await page.getByRole('navigation').getByText('Settings').click();

  // Using regex
  await page.getByText(/welcome/i).isVisible();
});
```

> **Warning:** `getByText()` can match multiple elements. Be specific or chain with another locator.

## 5.4 getByPlaceholder() â€” Find Input by Placeholder

```typescript
test('getByPlaceholder examples', async ({ page }) => {
  await page.goto('https://example.com/login');

  // Find input field by its placeholder text
  await page.getByPlaceholder('Enter your email').fill('user@example.com');
  await page.getByPlaceholder('Enter password').fill('Secret@123');
  await page.getByPlaceholder('Search...').fill('Playwright');
});
```

## 5.5 getByLabel() â€” Find Input by Label

```typescript
test('getByLabel examples', async ({ page }) => {
  await page.goto('https://example.com/register');

  // Finds the input associated with the <label> text
  await page.getByLabel('First Name').fill('John');
  await page.getByLabel('Last Name').fill('Doe');
  await page.getByLabel('Email Address').fill('john@example.com');

  // Works with aria-label too
  await page.getByLabel('Close dialog').click();
});
```

## 5.6 getByTestId() â€” Find by data-testid

```typescript
test('getByTestId examples', async ({ page }) => {
  // HTML: <button data-testid="submit-btn">Submit</button>
  await page.getByTestId('submit-btn').click();

  // Custom test ID attribute (configure in playwright.config.ts)
  // use: { testIdAttribute: 'data-automation-id' }
  await page.getByTestId('login-form').getByRole('button').click();
});
```

## 5.7 CSS Selectors â€” When to Use

```typescript
test('CSS selector examples', async ({ page }) => {
  await page.goto('https://example.com');

  // By ID
  await page.locator('#login-button').click();

  // By class
  await page.locator('.btn-primary').click();

  // By attribute
  await page.locator('[data-id="123"]').click();
  await page.locator('input[type="email"]').fill('user@test.com');

  // Combine tag + class
  await page.locator('button.submit-btn').click();

  // Child combinator
  await page.locator('.login-form > button').click();

  // Descendant
  await page.locator('.nav a[href="/home"]').click();

  // nth-child
  await page.locator('table tr:nth-child(2) td:first-child').textContent();
});
```

## 5.8 XPath â€” Use Sparingly

```typescript
test('XPath examples', async ({ page }) => {
  // Use XPath only when CSS selectors and ARIA locators won't work

  // Find by text content (use getByText instead when possible)
  await page.locator('//button[text()="Submit"]').click();

  // Navigate up to parent
  await page.locator('//label[text()="Username"]/../input').fill('admin');

  // Contains text
  await page.locator('//div[contains(@class, "error")]').textContent();

  // Find sibling
  await page.locator('//td[text()="Order #123"]/following-sibling::td[1]').textContent();
});
```

**When to use XPath:**
- Navigating to a parent element (CSS can't go up the DOM)
- Complex sibling/ancestor relationships
- Legacy applications where no semantic locators exist

**When NOT to use XPath:**
- When `getByRole()`, `getByText()`, `getByLabel()` work fine
- For simple ID or class selectors
- In CI/CD pipelines (fragile, breaks on DOM changes)

## 5.9 Locator Chaining and Filtering

```typescript
test('locator chaining', async ({ page }) => {
  await page.goto('https://example.com');

  // Chain locators for precision
  const loginForm = page.locator('.login-form');
  await loginForm.getByLabel('Username').fill('admin');
  await loginForm.getByRole('button', { name: 'Login' }).click();

  // Filter by has-text
  await page.locator('li').filter({ hasText: 'Premium Plan' }).click();

  // Filter by has (another locator inside)
  await page.locator('tr').filter({ has: page.locator('td', { hasText: 'Active' }) }).click();

  // Get by index (0-based)
  await page.locator('.product-card').nth(2).click(); // Third card

  // First and last
  await page.locator('.product-card').first().click();
  await page.locator('.product-card').last().click();
});
```

## 5.10 Locator Strategy Comparison Table

| Strategy | Readability | Stability | When to Use |
|---|---|---|---|
| `getByRole()` | â­â­â­â­â­ | â­â­â­â­â­ | Always prefer this first |
| `getByLabel()` | â­â­â­â­â­ | â­â­â­â­â­ | For form inputs |
| `getByPlaceholder()` | â­â­â­â­ | â­â­â­â­ | When label is absent |
| `getByText()` | â­â­â­â­ | â­â­â­ | Unique visible text |
| `getByTestId()` | â­â­â­â­ | â­â­â­â­â­ | Dev-added test IDs |
| CSS Selector | â­â­â­ | â­â­â­ | When above fail |
| XPath | â­â­ | â­â­ | Last resort |

> **Interview Tip:** Always say you prefer `getByRole()` and `getByLabel()` because they align with how users and screen readers interact with the page â€” making tests more resilient to implementation changes.

---

# Chapter 6: Handling UI Elements

## 6.1 Text Inputs and Buttons

```typescript
import { test, expect } from '@playwright/test';

test('text input interactions', async ({ page }) => {
  await page.goto('https://example.com/register');

  // Fill â€” clears existing value and types new value
  await page.getByLabel('Full Name').fill('John Doe');

  // Type â€” types character by character (simulates human typing)
  await page.getByLabel('Username').pressSequentially('john_doe', { delay: 100 });

  // Clear an input
  await page.getByLabel('Full Name').clear();

  // Press a keyboard key
  await page.getByLabel('Search').fill('Playwright');
  await page.keyboard.press('Enter');

  // Press Tab to move to next field
  await page.getByLabel('Email').press('Tab');

  // Get the current value of an input
  const value: string | null = await page.getByLabel('Username').inputValue();
  console.log('Current value:', value);
});

test('button interactions', async ({ page }) => {
  await page.goto('https://example.com');

  // Standard click
  await page.getByRole('button', { name: 'Submit' }).click();

  // Double click
  await page.getByRole('button', { name: 'Edit' }).dblclick();

  // Right click (context menu)
  await page.getByText('file.txt').click({ button: 'right' });

  // Click with modifier key (Ctrl+Click for multi-select)
  await page.getByRole('listitem').nth(2).click({ modifiers: ['Control'] });

  // Click at specific position within element
  await page.getByRole('canvas').click({ position: { x: 100, y: 200 } });

  // Force click (ignores visibility/overlap checks â€” use sparingly)
  await page.getByRole('button').click({ force: true });
});
```

## 6.2 Dropdowns (Select Elements)

```typescript
test('dropdown interactions', async ({ page }) => {
  await page.goto('https://example.com/form');

  // Select by visible text
  await page.getByLabel('Country').selectOption({ label: 'India' });

  // Select by value attribute
  await page.getByLabel('Country').selectOption({ value: 'IN' });

  // Select by index (0-based)
  await page.getByLabel('Country').selectOption({ index: 2 });

  // Select multiple options (multi-select dropdowns)
  await page.getByLabel('Skills').selectOption(['typescript', 'playwright', 'jest']);

  // Get selected option text
  const selected = await page.getByLabel('Country').inputValue();
  console.log('Selected:', selected);

  // Custom dropdown (not a <select> element â€” click-based)
  await page.getByRole('combobox', { name: 'Category' }).click();
  await page.getByRole('option', { name: 'Electronics' }).click();
});
```

## 6.3 Checkboxes and Radio Buttons

```typescript
test('checkbox and radio button interactions', async ({ page }) => {
  await page.goto('https://example.com/preferences');

  // Check a checkbox
  await page.getByLabel('Remember me').check();

  // Uncheck a checkbox
  await page.getByLabel('Subscribe to newsletter').uncheck();

  // Verify checked state
  await expect(page.getByLabel('Remember me')).toBeChecked();
  await expect(page.getByLabel('Subscribe to newsletter')).not.toBeChecked();

  // Radio buttons â€” click to select
  await page.getByLabel('Male').check();
  await page.getByLabel('Female').check(); // Selecting Female unchecks Male

  // Verify radio selection
  await expect(page.getByLabel('Female')).toBeChecked();
  await expect(page.getByLabel('Male')).not.toBeChecked();
});
```

## 6.4 File Uploads

```typescript
import path from 'path';

test('file upload', async ({ page }) => {
  await page.goto('https://example.com/upload');

  // Single file upload
  await page.getByLabel('Profile Picture').setInputFiles(
    path.join(__dirname, '../test-data/profile.png')
  );

  // Multiple files
  await page.getByLabel('Attachments').setInputFiles([
    path.join(__dirname, '../test-data/doc1.pdf'),
    path.join(__dirname, '../test-data/doc2.pdf'),
  ]);

  // Remove selected files
  await page.getByLabel('Attachments').setInputFiles([]);

  // Upload using file chooser (click triggers file chooser)
  const [fileChooser] = await Promise.all([
    page.waitForEvent('filechooser'),
    page.getByRole('button', { name: 'Upload File' }).click(),
  ]);
  await fileChooser.setFiles(path.join(__dirname, '../test-data/report.pdf'));
});
```

## 6.5 Handling Browser Alerts (Dialogs)

```typescript
test('handling alert dialogs', async ({ page }) => {
  await page.goto('https://example.com');

  // Handle alert (OK/Cancel dialogs)
  // IMPORTANT: Set up the handler BEFORE triggering the dialog
  page.on('dialog', async (dialog) => {
    console.log('Dialog message:', dialog.message());
    console.log('Dialog type:', dialog.type()); // alert, confirm, prompt

    if (dialog.type() === 'confirm') {
      await dialog.accept(); // Click OK/Yes
    } else if (dialog.type() === 'prompt') {
      await dialog.accept('My input value'); // Provide input and click OK
    } else {
      await dialog.dismiss(); // Click Cancel/Dismiss
    }
  });

  // Trigger a button that opens an alert
  await page.getByRole('button', { name: 'Delete Account' }).click();
});
```

## 6.6 Frames and iFrames

```typescript
test('working with iframes', async ({ page }) => {
  await page.goto('https://example.com/page-with-iframe');

  // Locate frame by name attribute
  // <iframe name="paymentFrame" src="...">
  const paymentFrame = page.frameLocator('iframe[name="paymentFrame"]');

  // Interact with elements INSIDE the iframe
  await paymentFrame.getByLabel('Card Number').fill('4111 1111 1111 1111');
  await paymentFrame.getByLabel('Expiry').fill('12/25');
  await paymentFrame.getByLabel('CVV').fill('123');
  await paymentFrame.getByRole('button', { name: 'Pay Now' }).click();

  // Locate frame by src URL
  const recaptchaFrame = page.frameLocator('iframe[src*="recaptcha"]');
  await recaptchaFrame.getByRole('checkbox').click();

  // Nested iframes (iframe inside an iframe)
  const outerFrame = page.frameLocator('#outer-frame');
  const innerFrame = outerFrame.frameLocator('#inner-frame');
  await innerFrame.getByRole('button', { name: 'Submit' }).click();
});
```

> **Common Mistake:** Trying to interact with iframe elements without switching into the frame context. Always use `frameLocator()`.

---

# Chapter 7: Wait Mechanisms

## 7.1 Auto-Waiting â€” Playwright's Superpower

Playwright automatically waits for elements before performing actions. When you call `.click()`, Playwright waits for the element to be:
1. **Attached** to the DOM
2. **Visible** (not hidden)
3. **Stable** (not animating)
4. **Enabled** (not disabled)
5. **Editable** (for fill/type actions)

```typescript
test('auto-waiting in action', async ({ page }) => {
  await page.goto('https://example.com');

  // Playwright waits automatically â€” no sleep() needed
  // If button appears after 2 seconds, Playwright waits up to timeout (default 30s)
  await page.getByRole('button', { name: 'Load More' }).click();

  // After clicking, the new items appear â€” Playwright waits for them
  await expect(page.getByRole('listitem')).toHaveCount(20);
});
```

## 7.2 waitForSelector â€” Wait for DOM Element

```typescript
test('waitForSelector', async ({ page }) => {
  await page.goto('https://example.com');

  // Wait for element to appear in DOM (default: 'visible')
  await page.waitForSelector('.success-message');

  // Wait with options
  await page.waitForSelector('.loading-spinner', {
    state: 'hidden',     // Wait for spinner to disappear
    timeout: 10000,      // Wait max 10 seconds
  });

  // States: 'attached', 'detached', 'visible', 'hidden'
  await page.waitForSelector('.modal', { state: 'attached' });
  await page.waitForSelector('.modal', { state: 'detached' });
});
```

## 7.3 waitForLoadState â€” Wait for Page Load

```typescript
test('waitForLoadState', async ({ page }) => {
  await page.goto('https://example.com');

  // 'domcontentloaded' â€” HTML parsed, DOM ready (fastest)
  await page.waitForLoadState('domcontentloaded');

  // 'load' â€” All resources (images, CSS, JS) loaded
  await page.waitForLoadState('load');

  // 'networkidle' â€” No network activity for 500ms (slowest, most reliable)
  await page.waitForLoadState('networkidle');

  // Practical usage â€” after navigation or clicking links
  await page.getByRole('link', { name: 'Reports' }).click();
  await page.waitForLoadState('networkidle');
  await expect(page.getByRole('heading', { name: 'Reports' })).toBeVisible();
});
```

## 7.4 waitForURL â€” Wait for Navigation

```typescript
test('waitForURL', async ({ page }) => {
  await page.goto('https://example.com/login');

  await page.getByLabel('Email').fill('user@test.com');
  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Login' }).click();

  // Wait until URL matches pattern
  await page.waitForURL('**/dashboard');
  // OR
  await page.waitForURL(/dashboard/);
  // OR exact URL
  await page.waitForURL('https://example.com/dashboard');
});
```

## 7.5 waitForResponse â€” Wait for API Response

```typescript
test('wait for API response', async ({ page }) => {
  await page.goto('https://example.com');

  // Wait for a specific API call to complete
  const [response] = await Promise.all([
    page.waitForResponse(resp => resp.url().includes('/api/users') && resp.status() === 200),
    page.getByRole('button', { name: 'Load Users' }).click(),
  ]);

  const data = await response.json();
  console.log('Users loaded:', data.length);
});
```

## 7.6 Explicit Waits â€” Custom Conditions

```typescript
test('custom wait conditions', async ({ page }) => {
  await page.goto('https://example.com');

  // Wait for a function to return true
  await page.waitForFunction(() => {
    // This runs IN the browser context
    return document.querySelectorAll('.product').length > 5;
  });

  // Wait with polling interval
  await page.waitForFunction(
    (count) => document.querySelectorAll('.item').length >= count,
    10, // argument passed to the function
    { timeout: 15000, polling: 500 } // check every 500ms, timeout after 15s
  );

  // Wait for a fixed time (avoid when possible)
  await page.waitForTimeout(2000); // Only use for debugging!
});
```

## 7.7 Fixing Flaky Tests

Flaky tests pass sometimes and fail sometimes. Common causes and fixes:

```typescript
test('fixing common flakiness patterns', async ({ page }) => {
  await page.goto('https://example.com');

  // âŒ FLAKY: No wait â€” element might not exist yet
  // await page.locator('.dynamic-content').click();

  // âœ… FIXED: Let Playwright auto-wait (it does this automatically)
  await page.locator('.dynamic-content').waitFor({ state: 'visible' });
  await page.locator('.dynamic-content').click();

  // âŒ FLAKY: Clicking before animation completes
  // await page.locator('.modal .submit-btn').click();

  // âœ… FIXED: Wait for animation to complete
  await page.locator('.modal').waitFor({ state: 'visible' });
  await page.locator('.modal').waitFor({ state: 'stable' }); // Wait for animation
  await page.locator('.modal .submit-btn').click();

  // âŒ FLAKY: Race condition â€” element changes after click
  // await page.getByRole('button', { name: 'Submit' }).click();
  // await expect(page.getByRole('button', { name: 'Submit' })).toBeDisabled();

  // âœ… FIXED: Use Promise.all for simultaneous wait
  await Promise.all([
    expect(page.getByRole('button', { name: 'Submit' })).toBeDisabled(),
    page.getByRole('button', { name: 'Submit' }).click(),
  ]);
});
```

> **Key Rule:** Never use `waitForTimeout()` in production tests. It slows tests and doesn't fix the root cause. Use event-driven waits instead.

---

# Chapter 8: Multiple Pages, Tabs, and Popups

## 8.1 Handling New Tabs

```typescript
import { test, expect, Page } from '@playwright/test';

test('handling new tabs', async ({ page, context }) => {
  await page.goto('https://example.com');

  // Method 1: Wait for new page event
  const [newPage] = await Promise.all([
    context.waitForEvent('page'),           // Listen for new tab
    page.getByRole('link', { name: 'Open in New Tab' }).click(),
  ]);

  await newPage.waitForLoadState('domcontentloaded');
  console.log('New tab URL:', newPage.url());

  // Interact with the new tab
  await expect(newPage.getByRole('heading')).toBeVisible();
  await newPage.getByRole('button', { name: 'Confirm' }).click();

  // Close the new tab
  await newPage.close();

  // You are back on the original page
  await expect(page).toHaveURL('https://example.com');
});
```

## 8.2 Opening New Pages Programmatically

```typescript
test('open multiple pages', async ({ context }) => {
  // Create multiple pages in the same context (shared cookies/session)
  const page1: Page = await context.newPage();
  const page2: Page = await context.newPage();

  await page1.goto('https://example.com/admin');
  await page2.goto('https://example.com/reports');

  // Work on page1
  await page1.getByRole('button', { name: 'Create User' }).click();

  // Switch to page2
  await page2.bringToFront();
  await expect(page2.getByRole('heading', { name: 'Reports' })).toBeVisible();

  await page1.close();
  await page2.close();
});
```

## 8.3 Handling Popups

```typescript
test('handling window.open() popups', async ({ page, context }) => {
  await page.goto('https://example.com');

  // Handle popup triggered by window.open()
  const popupPromise = context.waitForEvent('page');
  await page.getByRole('button', { name: 'Open Report' }).click();
  const popup = await popupPromise;

  await popup.waitForLoadState('networkidle');
  console.log('Popup URL:', popup.url());

  // Download from popup (if it's a PDF/report)
  const [download] = await Promise.all([
    popup.waitForEvent('download'),
    popup.getByRole('button', { name: 'Download' }).click(),
  ]);
  await download.saveAs('./downloads/report.pdf');
});
```

---

# Chapter 9: Test Organization

## 9.1 test.describe â€” Grouping Tests

```typescript
import { test, expect } from '@playwright/test';

// Group related tests
test.describe('User Authentication', () => {
  test('should login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('user@test.com');
    await page.getByLabel('Password').fill('Secret@123');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('wrong@test.com');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByText('Invalid credentials')).toBeVisible();
  });

  test('should redirect to login when accessing protected page', async ({ page }) => {
    await page.goto('/dashboard'); // Not logged in
    await expect(page).toHaveURL('/login');
  });
});

// Nested describe blocks
test.describe('Shopping Cart', () => {
  test.describe('Add to Cart', () => {
    test('should add single item', async ({ page }) => { /* ... */ });
    test('should add multiple items', async ({ page }) => { /* ... */ });
  });

  test.describe('Remove from Cart', () => {
    test('should remove item', async ({ page }) => { /* ... */ });
  });
});
```

## 9.2 Hooks â€” before and after

```typescript
import { test, expect } from '@playwright/test';

test.describe('Order Management', () => {
  // Runs ONCE before all tests in this describe block
  test.beforeAll(async () => {
    console.log('Setting up test data in database...');
    // Seed test data, create test users, etc.
  });

  // Runs ONCE after all tests in this describe block
  test.afterAll(async () => {
    console.log('Cleaning up test data...');
    // Delete test data, clean up resources
  });

  // Runs before EACH test
  test.beforeEach(async ({ page }) => {
    // Login before every test
    await page.goto('/login');
    await page.getByLabel('Email').fill('admin@test.com');
    await page.getByLabel('Password').fill('Admin@123');
    await page.getByRole('button', { name: 'Login' }).click();
    await page.waitForURL('/dashboard');
  });

  // Runs after EACH test
  test.afterEach(async ({ page }) => {
    // Take screenshot on failure (Playwright does this automatically with config)
    const testInfo = test.info();
    if (testInfo.status !== testInfo.expectedStatus) {
      await page.screenshot({ path: `test-results/failure-${testInfo.title}.png` });
    }
  });

  test('should create order', async ({ page }) => {
    // Page is already logged in (from beforeEach)
    await page.goto('/orders/new');
    // ... test logic
  });

  test('should view order list', async ({ page }) => {
    // Page is already logged in (from beforeEach)
    await page.goto('/orders');
    // ... test logic
  });
});
```

## 9.3 Fixtures â€” Custom Reusable Setup

Fixtures are Playwright's way of providing pre-configured objects to tests. They are more powerful than hooks.

```typescript
// fixtures/auth-fixture.ts
import { test as base, expect, Page } from '@playwright/test';

// Define what extra fixtures your tests need
type AuthFixtures = {
  loggedInPage: Page;
  adminPage: Page;
};

// Extend the base test with your fixtures
export const test = base.extend<AuthFixtures>({
  // This fixture logs in as a regular user
  loggedInPage: async ({ page }, use) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('user@test.com');
    await page.getByLabel('Password').fill('User@123');
    await page.getByRole('button', { name: 'Login' }).click();
    await page.waitForURL('/dashboard');

    // Provide the logged-in page to the test
    await use(page);

    // Cleanup after test (logout)
    await page.getByRole('button', { name: 'Logout' }).click();
  },

  // This fixture logs in as an admin
  adminPage: async ({ page }, use) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('admin@test.com');
    await page.getByLabel('Password').fill('Admin@123');
    await page.getByRole('button', { name: 'Login' }).click();
    await page.waitForURL('/admin-dashboard');
    await use(page);
  },
});

export { expect };
```

```typescript
// tests/profile.spec.ts â€” Using the custom fixture
import { test, expect } from '../fixtures/auth-fixture';

test('user can update profile', async ({ loggedInPage }) => {
  // loggedInPage is already logged in!
  await loggedInPage.goto('/profile');
  await loggedInPage.getByLabel('Display Name').fill('John Updated');
  await loggedInPage.getByRole('button', { name: 'Save' }).click();
  await expect(loggedInPage.getByText('Profile updated successfully')).toBeVisible();
});

test('admin can delete user', async ({ adminPage }) => {
  await adminPage.goto('/admin/users');
  await adminPage.getByRole('row', { name: 'test-user@example.com' })
    .getByRole('button', { name: 'Delete' }).click();
  await expect(adminPage.getByText('User deleted')).toBeVisible();
});
```

## 9.4 Page Object Model (POM)

POM separates **test logic** from **page interaction logic**. Each page of your app has a corresponding class.

```typescript
// pages/LoginPage.ts
import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
  // Declare locators as class properties
  private readonly emailInput: Locator;
  private readonly passwordInput: Locator;
  private readonly loginButton: Locator;
  private readonly errorMessage: Locator;

  constructor(private page: Page) {
    this.emailInput = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.loginButton = page.getByRole('button', { name: 'Login' });
    this.errorMessage = page.getByRole('alert');
  }

  // Navigation method
  async navigate(): Promise<void> {
    await this.page.goto('/login');
  }

  // Action methods
  async login(email: string, password: string): Promise<void> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  // Assertion methods
  async expectSuccessfulLogin(): Promise<void> {
    await expect(this.page).toHaveURL('/dashboard');
  }

  async expectErrorMessage(message: string): Promise<void> {
    await expect(this.errorMessage).toHaveText(message);
  }
}
```

```typescript
// pages/DashboardPage.ts
import { Page, Locator, expect } from '@playwright/test';

export class DashboardPage {
  private readonly welcomeMessage: Locator;
  private readonly navigationMenu: Locator;

  constructor(private page: Page) {
    this.welcomeMessage = page.getByRole('heading', { name: /Welcome/ });
    this.navigationMenu = page.getByRole('navigation');
  }

  async navigateTo(section: string): Promise<void> {
    await this.navigationMenu.getByRole('link', { name: section }).click();
  }

  async expectWelcomeMessage(username: string): Promise<void> {
    await expect(this.welcomeMessage).toContainText(username);
  }
}
```

```typescript
// tests/login.spec.ts â€” Clean test using POM
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';

test.describe('Login Tests', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    await loginPage.navigate();
  });

  test('should login with valid credentials', async () => {
    await loginPage.login('user@test.com', 'Secret@123');
    await loginPage.expectSuccessfulLogin();
    await dashboardPage.expectWelcomeMessage('John');
  });

  test('should show error with invalid credentials', async () => {
    await loginPage.login('wrong@test.com', 'WrongPassword');
    await loginPage.expectErrorMessage('Invalid email or password');
  });
});
```

> **POM Benefits:**
> - Tests are readable â€” reads like plain English
> - Changes to UI â†’ update ONE page class, not every test
> - Reusable across multiple test files
> - Maintainable at scale

---

# Chapter 10: Assertions â€” Deep Dive

## 10.1 Hard vs Soft Assertions

**Hard assertions** (default) â€” Stop the test immediately when they fail.
**Soft assertions** â€” Continue running even after failure, report all failures at the end.

```typescript
import { test, expect } from '@playwright/test';

test('hard assertion (default)', async ({ page }) => {
  await page.goto('https://example.com/profile');

  // If this fails, test stops here
  await expect(page.getByRole('heading')).toHaveText('My Profile');

  // This line never runs if above fails
  await expect(page.getByLabel('Email')).toHaveValue('user@test.com');
});

test('soft assertions â€” continue after failure', async ({ page }) => {
  await page.goto('https://example.com/profile');

  // Soft assertions â€” test continues even if these fail
  await expect.soft(page.getByRole('heading')).toHaveText('My Profile');
  await expect.soft(page.getByLabel('Email')).toHaveValue('user@test.com');
  await expect.soft(page.getByLabel('Phone')).toHaveValue('+91-9876543210');

  // All failures are reported together at the end
  // Useful for form validation tests â€” check all fields at once
});
```

## 10.2 UI Assertions

```typescript
test('comprehensive UI assertions', async ({ page }) => {
  await page.goto('https://example.com');

  const button = page.getByRole('button', { name: 'Submit' });
  const input = page.getByLabel('Email');
  const errorMsg = page.getByRole('alert');

  // Visibility
  await expect(button).toBeVisible();
  await expect(errorMsg).toBeHidden(); // or not.toBeVisible()

  // State
  await expect(button).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Processing' })).toBeDisabled();
  await expect(page.getByLabel('Remember me')).toBeChecked();
  await expect(page.getByLabel('Newsletter')).not.toBeChecked();

  // Text content
  await expect(button).toHaveText('Submit Form');
  await expect(button).toContainText('Submit'); // Partial match
  await expect(page.getByRole('heading')).toHaveText(/Welcome/); // Regex

  // Input value
  await expect(input).toHaveValue('user@test.com');
  await expect(input).toBeEmpty();

  // Attributes
  await expect(button).toHaveAttribute('type', 'submit');
  await expect(page.getByRole('img')).toHaveAttribute('alt', 'Company Logo');

  // CSS class
  await expect(button).toHaveClass(/btn-primary/);
  await expect(errorMsg).toHaveClass('error-message visible');

  // Count
  await expect(page.getByRole('listitem')).toHaveCount(5);
});
```

## 10.3 Page-Level Assertions

```typescript
test('page-level assertions', async ({ page }) => {
  await page.goto('https://example.com');

  // URL assertions
  await expect(page).toHaveURL('https://example.com/');
  await expect(page).toHaveURL(/example\.com/);
  await expect(page).toHaveURL('**/home'); // Glob pattern

  // Title assertions
  await expect(page).toHaveTitle('Example Domain');
  await expect(page).toHaveTitle(/Example/);
});
```

## 10.4 Custom Assertions

```typescript
// utils/custom-assertions.ts
import { expect, Locator, Page } from '@playwright/test';

// Custom matcher for toast messages
export async function expectToastMessage(page: Page, message: string): Promise<void> {
  const toast = page.locator('.toast-notification');
  await expect(toast).toBeVisible({ timeout: 5000 });
  await expect(toast).toContainText(message);
  await expect(toast).toBeHidden({ timeout: 5000 }); // Toasts auto-dismiss
}

// Custom matcher for table row data
export async function expectTableRow(
  page: Page,
  rowIndex: number,
  expectedData: string[]
): Promise<void> {
  const row = page.locator(`table tbody tr:nth-child(${rowIndex + 1})`);
  const cells = row.locator('td');
  await expect(cells).toHaveCount(expectedData.length);
  for (let i = 0; i < expectedData.length; i++) {
    await expect(cells.nth(i)).toHaveText(expectedData[i]);
  }
}
```

```typescript
// tests/orders.spec.ts â€” Using custom assertions
import { test } from '@playwright/test';
import { expectToastMessage, expectTableRow } from '../utils/custom-assertions';

test('order creation shows success toast', async ({ page }) => {
  await page.goto('/orders/new');
  await page.getByRole('button', { name: 'Create Order' }).click();
  await expectToastMessage(page, 'Order created successfully');
});
```

> **Interview Tip:** Soft assertions are valuable in data validation scenarios â€” e.g., verifying all fields of a user profile at once rather than stopping at the first mismatch.

---

# Chapter 11: Screenshots, Videos, and Traces

## 11.1 Screenshots

```typescript
import { test, expect } from '@playwright/test';
import path from 'path';

test('screenshot examples', async ({ page }) => {
  await page.goto('https://example.com');

  // Full page screenshot
  await page.screenshot({
    path: 'screenshots/full-page.png',
    fullPage: true,
  });

  // Viewport-only screenshot (default)
  await page.screenshot({ path: 'screenshots/viewport.png' });

  // Element-level screenshot
  const card = page.getByRole('article').first();
  await card.screenshot({ path: 'screenshots/card.png' });

  // Screenshot as Buffer (useful for attaching to reports)
  const buffer = await page.screenshot({ fullPage: true });
  await test.info().attach('page-screenshot', {
    body: buffer,
    contentType: 'image/png',
  });

  // Clip to specific area
  await page.screenshot({
    path: 'screenshots/clipped.png',
    clip: { x: 0, y: 0, width: 800, height: 400 },
  });
});
```

**playwright.config.ts â€” Auto-screenshot on failure:**
```typescript
use: {
  screenshot: 'only-on-failure',  // Options: 'off', 'on', 'only-on-failure'
}
```

## 11.2 Video Recording

```typescript
// playwright.config.ts
use: {
  video: 'retain-on-failure', // Options: 'off', 'on', 'retain-on-failure', 'on-first-retry'
}
```

```typescript
// Per-test video control
test('record specific test', async ({ page }) => {
  // Video is recorded automatically based on config
  await page.goto('https://example.com');
  await page.getByRole('button', { name: 'Start Tour' }).click();
  // Video saved to test-results/ folder on failure
});
```

## 11.3 Trace Viewer â€” Most Powerful Debugging Tool

Trace captures: DOM snapshots, network requests, console logs, screenshots at every step.

```typescript
// playwright.config.ts
use: {
  trace: 'on-first-retry', // Options: 'off', 'on', 'on-first-retry', 'retain-on-failure'
}
```

```typescript
// Manually capture trace in a test
test('trace a specific flow', async ({ page, context }) => {
  // Start tracing
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true });

  await page.goto('https://example.com');
  await page.getByRole('button', { name: 'Checkout' }).click();

  // Stop and save trace
  await context.tracing.stop({ path: 'traces/checkout-trace.zip' });
});
```

**View the trace:**
```
npx playwright show-trace traces/checkout-trace.zip
```

> The Trace Viewer shows a timeline. Click any step to see the DOM state at that exact moment â€” like a time machine for your test.

---

# Chapter 12: Test Data Handling

## 12.1 JSON Test Data

```typescript
// test-data/users.json
// {
//   "validUser": { "email": "user@test.com", "password": "Secret@123" },
//   "adminUser": { "email": "admin@test.com", "password": "Admin@123" },
//   "invalidUser": { "email": "bad@test.com", "password": "wrong" }
// }

import { test, expect } from '@playwright/test';
import testData from '../test-data/users.json';

test('login with valid user from JSON', async ({ page }) => {
  const { email, password } = testData.validUser;
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page).toHaveURL('/dashboard');
});
```

## 12.2 Data-Driven Tests

```typescript
// Test multiple users with the same test logic
const users = [
  { role: 'admin',  email: 'admin@test.com',  password: 'Admin@123',  expectedUrl: '/admin' },
  { role: 'user',   email: 'user@test.com',   password: 'User@123',   expectedUrl: '/dashboard' },
  { role: 'viewer', email: 'viewer@test.com', password: 'Viewer@123', expectedUrl: '/view' },
];

for (const user of users) {
  test(`login as ${user.role}`, async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(user.email);
    await page.getByLabel('Password').fill(user.password);
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page).toHaveURL(user.expectedUrl);
  });
}
```

## 12.3 Environment Variables and .env Files

```typescript
// .env
// BASE_URL=https://staging.example.com
// ADMIN_EMAIL=admin@test.com
// ADMIN_PASSWORD=Admin@123
// API_KEY=abc123xyz

// Install dotenv: npm install dotenv --save-dev
```

```typescript
// playwright.config.ts â€” Load .env
import { defineConfig } from '@playwright/test';
import dotenv from 'dotenv';

dotenv.config(); // Load .env file

export default defineConfig({
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
  },
});
```

```typescript
// In tests â€” use process.env
test('login with env credentials', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill(process.env.ADMIN_EMAIL!);
  await page.getByLabel('Password').fill(process.env.ADMIN_PASSWORD!);
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page).toHaveURL('/admin');
});
```

## 12.4 Dynamic Test Data â€” Faker

```typescript
// npm install @faker-js/faker --save-dev
import { faker } from '@faker-js/faker';

test('register new user with dynamic data', async ({ page }) => {
  const newUser = {
    firstName: faker.person.firstName(),
    lastName: faker.person.lastName(),
    email: faker.internet.email(),
    password: faker.internet.password({ length: 12, memorable: false }),
    phone: faker.phone.number(),
  };

  await page.goto('/register');
  await page.getByLabel('First Name').fill(newUser.firstName);
  await page.getByLabel('Last Name').fill(newUser.lastName);
  await page.getByLabel('Email').fill(newUser.email);
  await page.getByLabel('Password').fill(newUser.password);
  await page.getByRole('button', { name: 'Register' }).click();
  await expect(page.getByText('Registration successful')).toBeVisible();
});
```

---

# Chapter 13: API Testing with Playwright

## 13.1 Why API Testing in Playwright?

Playwright's `request` context lets you make HTTP calls directly â€” without a browser. You can:
- Set up test data via API before UI tests
- Verify backend state after UI actions
- Test APIs in isolation (faster than UI tests)

## 13.2 GET Request

```typescript
import { test, expect, request } from '@playwright/test';

test('GET â€” fetch user details', async ({ request }) => {
  const response = await request.get('https://jsonplaceholder.typicode.com/users/1');

  // Assert status code
  expect(response.status()).toBe(200);
  expect(response.ok()).toBeTruthy(); // True if status is 200-299

  // Parse JSON response
  const user = await response.json();
  expect(user.id).toBe(1);
  expect(user.email).toContain('@');
  expect(user.name).toBeTruthy();
});
```

## 13.3 POST Request

```typescript
test('POST â€” create new user via API', async ({ request }) => {
  const newUser = {
    name: 'John Doe',
    email: 'john@test.com',
    role: 'user',
  };

  const response = await request.post('https://api.example.com/users', {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${process.env.API_TOKEN}`,
    },
    data: newUser, // Playwright auto-serializes to JSON
  });

  expect(response.status()).toBe(201); // Created
  const createdUser = await response.json();
  expect(createdUser.id).toBeDefined();
  expect(createdUser.email).toBe(newUser.email);
});
```

## 13.4 PUT and DELETE

```typescript
test('PUT â€” update user', async ({ request }) => {
  const response = await request.put('https://api.example.com/users/123', {
    headers: { 'Authorization': `Bearer ${process.env.API_TOKEN}` },
    data: { name: 'Jane Updated' },
  });
  expect(response.status()).toBe(200);
  const updated = await response.json();
  expect(updated.name).toBe('Jane Updated');
});

test('DELETE â€” remove user', async ({ request }) => {
  const response = await request.delete('https://api.example.com/users/123', {
    headers: { 'Authorization': `Bearer ${process.env.API_TOKEN}` },
  });
  expect(response.status()).toBe(204); // No Content
});
```

## 13.5 Combined UI + API Testing

```typescript
test('create order via API, verify in UI', async ({ page, request }) => {
  // Step 1: Create order via API (fast â€” no UI interaction)
  const orderResponse = await request.post('/api/orders', {
    data: { productId: 'PROD-001', quantity: 2, userId: 'USER-123' },
    headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
  });
  expect(orderResponse.status()).toBe(201);
  const order = await orderResponse.json();
  const orderId: string = order.id;

  // Step 2: Login via UI and verify the order appears
  await page.goto('/login');
  await page.getByLabel('Email').fill('user@test.com');
  await page.getByLabel('Password').fill('Secret@123');
  await page.getByRole('button', { name: 'Login' }).click();

  await page.goto(`/orders/${orderId}`);
  await expect(page.getByText(orderId)).toBeVisible();
  await expect(page.getByText('2 items')).toBeVisible();
});

test('UI action triggers correct API call', async ({ page }) => {
  await page.goto('/products');

  // Intercept the API call made by the UI
  const responsePromise = page.waitForResponse(
    resp => resp.url().includes('/api/cart') && resp.request().method() === 'POST'
  );

  await page.getByRole('button', { name: 'Add to Cart' }).first().click();
  const cartResponse = await responsePromise;

  expect(cartResponse.status()).toBe(200);
  const cartData = await cartResponse.json();
  expect(cartData.totalItems).toBeGreaterThan(0);
});
```

## 13.6 API Authentication â€” Login via API

```typescript
// fixtures/api-auth-fixture.ts
import { test as base, request as requestLib } from '@playwright/test';

type ApiFixtures = {
  authToken: string;
};

export const test = base.extend<ApiFixtures>({
  authToken: async ({ request }, use) => {
    // Get auth token via API (faster than UI login)
    const response = await request.post('/api/auth/login', {
      data: { email: 'admin@test.com', password: 'Admin@123' },
    });
    const { token } = await response.json();
    await use(token);
  },
});
```

---

# Chapter 14: Cross-Browser and Mobile Testing

## 14.1 Running on Multiple Browsers

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  projects: [
    {
      name: 'Desktop Chrome',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'Desktop Firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'Desktop Safari',
      use: { ...devices['Desktop Safari'] },
    },
  ],
});
```

```typescript
// Run on all browsers
// npx playwright test

// Run on specific browser
// npx playwright test --project="Desktop Chrome"
// npx playwright test --project="Desktop Firefox"
```

## 14.2 Mobile Device Emulation

```typescript
// playwright.config.ts
projects: [
  {
    name: 'Mobile Chrome (Pixel 5)',
    use: { ...devices['Pixel 5'] },
  },
  {
    name: 'Mobile Safari (iPhone 12)',
    use: { ...devices['iPhone 12'] },
  },
  {
    name: 'Tablet (iPad)',
    use: { ...devices['iPad Pro'] },
  },
],
```

```typescript
// Manual device emulation in a test
import { devices } from '@playwright/test';

test('mobile viewport test', async ({ browser }) => {
  const context = await browser.newContext({
    ...devices['iPhone 14'],
    locale: 'en-US',
    geolocation: { latitude: 28.6139, longitude: 77.2090 }, // New Delhi
    permissions: ['geolocation'],
  });
  const page = await context.newPage();

  await page.goto('https://example.com');

  // Check mobile menu exists
  await expect(page.getByRole('button', { name: 'Menu' })).toBeVisible();
  await page.getByRole('button', { name: 'Menu' }).click();
  await expect(page.getByRole('navigation')).toBeVisible();

  await context.close();
});
```

## 14.3 Handling Browser-Specific Behavior

```typescript
import { test, expect } from '@playwright/test';

test('browser-specific handling', async ({ page, browserName }) => {
  await page.goto('https://example.com');

  if (browserName === 'webkit') {
    // Safari-specific behavior
    console.log('Running on Safari (WebKit)');
    // Handle Safari-specific date picker, etc.
  }

  if (browserName === 'firefox') {
    // Firefox-specific handling
    console.log('Running on Firefox');
  }

  // Skip a test for a specific browser
  test.skip(browserName === 'webkit', 'Safari does not support this feature yet');
});
```

---

# Chapter 15: Parallel Execution

## 15.1 How Playwright Parallelism Works

```typescript
// playwright.config.ts
export default defineConfig({
  // Tests in DIFFERENT files run in parallel (default)
  fullyParallel: false, // Default: tests within a file run sequentially

  // Enable parallelism within each file too
  fullyParallel: true,

  // Number of parallel workers
  workers: 4,          // Fixed number
  workers: '50%',      // 50% of CPU cores
  workers: undefined,  // Auto (based on CPU) â€” recommended for local
});
```

```typescript
// Force a specific file to run sequentially (useful for order-dependent tests)
test.describe.configure({ mode: 'serial' });

test.describe('Sequential Payment Flow', () => {
  test('step 1 â€” add to cart', async ({ page }) => { /* ... */ });
  test('step 2 â€” checkout', async ({ page }) => { /* ... */ });
  test('step 3 â€” confirm payment', async ({ page }) => { /* ... */ });
});
```

## 15.2 Test Sharding (For CI/CD)

Sharding splits tests across multiple machines:

```typescript
// Machine 1 runs shard 1 of 4
// npx playwright test --shard=1/4

// Machine 2 runs shard 2 of 4
// npx playwright test --shard=2/4

// GitHub Actions example:
// strategy:
//   matrix:
//     shard: [1, 2, 3, 4]
// steps:
//   - run: npx playwright test --shard=${{ matrix.shard }}/4
```

## 15.3 Retry Logic

```typescript
// playwright.config.ts
retries: process.env.CI ? 2 : 0, // Retry 2 times in CI, 0 locally

// Per-test retry
test('unstable external API test', async ({ page }) => {
  test.info().retry; // Current retry number (0 = first attempt)
}, { retries: 3 });
```

## 15.4 Timeouts

```typescript
// playwright.config.ts
export default defineConfig({
  timeout: 60000,           // Test timeout: 60 seconds
  expect: {
    timeout: 10000,         // Assertion timeout: 10 seconds
  },
  use: {
    actionTimeout: 30000,   // Per-action timeout: 30 seconds
    navigationTimeout: 30000,
  },
});

// Override timeout per test
test('long running process', async ({ page }) => {
  test.setTimeout(120000); // 2 minutes for this test only
  await page.goto('/generate-large-report');
  await page.waitForLoadState('networkidle');
});
```

---

# Chapter 16: Reporting

## 16.1 Built-in HTML Reporter

```typescript
// playwright.config.ts
reporter: [
  ['html', { open: 'never', outputFolder: 'playwright-report' }],
],

// After tests run:
// npx playwright show-report
// Opens: http://localhost:9323
```

The HTML report shows:
- Pass/Fail/Skip counts
- Test duration
- Failure screenshots, videos, traces
- Filter by project, tag, status

## 16.2 Multiple Reporters

```typescript
// playwright.config.ts
reporter: [
  ['html'],                                    // HTML report (local)
  ['junit', { outputFile: 'results.xml' }],    // For Jenkins/Azure DevOps
  ['json', { outputFile: 'results.json' }],    // For custom processing
  ['dot'],                                     // Minimal console output
  ['list'],                                    // Detailed console output
],
```

## 16.3 Custom Reporter

```typescript
// reporters/custom-reporter.ts
import {
  Reporter,
  TestCase,
  TestResult,
  FullResult,
  Suite,
} from '@playwright/test/reporter';

class CustomReporter implements Reporter {
  private passCount = 0;
  private failCount = 0;
  private startTime: number = Date.now();

  onBegin(config: any, suite: Suite): void {
    console.log(`\nðŸŽ¬ Starting ${suite.allTests().length} tests...\n`);
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const icon = result.status === 'passed' ? 'âœ…' : result.status === 'failed' ? 'âŒ' : 'âš ï¸';
    console.log(`${icon} ${test.title} [${result.duration}ms]`);

    if (result.status === 'passed') this.passCount++;
    else this.failCount++;
  }

  onEnd(result: FullResult): void {
    const duration = ((Date.now() - this.startTime) / 1000).toFixed(2);
    console.log(`\nðŸ“Š Results: ${this.passCount} passed, ${this.failCount} failed`);
    console.log(`â±ï¸  Total time: ${duration}s`);
    console.log(`ðŸ Status: ${result.status.toUpperCase()}\n`);
  }
}

export default CustomReporter;
```

```typescript
// playwright.config.ts â€” use custom reporter
reporter: [
  ['./reporters/custom-reporter.ts'],
  ['html'],
],
```

## 16.4 Allure Reporter (Popular in Enterprise)

```typescript
// npm install allure-playwright --save-dev

// playwright.config.ts
reporter: [
  ['allure-playwright'],
],

// After test run:
// npx allure generate allure-results --clean -o allure-report
// npx allure open allure-report
```

---

# Chapter 17: CI/CD Integration

## 17.1 GitHub Actions

```yaml
# .github/workflows/playwright.yml
name: Playwright Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 6 * * *'  # Run daily at 6 AM UTC

jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 60

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Install Playwright browsers
        run: npx playwright install --with-deps

      - name: Run tests
        run: npx playwright test
        env:
          BASE_URL: ${{ secrets.BASE_URL }}
          ADMIN_EMAIL: ${{ secrets.ADMIN_EMAIL }}
          ADMIN_PASSWORD: ${{ secrets.ADMIN_PASSWORD }}
          CI: true

      - name: Upload test report
        uses: actions/upload-artifact@v4
        if: always()  # Upload even if tests fail
        with:
          name: playwright-report
          path: playwright-report/
          retention-days: 30

      - name: Upload traces
        uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: test-traces
          path: test-results/
```

## 17.2 Parallel Sharding in GitHub Actions

```yaml
jobs:
  test:
    name: "Tests (Shard ${{ matrix.shard }}/${{ strategy.job-total }})"
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        shard: [1, 2, 3, 4]

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npx playwright test --shard=${{ matrix.shard }}/${{ strategy.job-total }}
        env:
          CI: true
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report-${{ matrix.shard }}
          path: playwright-report/
```

## 17.3 Jenkins Pipeline

```groovy
// Jenkinsfile
pipeline {
    agent any

    tools {
        nodejs '20.x'
    }

    environment {
        BASE_URL = credentials('BASE_URL')
        ADMIN_EMAIL = credentials('ADMIN_EMAIL')
        ADMIN_PASSWORD = credentials('ADMIN_PASSWORD')
    }

    stages {
        stage('Install') {
            steps {
                sh 'npm ci'
                sh 'npx playwright install --with-deps'
            }
        }

        stage('Test') {
            steps {
                sh 'npx playwright test --reporter=junit,html'
            }
        }

        stage('Publish Report') {
            steps {
                junit 'results.xml'
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'playwright-report',
                    reportFiles: 'index.html',
                    reportName: 'Playwright Report',
                ])
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'playwright-report/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'test-results/**', allowEmptyArchive: true
        }
        failure {
            emailext(
                subject: "âŒ Playwright Tests FAILED: ${env.JOB_NAME}",
                body: "Test run failed. Check report: ${env.BUILD_URL}",
                to: 'team@example.com'
            )
        }
    }
}
```

## 17.4 Headless Execution

```typescript
// playwright.config.ts â€” always headless in CI
use: {
  headless: process.env.CI ? true : false,
  // Or simply:
  headless: !!process.env.CI,
},

// Command line override:
// npx playwright test --headed          (force headed)
// npx playwright test --headless=false  (force headed)
```

---

# Chapter 18: Debugging and Troubleshooting

## 18.1 Playwright Inspector (PWDEBUG)

```typescript
// Set environment variable before running
// Windows:  $env:PWDEBUG=1; npx playwright test
// Linux/Mac: PWDEBUG=1 npx playwright test

// This opens the Playwright Inspector:
// - Step through each action
// - See locators highlighted in the browser
// - View action log
// - Explore page with selector picker
```

## 18.2 Debug Mode with CLI

```typescript
// Run a specific test in debug mode
// npx playwright test tests/login.spec.ts --debug

// Debug from a specific line using breakpoints in VS Code
// Add this in your test:
await page.pause(); // Pauses execution â€” opens Playwright Inspector

// Then resume manually in the inspector
```

## 18.3 Slow Motion

```typescript
// playwright.config.ts â€” slow down ALL tests
use: {
  launchOptions: {
    slowMo: 500, // 500ms delay between each action
  },
},

// Per-browser in test
test('slow motion debugging', async ({ browser }) => {
  const context = await browser.newContext();
  const page = await context.newPage();
  // slowMo is set at browser launch, not context level
  await page.goto('https://example.com');
});
```

## 18.4 Console and Network Logs

```typescript
test('capture console and network logs', async ({ page }) => {
  // Capture console messages
  page.on('console', (msg) => {
    console.log(`Browser console [${msg.type()}]: ${msg.text()}`);
  });

  // Capture network requests
  page.on('request', (req) => {
    if (req.url().includes('/api')) {
      console.log(`â†’ ${req.method()} ${req.url()}`);
    }
  });

  // Capture network responses
  page.on('response', (resp) => {
    if (resp.url().includes('/api')) {
      console.log(`â† ${resp.status()} ${resp.url()}`);
    }
  });

  // Capture page errors
  page.on('pageerror', (error) => {
    console.error('Page error:', error.message);
  });

  await page.goto('https://example.com');
  // All console/network activity is logged
});
```

## 18.5 Network Interception (Mocking)

```typescript
test('mock API response', async ({ page }) => {
  // Intercept and mock an API call
  await page.route('/api/products', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, name: 'Mock Product A', price: 999 },
        { id: 2, name: 'Mock Product B', price: 1499 },
      ]),
    });
  });

  await page.goto('/products');
  await expect(page.getByText('Mock Product A')).toBeVisible();
  await expect(page.getByText('Mock Product B')).toBeVisible();
});

test('simulate API error', async ({ page }) => {
  await page.route('/api/users', async (route) => {
    await route.fulfill({
      status: 500,
      body: 'Internal Server Error',
    });
  });

  await page.goto('/users');
  await expect(page.getByText('Failed to load users')).toBeVisible();
});

test('modify API response', async ({ page }) => {
  await page.route('/api/cart', async (route) => {
    const response = await route.fetch(); // Get real response
    const json = await response.json();

    // Modify it
    json.totalItems = 99;

    await route.fulfill({ response, json }); // Return modified
  });

  await page.goto('/cart');
  await expect(page.getByText('99 items')).toBeVisible();
});
```

## 18.6 Common Issues and Fixes

| Issue | Cause | Fix |
|---|---|---|
| `Timeout exceeded` | Element not found in time | Check locator, increase timeout, check if element is in iframe |
| `Element is not visible` | Element exists but hidden | Wait for visibility: `.waitFor({ state: 'visible' })` |
| `strict mode violation` | Multiple elements match locator | Make locator more specific, use `.nth()` or `.filter()` |
| `Navigation timeout` | Page takes too long to load | Increase `navigationTimeout` in config |
| `Locator not found` | Wrong selector | Use Playwright Inspector to find correct selector |
| `Test is flaky` | Race condition | Use proper wait strategies, avoid `waitForTimeout` |
| `Out of memory` | Too many parallel workers | Reduce `workers` in config |

---

# Chapter 19: Best Practices

## 19.1 Test Naming Conventions

```typescript
// âœ… GOOD â€” Descriptive, action-focused, outcome-clear
test('should display error message when password is less than 8 characters', ...);
test('should redirect to dashboard after successful login', ...);
test('should disable submit button when required fields are empty', ...);

// âŒ BAD â€” Vague, non-descriptive
test('login test', ...);
test('test 1', ...);
test('check button', ...);
```

## 19.2 Locator Best Practices

```typescript
// âœ… BEST â€” Use semantic locators
await page.getByRole('button', { name: 'Submit Order' }).click();
await page.getByLabel('Email Address').fill('user@test.com');

// âœ… GOOD â€” Use data-testid when semantic locators are insufficient
await page.getByTestId('checkout-button').click();

// âš ï¸ ACCEPTABLE â€” CSS selectors for structural things
await page.locator('table tbody tr').nth(0).click();

// âŒ AVOID â€” Brittle XPath
await page.locator('//div[@class="container"]/div[2]/button[1]').click();

// âŒ AVOID â€” Auto-generated class names (change on every build)
await page.locator('.css-1abc2def').click();
```

## 19.3 Clean Test Structure

```typescript
// âœ… GOOD â€” AAA Pattern (Arrange, Act, Assert)
test('should calculate order total correctly', async ({ page }) => {
  // ARRANGE â€” Set up prerequisites
  await page.goto('/cart');
  await page.getByRole('button', { name: 'Add Item' }).click();

  // ACT â€” Perform the action being tested
  await page.getByRole('button', { name: 'Apply Coupon' }).click();
  await page.getByLabel('Coupon Code').fill('SAVE10');
  await page.getByRole('button', { name: 'Apply' }).click();

  // ASSERT â€” Verify the outcome
  await expect(page.getByTestId('order-total')).toHaveText('â‚¹900');
  await expect(page.getByText('10% discount applied')).toBeVisible();
});
```

## 19.4 Avoid These Common Mistakes

```typescript
// âŒ MISTAKE 1: Using waitForTimeout (hardcoded sleep)
await page.waitForTimeout(3000); // Slow and unreliable

// âœ… FIX: Use event-based waits
await page.waitForLoadState('networkidle');
await page.getByText('Loading...').waitFor({ state: 'hidden' });

// âŒ MISTAKE 2: Not using baseURL
await page.goto('https://staging.example.com/login'); // Hardcoded URL

// âœ… FIX: Use baseURL in config and relative paths
await page.goto('/login'); // Uses baseURL from config

// âŒ MISTAKE 3: Sharing state between tests
let userId: string;
test('create user', async ({ page }) => {
  userId = await createUser(); // Bad â€” another test depends on this
});
test('delete user', async ({ page }) => {
  await deleteUser(userId); // Breaks if run in isolation
});

// âœ… FIX: Each test is independent
test('delete user', async ({ page, request }) => {
  const user = await createUserViaApi(request); // Create own data
  await deleteUser(user.id);
});

// âŒ MISTAKE 4: Ignoring test.afterEach cleanup
test('create temp file', async ({ page }) => {
  // Creates resources but never cleans up
});

// âœ… FIX: Always clean up in afterEach
test.afterEach(async ({ request }) => {
  await request.delete(`/api/test-data/${testDataId}`);
});
```

## 19.5 Performance Tips

```typescript
// 1. Use API for setup (10x faster than UI)
test.beforeEach(async ({ request }) => {
  await request.post('/api/test/seed'); // Seed data via API
});

// 2. Reuse authentication state across tests
// playwright.config.ts
// globalSetup: './global-setup.ts',

// global-setup.ts
import { chromium } from '@playwright/test';

async function globalSetup() {
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto('/login');
  await page.getByLabel('Email').fill('admin@test.com');
  await page.getByLabel('Password').fill('Admin@123');
  await page.getByRole('button', { name: 'Login' }).click();

  // Save auth state to file
  await context.storageState({ path: 'auth/admin.json' });
  await browser.close();
}

export default globalSetup;
```

```typescript
// playwright.config.ts â€” reuse saved auth state
projects: [
  {
    name: 'Admin Tests',
    use: {
      storageState: 'auth/admin.json', // Already logged in!
    },
    testMatch: 'tests/admin/**/*.spec.ts',
  },
],
```

---

# Chapter 20: Interview Questions and Scenarios

## 20.1 Beginner Level Questions

**Q1: What is the difference between `page.click()` and `locator.click()`?**

> `page.click(selector)` is a legacy API that finds and clicks in one call. `locator.click()` is the modern approach â€” it creates a locator object first (lazy), applies auto-waiting, and supports chaining. Always prefer `locator.click()`.

**Q2: What is auto-waiting in Playwright?**

> Before performing any action (click, fill, etc.), Playwright automatically waits for the element to be: attached to DOM, visible, stable (not animating), enabled, and editable. This eliminates the need for `Thread.sleep()` or explicit waits in most cases.

**Q3: What is the difference between `fill()` and `pressSequentially()`?**

> `fill()` clears the field and sets the value in one atomic operation â€” fastest. `pressSequentially()` (formerly `type()`) simulates key-by-key typing with optional delay â€” useful for testing input validation that triggers on each keystroke.

**Q4: How do you handle a dropdown in Playwright?**

```typescript
// Native <select> element
await page.getByLabel('Country').selectOption({ label: 'India' });

// Custom dropdown (click-based)
await page.getByRole('combobox', { name: 'Category' }).click();
await page.getByRole('option', { name: 'Electronics' }).click();
```

**Q5: What is a BrowserContext?**

> A BrowserContext is like an incognito browser profile. Each context has isolated cookies, localStorage, and sessionStorage. Multiple contexts can share one browser instance. This enables true test isolation without launching new browsers.

---

## 20.2 Intermediate Level Questions

**Q6: How do you handle multiple browser tabs in Playwright?**

```typescript
const [newPage] = await Promise.all([
  context.waitForEvent('page'),
  page.getByRole('link', { name: 'Open in New Tab' }).click(),
]);
await newPage.waitForLoadState('domcontentloaded');
// Work with newPage
await newPage.close();
```

**Q7: How do you mock API responses in Playwright?**

```typescript
await page.route('/api/products', route =>
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([{ id: 1, name: 'Test Product' }]),
  })
);
```

**Q8: What is the Page Object Model (POM) and why use it?**

> POM organizes test code by creating a class for each page of the application. The class contains locators and action methods for that page. Benefits: reusability, maintainability (change UI â†’ update one class), readability, and separation of concerns.

**Q9: What is the difference between soft and hard assertions?**

> Hard assertions (default `expect()`) stop the test immediately on failure. Soft assertions (`expect.soft()`) continue running and collect all failures, reporting them together at the end. Use soft assertions for form validation tests where you want to verify all fields at once.

**Q10: How do you implement data-driven testing in Playwright?**

```typescript
const testCases = [
  { input: '', expected: 'Email is required' },
  { input: 'notanemail', expected: 'Invalid email format' },
  { input: 'valid@test.com', expected: '' },
];

for (const { input, expected } of testCases) {
  test(`email validation: "${input}"`, async ({ page }) => {
    await page.getByLabel('Email').fill(input);
    await page.getByRole('button', { name: 'Submit' }).click();
    if (expected) {
      await expect(page.getByText(expected)).toBeVisible();
    } else {
      await expect(page.getByRole('alert')).toBeHidden();
    }
  });
}
```

---

## 20.3 Advanced Level Questions

**Q11: How do you handle iframes in Playwright?**

```typescript
const frame = page.frameLocator('iframe[name="payment"]');
await frame.getByLabel('Card Number').fill('4111111111111111');
await frame.getByRole('button', { name: 'Pay' }).click();
```

**Q12: Explain Playwright's fixture system.**

> Fixtures are functions that provide pre-configured objects (page, browser, context, custom objects) to tests. They replace beforeEach/afterEach hooks with composable, reusable, scoped dependencies. Each fixture handles its own setup and teardown (using `await use(value)`).

**Q13: How do you set up authentication once and reuse it across tests?**

> Use `globalSetup` to log in once and save the browser state with `context.storageState({ path: 'auth/user.json' })`. Then configure `storageState` in `playwright.config.ts` so all tests in that project start already logged in.

**Q14: How do you run Playwright tests in parallel and avoid race conditions?**

> Enable `fullyParallel: true` for file-level parallelism. Use separate test data per test (no shared state). Use `test.describe.configure({ mode: 'serial' })` for tests that must run in order. Use API setup instead of UI setup to speed up and isolate test data.

**Q15: How do you integrate Playwright with GitHub Actions with sharding?**

```yaml
strategy:
  matrix:
    shard: [1, 2, 3, 4]
steps:
  - run: npx playwright test --shard=${{ matrix.shard }}/4
```

---

## 20.4 Real-World Scenario Questions

**Scenario 1: Your test keeps failing with "Timeout waiting for element". What do you do?**

```
1. Check if the locator is correct using Playwright Inspector (PWDEBUG=1)
2. Check if element is inside an iframe (use frameLocator)
3. Check if element is conditionally rendered (add waitFor)
4. Increase actionTimeout in config if the app is legitimately slow
5. Check network logs â€” is the API call failing?
6. Use page.pause() to freeze execution and inspect manually
```

**Scenario 2: How do you design a framework for 500+ tests?**

```
Structure:
â”œâ”€â”€ tests/
â”‚   â”œâ”€â”€ e2e/          â€” End-to-end flows
â”‚   â”œâ”€â”€ api/          â€” API tests
â”‚   â””â”€â”€ smoke/        â€” Critical path tests
â”œâ”€â”€ pages/            â€” Page Object Model
â”œâ”€â”€ fixtures/         â€” Shared fixtures
â”œâ”€â”€ utils/            â€” Helpers (faker, date utils, etc.)
â”œâ”€â”€ test-data/        â€” JSON data files
â”œâ”€â”€ auth/             â€” Saved auth states
â””â”€â”€ playwright.config.ts

Config strategy:
- Multiple playwright.config.ts (one per environment)
- Tags for smoke, regression, critical tests
- Sharding for CI/CD
- Separate projects for different user roles
```

**Scenario 3: How do you ensure test stability in a flaky environment?**

```typescript
// 1. Retry strategy
retries: process.env.CI ? 2 : 0

// 2. Proper waits (no hardcoded sleep)
await page.waitForLoadState('networkidle');

// 3. Mock unstable third-party APIs
await page.route('**/external-api.com/**', route =>
  route.fulfill({ status: 200, body: JSON.stringify(mockData) })
);

// 4. Increased timeouts for slow environments
test.setTimeout(120000);

// 5. Isolation â€” each test creates own data
```

**Scenario 4: Client asks "Why is our test suite taking 40 minutes to run?"**

```
Analysis steps:
1. Check number of workers â€” are tests running in parallel?
2. Check if using UI for setup (switch to API setup â€” 10x faster)
3. Check for waitForTimeout() calls â€” replace with event-based waits
4. Enable sharding â€” split 500 tests across 4 machines â†’ 10 min each
5. Identify slow tests with HTML report â€” optimize or move to smoke suite
6. Use storageState to skip UI login for every test
```

---

## 20.5 Quick Reference â€” Key Commands

```typescript
// Installation
// npm init playwright@latest
// npx playwright install

// Running Tests
// npx playwright test                          â€” all tests
// npx playwright test login.spec.ts            â€” specific file
// npx playwright test --headed                 â€” show browser
// npx playwright test --debug                  â€” debug mode
// npx playwright test --grep "login"           â€” by name
// npx playwright test --project=chromium       â€” specific browser
// npx playwright test --shard=1/4              â€” shard

// Reports
// npx playwright show-report                   â€” open HTML report
// npx playwright show-trace trace.zip          â€” open trace

// Code Generation (Record and Playback)
// npx playwright codegen https://example.com   â€” record test
```

---

## Appendix: Framework Architecture Blueprint

```
playwright-framework/
â”œâ”€â”€ playwright.config.ts              # Main config
â”œâ”€â”€ playwright.config.staging.ts      # Staging-specific config
â”œâ”€â”€ global-setup.ts                   # Login once, save auth state
â”œâ”€â”€ tsconfig.json
â”œâ”€â”€ package.json
â”‚
â”œâ”€â”€ auth/
â”‚   â”œâ”€â”€ admin.json                    # Saved admin auth state
â”‚   â””â”€â”€ user.json                     # Saved user auth state
â”‚
â”œâ”€â”€ pages/                            # Page Object Model
â”‚   â”œâ”€â”€ BasePage.ts                   # Common methods
â”‚   â”œâ”€â”€ LoginPage.ts
â”‚   â”œâ”€â”€ DashboardPage.ts
â”‚   â””â”€â”€ OrderPage.ts
â”‚
â”œâ”€â”€ fixtures/
â”‚   â”œâ”€â”€ auth-fixture.ts               # Auth fixtures
â”‚   â””â”€â”€ api-fixture.ts                # API fixtures
â”‚
â”œâ”€â”€ tests/
â”‚   â”œâ”€â”€ smoke/                        # Critical path (run on every PR)
â”‚   â”‚   â””â”€â”€ login.spec.ts
â”‚   â”œâ”€â”€ e2e/                          # Full flows
â”‚   â”‚   â”œâ”€â”€ order-flow.spec.ts
â”‚   â”‚   â””â”€â”€ user-management.spec.ts
â”‚   â””â”€â”€ api/                          # API tests
â”‚       â””â”€â”€ orders-api.spec.ts
â”‚
â”œâ”€â”€ utils/
â”‚   â”œâ”€â”€ api-helper.ts                 # API utility functions
â”‚   â”œâ”€â”€ date-helper.ts                # Date utilities
â”‚   â””â”€â”€ test-data-generator.ts        # Dynamic data
â”‚
â”œâ”€â”€ test-data/
â”‚   â”œâ”€â”€ users.json
â”‚   â””â”€â”€ products.json
â”‚
â”œâ”€â”€ reporters/
â”‚   â””â”€â”€ custom-reporter.ts
â”‚
â””â”€â”€ .github/
    â””â”€â”€ workflows/
        â””â”€â”€ playwright.yml            # CI/CD pipeline
```

---

*End of Playwright + TypeScript Complete Training Guide*
*Total Coverage: 20 Chapters | Beginner â†’ Expert Level*

---

# EXPANSION: Chapter 1 â€” Introduction (Deep Dive)

## 1.6 How Playwright Differs Architecturally from Selenium

### Selenium Architecture (Old Approach)
```
Test Code â†’ WebDriver API â†’ Browser Driver (chromedriver.exe) â†’ Browser
```
- Every command makes an **HTTP request** over the network (even locally)
- Each action = one round-trip = slow
- Browser drivers must be maintained separately and version-matched
- No built-in waiting â€” developers must add explicit waits everywhere

### Playwright Architecture (Modern Approach)
```
Test Code â†’ Playwright API â†’ WebSocket (persistent connection) â†’ Browser
```
- Uses a **single persistent WebSocket connection**
- Commands are sent as JSON messages â€” much faster
- Browsers bundled with Playwright â€” no driver management
- Built-in auto-waiting baked into every action

**Real-world impact:**
A Selenium test suite that takes 2 hours can often be reduced to 30 minutes with Playwright because:
1. No driver startup overhead
2. Auto-waiting eliminates unnecessary `Thread.sleep()` calls
3. Native parallel execution
4. API setup replaces slow UI setup

---

## 1.7 Playwright's Event-Driven Model

Playwright uses an **event-driven** approach. You can listen to browser events:

```typescript
import { test } from '@playwright/test';

test('event listeners', async ({ page }) => {
  // Listen for console messages from the browser
  page.on('console', (msg) => {
    console.log(`[Browser Console] ${msg.type()}: ${msg.text()}`);
  });

  // Listen for page errors (JavaScript errors in the browser)
  page.on('pageerror', (error) => {
    console.error(`[Page Error] ${error.message}`);
    console.error(error.stack);
  });

  // Listen for all network requests
  page.on('request', (request) => {
    console.log(`>> ${request.method()} ${request.url()}`);
  });

  // Listen for all network responses
  page.on('response', (response) => {
    console.log(`<< ${response.status()} ${response.url()}`);
  });

  // Listen for file downloads
  page.on('download', (download) => {
    console.log(`Download started: ${download.suggestedFilename()}`);
  });

  // Listen for new dialogs (alerts, confirms, prompts)
  page.on('dialog', async (dialog) => {
    console.log(`Dialog: ${dialog.type()} - ${dialog.message()}`);
    await dialog.accept();
  });

  // Listen for new pages/tabs being opened
  page.on('popup', async (popup) => {
    console.log(`Popup opened: ${popup.url()}`);
    await popup.waitForLoadState();
  });

  await page.goto('https://example.com');
});
```

---

## 1.8 Playwright vs Cypress â€” Detailed Comparison

| Aspect | Playwright | Cypress |
|---|---|---|
| **Multi-browser** | Chrome, Firefox, Safari | Chrome, Edge (limited Firefox) |
| **Multi-tab** | âœ… Full support | âŒ Not supported |
| **iFrames** | âœ… Native `frameLocator()` | âš ï¸ Complex workarounds |
| **Shadow DOM** | âœ… Native | âš ï¸ Limited |
| **API Testing** | âœ… Built-in `request` context | âœ… `cy.request()` |
| **File Upload** | âœ… `setInputFiles()` | âš ï¸ Workarounds needed |
| **Network Mocking** | âœ… `page.route()` | âœ… `cy.intercept()` |
| **Real device testing** | âœ… Device emulation | âŒ |
| **Programming languages** | TS, JS, Python, Java, C# | JS/TS only |
| **Running in same process as app** | âŒ (separate process) | âœ… (same origin) |
| **Time travel debugging** | âœ… Trace Viewer | âœ… Cypress Dashboard (paid) |
| **Pricing** | Free & Open Source | Free + Paid Cloud |
| **Learning curve** | Medium | Easy |
| **Best for** | Enterprise, complex scenarios | Simple web apps, beginners |

**When to choose Playwright:**
- Multi-browser testing required (especially Safari/WebKit)
- Multi-tab scenarios
- Complex authentication flows
- API + UI combined testing
- Enterprise-scale frameworks
- Python/Java/.NET teams

**When to choose Cypress:**
- Quick prototype automation
- Simple single-page apps
- Team already familiar with Cypress

---

# EXPANSION: Chapter 2 â€” Setup (Deep Dive)

## 2.6 Understanding package.json Scripts

After `npm init playwright@latest`, your `package.json` will have:

```json
{
  "name": "my-playwright-project",
  "version": "1.0.0",
  "scripts": {
    "test": "npx playwright test",
    "test:headed": "npx playwright test --headed",
    "test:debug": "npx playwright test --debug",
    "test:report": "npx playwright show-report",
    "test:chrome": "npx playwright test --project=chromium",
    "test:firefox": "npx playwright test --project=firefox",
    "test:safari": "npx playwright test --project=webkit",
    "test:smoke": "npx playwright test --grep @smoke",
    "test:regression": "npx playwright test --grep @regression",
    "codegen": "npx playwright codegen"
  },
  "devDependencies": {
    "@playwright/test": "^1.44.0",
    "@types/node": "^20.0.0",
    "typescript": "^5.0.0",
    "dotenv": "^16.0.0",
    "@faker-js/faker": "^8.0.0"
  }
}
```

Run scripts with:
```
npm test                  â†’ runs all tests
npm run test:headed       â†’ visible browser
npm run test:smoke        â†’ only @smoke tagged tests
```

---

## 2.7 Full playwright.config.ts for Enterprise Projects

```typescript
import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

// Load environment-specific .env file
const ENV = process.env.TEST_ENV || 'staging';
dotenv.config({ path: path.resolve(__dirname, `.env.${ENV}`) });

export default defineConfig({
  // â”€â”€â”€ Test Discovery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  testDir: './tests',
  testMatch: '**/*.spec.ts',          // Only files ending in .spec.ts
  testIgnore: '**/skip/**',           // Ignore this folder

  // â”€â”€â”€ Execution â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  fullyParallel: true,                // All tests run in parallel
  forbidOnly: !!process.env.CI,       // Fail if test.only() is left in code
  retries: process.env.CI ? 2 : 0,   // Retry on CI only
  workers: process.env.CI ? 4 : undefined,

  // â”€â”€â”€ Global Timeout â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  timeout: 60_000,                    // 60 seconds per test
  expect: {
    timeout: 10_000,                  // 10 seconds for assertions
    toHaveScreenshot: {
      maxDiffPixels: 100,             // Allow 100 pixel differences (anti-aliasing)
    },
  },

  // â”€â”€â”€ Reporting â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  reporter: process.env.CI
    ? [['junit', { outputFile: 'results/junit.xml' }], ['html', { open: 'never' }]]
    : [['html', { open: 'on-failure' }], ['list']],

  // â”€â”€â”€ Shared Browser Settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  use: {
    baseURL: process.env.BASE_URL,
    headless: !!process.env.CI,

    // Timeouts
    actionTimeout: 30_000,
    navigationTimeout: 30_000,

    // Evidence collection
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',

    // Browser settings
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,           // Useful for staging environments
    locale: 'en-IN',
    timezoneId: 'Asia/Kolkata',

    // Extra HTTP headers (e.g., bypass WAF in test environments)
    extraHTTPHeaders: {
      'x-test-automation': 'playwright',
    },
  },

  // â”€â”€â”€ Projects (Browsers/Devices) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  projects: [
    // â”€â”€ Setup project (runs once before all tests) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    {
      name: 'setup',
      testMatch: '**/global.setup.ts',
    },

    // â”€â”€ Desktop browsers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup'],         // Run setup first
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
      dependencies: ['setup'],
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
      dependencies: ['setup'],
    },

    // â”€â”€ Mobile devices â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 7'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 14 Pro'] },
    },

    // â”€â”€ API tests (no browser needed) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    {
      name: 'api',
      testMatch: '**/api/**/*.spec.ts',
      use: { baseURL: process.env.API_BASE_URL },
    },
  ],

  // â”€â”€â”€ Output directories â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  outputDir: 'test-results/',

  // â”€â”€â”€ Global setup/teardown â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  globalSetup: './tests/global.setup.ts',
  globalTeardown: './tests/global.teardown.ts',
});
```

---

## 2.8 Global Setup and Teardown

```typescript
// tests/global.setup.ts
import { chromium, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig) {
  console.log('\nðŸš€ Global Setup: Creating authentication states...');

  const browser = await chromium.launch();

  // â”€â”€ Create Admin auth state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const adminContext = await browser.newContext();
  const adminPage = await adminContext.newPage();

  await adminPage.goto(`${process.env.BASE_URL}/login`);
  await adminPage.getByLabel('Email').fill(process.env.ADMIN_EMAIL!);
  await adminPage.getByLabel('Password').fill(process.env.ADMIN_PASSWORD!);
  await adminPage.getByRole('button', { name: 'Sign In' }).click();
  await adminPage.waitForURL('**/admin/dashboard');

  // Save admin auth state
  await adminContext.storageState({ path: './auth/admin.json' });
  console.log('âœ… Admin auth state saved');

  // â”€â”€ Create Regular User auth state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const userContext = await browser.newContext();
  const userPage = await userContext.newPage();

  await userPage.goto(`${process.env.BASE_URL}/login`);
  await userPage.getByLabel('Email').fill(process.env.USER_EMAIL!);
  await userPage.getByLabel('Password').fill(process.env.USER_PASSWORD!);
  await userPage.getByRole('button', { name: 'Sign In' }).click();
  await userPage.waitForURL('**/dashboard');

  await userContext.storageState({ path: './auth/user.json' });
  console.log('âœ… User auth state saved');

  await browser.close();
  console.log('ðŸ Global Setup complete\n');
}

export default globalSetup;
```

```typescript
// tests/global.teardown.ts
async function globalTeardown() {
  console.log('\nðŸ§¹ Global Teardown: Cleaning up test data...');
  // Delete test users, orders, etc. created during tests
  // await cleanupTestDatabase();
  console.log('âœ… Teardown complete\n');
}

export default globalTeardown;
```

---

# EXPANSION: Chapter 3 â€” TypeScript (Deep Dive)

## 3.5 Advanced TypeScript Patterns for Playwright

### Generic Functions

```typescript
// A generic function that works with any type
async function getElementText<T extends string>(
  page: import('@playwright/test').Page,
  locator: string
): Promise<T> {
  const text = await page.locator(locator).textContent();
  return (text?.trim() ?? '') as T;
}

// Usage
const heading = await getElementText<string>(page, 'h1');
```

### Union Types and Type Guards

```typescript
type UserRole = 'admin' | 'manager' | 'user' | 'viewer';

interface LoginCredentials {
  email: string;
  password: string;
  role: UserRole;
  expectedUrl: string;
  permissions: string[];
}

// Type guard function
function isAdmin(creds: LoginCredentials): boolean {
  return creds.role === 'admin';
}

const credentials: LoginCredentials[] = [
  {
    email: 'admin@test.com',
    password: 'Admin@123',
    role: 'admin',
    expectedUrl: '/admin/dashboard',
    permissions: ['create', 'read', 'update', 'delete'],
  },
  {
    email: 'viewer@test.com',
    password: 'Viewer@123',
    role: 'viewer',
    expectedUrl: '/dashboard',
    permissions: ['read'],
  },
];
```

### Enums for Test Configuration

```typescript
enum TestEnvironment {
  LOCAL = 'http://localhost:3000',
  DEV = 'https://dev.example.com',
  STAGING = 'https://staging.example.com',
  PRODUCTION = 'https://example.com',
}

enum BrowserType {
  CHROME = 'chromium',
  FIREFOX = 'firefox',
  SAFARI = 'webkit',
}

enum Timeout {
  SHORT = 5_000,
  MEDIUM = 15_000,
  LONG = 30_000,
  EXTENDED = 60_000,
  CI = 120_000,
}

// Usage in tests
test('use typed constants', async ({ page }) => {
  await page.goto(TestEnvironment.STAGING);
  await page.waitForLoadState('networkidle');
  await page.getByRole('button').click({ timeout: Timeout.MEDIUM });
});
```

### Interfaces for Page Objects

```typescript
// Define interface first â€” describes contract
interface ILoginPage {
  navigate(): Promise<void>;
  login(email: string, password: string): Promise<void>;
  loginAsAdmin(): Promise<void>;
  loginAsUser(): Promise<void>;
  expectError(message: string): Promise<void>;
  expectSuccessfulRedirect(url: string): Promise<void>;
}

// Implement the interface
class LoginPage implements ILoginPage {
  constructor(private readonly page: import('@playwright/test').Page) {}

  async navigate(): Promise<void> {
    await this.page.goto('/login');
    await this.page.waitForLoadState('domcontentloaded');
  }

  async login(email: string, password: string): Promise<void> {
    await this.page.getByLabel('Email').fill(email);
    await this.page.getByLabel('Password').fill(password);
    await this.page.getByRole('button', { name: 'Sign In' }).click();
  }

  async loginAsAdmin(): Promise<void> {
    await this.login(process.env.ADMIN_EMAIL!, process.env.ADMIN_PASSWORD!);
  }

  async loginAsUser(): Promise<void> {
    await this.login(process.env.USER_EMAIL!, process.env.USER_PASSWORD!);
  }

  async expectError(message: string): Promise<void> {
    await expect(this.page.getByRole('alert')).toHaveText(message);
  }

  async expectSuccessfulRedirect(url: string): Promise<void> {
    await expect(this.page).toHaveURL(url);
  }
}
```

### Optional Chaining and Nullish Coalescing

```typescript
test('handling null and undefined safely', async ({ page }) => {
  await page.goto('https://example.com');

  // Optional chaining (?.) â€” safe property access
  const text = await page.getByRole('heading').textContent();
  const trimmed = text?.trim()?.toLowerCase(); // No crash if text is null

  // Nullish coalescing (??) â€” default values
  const title = await page.title() ?? 'No Title';
  const count = await page.getByRole('listitem').count() ?? 0;

  // Combining both
  const headerText = (await page.getByRole('banner').textContent())?.trim() ?? 'Default Header';
  console.log(headerText);
});
```

---

## 3.6 Async/Await â€” Common Mistakes and Fixes

```typescript
// âŒ MISTAKE 1: Forgetting await
test('forgot await', async ({ page }) => {
  page.goto('https://example.com');  // Returns Promise â€” not awaited!
  page.getByRole('button').click();  // Executes before navigation completes
  // Test passes but nothing actually ran correctly
});

// âœ… FIX: Always await Playwright actions
test('correct await usage', async ({ page }) => {
  await page.goto('https://example.com');
  await page.getByRole('button').click();
});

// âŒ MISTAKE 2: await inside forEach (doesn't work with async)
test('wrong async forEach', async ({ page }) => {
  const items = ['item1', 'item2', 'item3'];
  items.forEach(async (item) => {
    await page.getByText(item).click(); // These run in parallel unexpectedly!
  });
});

// âœ… FIX: Use for...of for sequential async
test('correct sequential async', async ({ page }) => {
  const items = ['item1', 'item2', 'item3'];
  for (const item of items) {
    await page.getByText(item).click();
  }
});

// âœ… FIX: Use Promise.all for parallel async
test('parallel async with Promise.all', async ({ page }) => {
  const items = ['item1', 'item2', 'item3'];
  await Promise.all(items.map(item => page.getByText(item).isVisible()));
});

// âŒ MISTAKE 3: Not handling Promise rejections
test('unhandled rejection', async ({ page }) => {
  try {
    await page.goto('https://invalid-url-that-fails.com');
  } catch (error) {
    // âŒ Swallowing the error â€” test passes even when it should fail
    console.log('Failed to navigate');
  }
});

// âœ… FIX: Let errors propagate or re-throw
test('proper error handling', async ({ page }) => {
  try {
    await page.goto('https://example.com', { timeout: 5000 });
  } catch (error) {
    throw new Error(`Navigation failed: ${error}`); // Re-throw with context
  }
});
```

---

# EXPANSION: Chapter 4 â€” First Test (Deep Dive)

## 4.5 Test Annotations and Tags

```typescript
import { test, expect } from '@playwright/test';

// Mark a test to skip
test.skip('this feature is not ready yet', async ({ page }) => {
  // Will not run
});

// Mark a test as expected to fail (passes the build if it fails)
test.fail('known bug â€” will be fixed in v2.1', async ({ page }) => {
  await page.goto('/broken-page');
  await expect(page.getByRole('heading')).toHaveText('Working Page'); // Fails expectedly
});

// Mark a test to run only (useful during development â€” DO NOT commit)
test.only('debug this specific test', async ({ page }) => {
  // Only this test runs in the file
});

// Add custom annotations for reports and filtering
test('login with 2FA @smoke @critical', async ({ page }) => {
  test.info().annotations.push({ type: 'jira', description: 'AUTH-123' });
  test.info().annotations.push({ type: 'owner', description: 'QA Team' });
  // ...
});

// Skip on specific browser
test('clipboard test â€” not supported in Firefox', async ({ page, browserName }) => {
  test.skip(browserName === 'firefox', 'Clipboard API not available in Firefox');
  await page.getByRole('button', { name: 'Copy' }).click();
  // ...
});

// Conditionally slow test down
test('slow network test', async ({ page }) => {
  // Simulate slow 3G connection
  await page.context().setOffline(false);
  // ...
});
```

## 4.6 test.step() â€” Structured Test Steps

```typescript
test('checkout flow with steps', async ({ page }) => {
  await test.step('Navigate to shop', async () => {
    await page.goto('/shop');
    await expect(page.getByRole('heading', { name: 'Products' })).toBeVisible();
  });

  await test.step('Add item to cart', async () => {
    await page.getByText('Playwright Book').getByRole('button', { name: 'Add to Cart' }).click();
    await expect(page.getByTestId('cart-count')).toHaveText('1');
  });

  await test.step('Proceed to checkout', async () => {
    await page.getByRole('link', { name: 'Checkout' }).click();
    await page.waitForURL('**/checkout');
  });

  await test.step('Fill shipping details', async () => {
    await page.getByLabel('Full Name').fill('John Doe');
    await page.getByLabel('Address').fill('123 Main St');
    await page.getByLabel('City').fill('Mumbai');
    await page.getByLabel('PIN Code').fill('400001');
  });

  await test.step('Confirm order', async () => {
    await page.getByRole('button', { name: 'Place Order' }).click();
    await expect(page.getByText('Order confirmed!')).toBeVisible();
  });
});
```

> `test.step()` shows in the HTML report as expandable steps â€” great for readability when tests fail.

## 4.7 Parametrize Tests with test.describe.each (custom)

```typescript
// Data-driven test using describe
const loginScenarios = [
  { desc: 'valid admin', email: 'admin@test.com', password: 'Admin@123', url: '/admin' },
  { desc: 'valid user', email: 'user@test.com', password: 'User@123', url: '/dashboard' },
  { desc: 'empty email', email: '', password: 'Admin@123', error: 'Email is required' },
  { desc: 'wrong password', email: 'admin@test.com', password: 'wrong', error: 'Invalid credentials' },
];

for (const scenario of loginScenarios) {
  test(`login: ${scenario.desc}`, async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(scenario.email);
    await page.getByLabel('Password').fill(scenario.password);
    await page.getByRole('button', { name: 'Sign In' }).click();

    if (scenario.url) {
      await expect(page).toHaveURL(scenario.url);
    } else if (scenario.error) {
      await expect(page.getByRole('alert')).toHaveText(scenario.error);
    }
  });
}
```

---

# EXPANSION: Chapter 5 â€” Locators (Deep Dive)

## 5.11 Codegen â€” Auto-Generate Locators

Playwright's codegen tool records your browser interactions and generates test code:

```
npx playwright codegen https://example.com
```

This opens two windows:
1. A real browser â€” interact with it normally
2. Playwright Inspector â€” shows generated TypeScript code in real time

**Tips for using codegen effectively:**
- Use the locator picker (target icon) to inspect elements
- Codegen prefers `getByRole`, `getByLabel`, `getByText` â€” the best locators
- Copy generated locators into your Page Object Model
- Do NOT just copy-paste the entire generated test â€” refactor it properly

---

## 5.12 Locator Chaining â€” Real-World Examples

```typescript
import { test, expect } from '@playwright/test';

test('advanced locator chaining', async ({ page }) => {
  await page.goto('https://example.com/orders');

  // â”€â”€ Scenario 1: Find a button inside a specific table row â”€â”€â”€â”€â”€â”€â”€â”€
  // Find the row containing "Order #1042" and click its "View" button
  await page.getByRole('row', { name: /Order #1042/ })
    .getByRole('button', { name: 'View' })
    .click();

  await page.goBack();

  // â”€â”€ Scenario 2: Find an item inside a named section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const sidebar = page.getByRole('complementary'); // <aside> element
  await sidebar.getByRole('link', { name: 'Recent Orders' }).click();

  // â”€â”€ Scenario 3: Filter list items by status â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  // Click the first "Pending" order from a list
  await page.getByRole('listitem')
    .filter({ has: page.getByText('Pending') })
    .first()
    .getByRole('button', { name: 'Process' })
    .click();

  // â”€â”€ Scenario 4: Find in a dialog/modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const dialog = page.getByRole('dialog', { name: 'Confirm Deletion' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Yes, Delete' }).click();

  // â”€â”€ Scenario 5: Find in a form group by label â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  // Multiple forms on same page â€” scope to the right one
  const shippingSection = page.locator('section', { hasText: 'Shipping Address' });
  await shippingSection.getByLabel('City').fill('Mumbai');
  await shippingSection.getByLabel('State').selectOption('MH');
});
```

---

## 5.13 Locating Elements in Complex UI Patterns

### Data Tables

```typescript
test('interacting with data tables', async ({ page }) => {
  await page.goto('https://example.com/admin/users');

  // Get all rows (excluding header)
  const rows = page.locator('table tbody tr');
  const count = await rows.count();
  console.log(`Total users: ${count}`);

  // Read data from a specific cell
  const firstRowName = await rows.nth(0).locator('td').nth(1).textContent();
  console.log(`First user: ${firstRowName}`);

  // Find row by cell content and act on it
  const targetRow = page.locator('tr').filter({ hasText: 'jane@example.com' });
  await targetRow.getByRole('button', { name: 'Edit' }).click();

  // Verify all rows have a status badge
  const statusBadges = page.locator('td .status-badge');
  await expect(statusBadges).toHaveCount(count);

  // Sort by column
  await page.getByRole('columnheader', { name: 'Name' }).click();

  // Verify first row after sort
  const sortedFirstRow = await rows.nth(0).locator('td').nth(1).textContent();
  console.log(`After sort: ${sortedFirstRow}`);
});
```

### Accordion and Expandable Sections

```typescript
test('accordion interactions', async ({ page }) => {
  await page.goto('https://example.com/faq');

  // Click to expand an accordion panel
  const faqItem = page.locator('.accordion-item').filter({ hasText: 'What is Playwright?' });
  const trigger = faqItem.getByRole('button');
  const panel = faqItem.locator('.accordion-panel');

  // Verify initially collapsed
  await expect(panel).toBeHidden();

  // Click to expand
  await trigger.click();
  await expect(panel).toBeVisible();
  await expect(panel).toContainText('Playwright is a testing framework');

  // Click again to collapse
  await trigger.click();
  await expect(panel).toBeHidden();
});
```

### Tabs

```typescript
test('tab navigation', async ({ page }) => {
  await page.goto('https://example.com/settings');

  // Click a tab
  await page.getByRole('tab', { name: 'Security' }).click();

  // Verify tab is selected
  await expect(page.getByRole('tab', { name: 'Security' })).toHaveAttribute('aria-selected', 'true');

  // Verify correct panel is shown
  await expect(page.getByRole('tabpanel', { name: 'Security' })).toBeVisible();
  await expect(page.getByLabel('Change Password')).toBeVisible();

  // Navigate to another tab
  await page.getByRole('tab', { name: 'Notifications' }).click();
  await expect(page.getByRole('tabpanel', { name: 'Notifications' })).toBeVisible();
});
```

### Autocomplete / Search Suggestions

```typescript
test('autocomplete field', async ({ page }) => {
  await page.goto('https://example.com/search');

  // Type in search box â€” triggers autocomplete
  await page.getByRole('combobox', { name: 'Search products' }).fill('Play');

  // Wait for dropdown suggestions to appear
  await page.getByRole('listbox').waitFor({ state: 'visible' });

  // Verify suggestions are shown
  const suggestions = page.getByRole('option');
  await expect(suggestions.first()).toBeVisible();
  const count = await suggestions.count();
  expect(count).toBeGreaterThan(0);

  // Select a suggestion
  await page.getByRole('option', { name: /Playwright/ }).click();

  // Verify selection updated the input
  await expect(page.getByRole('combobox', { name: 'Search products' }))
    .toHaveValue(/Playwright/);
});
```

---

## 5.14 Shadow DOM

Some modern web components use Shadow DOM â€” elements encapsulated inside a shadow root. Playwright handles them natively:

```typescript
test('shadow DOM elements', async ({ page }) => {
  await page.goto('https://example.com/web-components');

  // Playwright automatically pierces Shadow DOM
  // No special handling needed in most cases
  await page.locator('my-custom-input').getByLabel('Username').fill('admin');

  // If you need to explicitly access shadow root
  const host = page.locator('my-component');
  // Playwright CSS selectors auto-pierce shadow DOM
  await page.locator('my-component >> input[name="email"]').fill('user@test.com');
});
```

---

# EXPANSION: Chapter 6 â€” UI Elements (Deep Dive)

## 6.7 Drag and Drop

```typescript
test('drag and drop interactions', async ({ page }) => {
  await page.goto('https://example.com/kanban');

  // Method 1: Using dragTo() â€” easiest
  const taskCard = page.getByText('Fix login bug');
  const targetColumn = page.locator('.column-in-progress');
  await taskCard.dragTo(targetColumn);

  // Method 2: Using mouse events â€” more control
  const source = page.locator('.draggable-item').nth(0);
  const target = page.locator('.drop-zone').nth(2);

  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();

  if (sourceBox && targetBox) {
    await page.mouse.move(
      sourceBox.x + sourceBox.width / 2,
      sourceBox.y + sourceBox.height / 2
    );
    await page.mouse.down();
    await page.mouse.move(
      targetBox.x + targetBox.width / 2,
      targetBox.y + targetBox.height / 2,
      { steps: 10 } // Smooth movement in 10 steps
    );
    await page.mouse.up();
  }

  // Verify item moved
  await expect(targetColumn.getByText('Fix login bug')).toBeVisible();
});
```

## 6.8 Date Pickers

```typescript
test('date picker interaction', async ({ page }) => {
  await page.goto('https://example.com/booking');

  // Simple approach â€” fill directly if it's a text input
  await page.getByLabel('Check-in Date').fill('2025-12-25');
  await page.getByLabel('Check-in Date').press('Enter');

  // For complex calendar date pickers:
  await page.getByLabel('Departure Date').click(); // Open calendar

  // Navigate to December 2025
  while (!(await page.getByRole('heading', { name: /December 2025/ }).isVisible())) {
    await page.getByRole('button', { name: 'Next month' }).click();
  }

  // Click on day 25
  await page.getByRole('button', { name: '25' }).click();

  // Verify selected date is shown
  await expect(page.getByLabel('Departure Date')).toHaveValue('12/25/2025');
});
```

## 6.9 Rich Text Editors (Quill, TinyMCE)

```typescript
test('rich text editor', async ({ page }) => {
  await page.goto('https://example.com/blog/new');

  // Quill editor â€” find the editable div
  const editor = page.locator('.ql-editor');
  await editor.click();
  await editor.fill('This is my blog post content');

  // Type and apply formatting
  await editor.selectText();
  await page.getByRole('button', { name: 'Bold' }).click();

  // TinyMCE â€” uses iframe
  const tinyFrame = page.frameLocator('#tinymce-editor_ifr');
  const body = tinyFrame.locator('body');
  await body.click();
  await body.fill('TinyMCE content here');

  // ContentEditable elements
  const contentEditable = page.locator('[contenteditable="true"]');
  await contentEditable.click();
  await contentEditable.pressSequentially('Hello World');
});
```

## 6.10 Keyboard Interactions

```typescript
test('keyboard shortcuts and navigation', async ({ page }) => {
  await page.goto('https://example.com/editor');

  // Keyboard shortcuts
  await page.keyboard.press('Control+a');      // Select all
  await page.keyboard.press('Control+c');      // Copy
  await page.keyboard.press('Control+v');      // Paste
  await page.keyboard.press('Control+z');      // Undo
  await page.keyboard.press('Control+y');      // Redo
  await page.keyboard.press('Escape');          // Close modal/dialog
  await page.keyboard.press('F5');              // Refresh (rare in tests)

  // Tab navigation through form fields
  await page.getByLabel('First Name').click();
  await page.keyboard.press('Tab');  // Move to next field
  await page.keyboard.press('Tab');  // Move to next field
  await page.keyboard.press('Shift+Tab'); // Move back

  // Type special characters
  await page.getByLabel('Search').fill('');
  await page.keyboard.type('hello@world.com', { delay: 50 });

  // Hold modifier keys
  await page.keyboard.down('Shift');
  await page.getByRole('link', { name: 'Item 1' }).click();  // Open in new tab
  await page.keyboard.up('Shift');
});
```

## 6.11 Hover Interactions and Tooltips

```typescript
test('hover interactions', async ({ page }) => {
  await page.goto('https://example.com/products');

  // Hover over an element
  const productCard = page.getByRole('article').first();
  await productCard.hover();

  // Verify tooltip appears
  await expect(page.getByRole('tooltip')).toBeVisible();
  await expect(page.getByRole('tooltip')).toContainText('Quick view');

  // Click a button that only appears on hover
  await page.getByRole('button', { name: 'Quick Add' }).click();

  // Move mouse away
  await page.mouse.move(0, 0);
  await expect(page.getByRole('tooltip')).toBeHidden();

  // Hover with specific position
  await page.getByRole('article').nth(2).hover({ position: { x: 10, y: 10 } });
});
```

## 6.12 Handling Dynamic Content

```typescript
test('dynamic content patterns', async ({ page }) => {
  await page.goto('https://example.com/dashboard');

  // â”€â”€ Infinite scroll â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  // Scroll to bottom to load more items
  let previousCount = 0;
  let currentCount = await page.getByRole('listitem').count();

  while (currentCount > previousCount) {
    previousCount = currentCount;
    // Scroll to bottom of page
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(1000); // Brief pause for new items to load
    currentCount = await page.getByRole('listitem').count();
  }
  console.log(`Total items after scroll: ${currentCount}`);

  // â”€â”€ Loading skeleton â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  await page.getByRole('button', { name: 'Refresh Data' }).click();

  // Wait for skeleton loader to appear and disappear
  await page.locator('.skeleton-loader').waitFor({ state: 'visible' });
  await page.locator('.skeleton-loader').waitFor({ state: 'hidden', timeout: 10000 });

  // Now real content is visible
  await expect(page.getByRole('table')).toBeVisible();

  // â”€â”€ Lazy-loaded images â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const image = page.getByRole('img', { name: 'Product photo' });
  await image.scrollIntoViewIfNeeded();
  await expect(image).toHaveAttribute('src', /^https?:\/\//); // Has real src (not placeholder)
});
```

## 6.13 File Downloads

```typescript
import path from 'path';
import fs from 'fs';

test('file download handling', async ({ page }) => {
  await page.goto('https://example.com/reports');

  // Wait for download event AND trigger it simultaneously
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Export CSV' }).click(),
  ]);

  // Get download info
  console.log('Filename:', download.suggestedFilename());
  console.log('Download URL:', download.url());

  // Save to specific path
  const downloadPath = path.join('./downloads', download.suggestedFilename());
  await download.saveAs(downloadPath);

  // Verify file was downloaded
  expect(fs.existsSync(downloadPath)).toBeTruthy();

  // Check file size
  const stats = fs.statSync(downloadPath);
  expect(stats.size).toBeGreaterThan(0);

  // Verify CSV content
  const content = fs.readFileSync(downloadPath, 'utf-8');
  expect(content).toContain('Order ID');
  expect(content).toContain('Customer Name');

  // Cleanup
  fs.unlinkSync(downloadPath);
});
```

## 6.14 Multi-select and Complex Form Patterns

```typescript
test('complex form interactions', async ({ page }) => {
  await page.goto('https://example.com/profile/settings');

  // â”€â”€ Range slider â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const slider = page.getByRole('slider', { name: 'Volume' });
  await slider.fill('75'); // Set to 75%

  // â”€â”€ Color picker â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  await page.getByLabel('Theme Color').fill('#FF6B35');

  // â”€â”€ Chip/tag multi-select â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const skillsInput = page.getByLabel('Skills');
  await skillsInput.fill('TypeScript');
  await page.getByRole('option', { name: 'TypeScript' }).click();
  await skillsInput.fill('Playwright');
  await page.getByRole('option', { name: 'Playwright' }).click();

  // Verify chips were added
  await expect(page.locator('.chip', { hasText: 'TypeScript' })).toBeVisible();
  await expect(page.locator('.chip', { hasText: 'Playwright' })).toBeVisible();

  // Remove a chip
  await page.locator('.chip', { hasText: 'TypeScript' }).getByLabel('Remove').click();
  await expect(page.locator('.chip', { hasText: 'TypeScript' })).toBeHidden();

  // â”€â”€ Star rating â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  // Click the 4th star (1-indexed)
  await page.locator('.star-rating .star').nth(3).click();
  await expect(page.locator('.star-rating')).toHaveAttribute('data-rating', '4');

  // â”€â”€ Toggle switch â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const darkModeToggle = page.getByRole('switch', { name: 'Dark Mode' });
  const initialState = await darkModeToggle.isChecked();
  await darkModeToggle.click();
  await expect(darkModeToggle).toBeChecked({ checked: !initialState });
});
```

---

# EXPANSION: Chapter 7 â€” Wait Mechanisms (Deep Dive)

## 7.8 Understanding Playwright's Actionability Checks

Before Playwright performs any action, it checks these conditions in order:

```
1. ATTACHED    â†’ Element exists in the DOM
2. VISIBLE     â†’ Element is not hidden (displayâ‰ none, visibilityâ‰ hidden, opacity>0)
3. STABLE      â†’ Element is not animating (position unchanged for 2 frames)
4. ENABLED     â†’ Element is not disabled (no disabled attribute)
5. EDITABLE    â†’ Element can accept input (only for fill/type actions)
6. RECEIVES EVENTS â†’ No other element covers it (only for click)
```

```typescript
test('actionability check examples', async ({ page }) => {
  await page.goto('https://example.com');

  // Playwright automatically retries until all checks pass (up to timeout)
  // You do NOT need to manually wait for each condition

  // If button is inside a loading state, Playwright waits until:
  // - It appears (attached)
  // - It becomes visible
  // - Its animation completes (stable)
  // - The disabled attribute is removed
  await page.getByRole('button', { name: 'Submit' }).click();

  // Force skips actionability checks (use only when you know what you're doing)
  await page.getByRole('button', { name: 'Hidden Trigger' }).click({ force: true });

  // Timeout can be set per action
  await page.getByRole('button', { name: 'Slow Button' }).click({ timeout: 60000 });
});
```

## 7.9 waitForEvent â€” Waiting for Browser Events

```typescript
test('waitForEvent patterns', async ({ page, context }) => {
  await page.goto('https://example.com');

  // Wait for a download to start
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Download Report' }).click(),
  ]);
  await download.saveAs('./downloads/report.pdf');

  // Wait for a new page/tab
  const [newPage] = await Promise.all([
    context.waitForEvent('page'),
    page.getByRole('link', { name: 'Open in New Tab' }).click(),
  ]);
  await newPage.waitForLoadState();
  await newPage.close();

  // Wait for a file chooser dialog
  const [fileChooser] = await Promise.all([
    page.waitForEvent('filechooser'),
    page.getByRole('button', { name: 'Choose File' }).click(),
  ]);
  await fileChooser.setFiles('./test-data/sample.pdf');

  // Wait for a dialog (alert/confirm/prompt)
  const [dialog] = await Promise.all([
    page.waitForEvent('dialog'),
    page.getByRole('button', { name: 'Delete' }).click(),
  ]);
  expect(dialog.message()).toContain('Are you sure');
  await dialog.accept();
});
```

## 7.10 Network Wait Patterns

```typescript
test('network-based waiting', async ({ page }) => {
  await page.goto('https://example.com/dashboard');

  // Wait for a specific API request
  const searchRequest = page.waitForRequest(
    req => req.url().includes('/api/search') && req.method() === 'POST'
  );
  await page.getByPlaceholder('Search...').fill('playwright');
  await page.keyboard.press('Enter');
  const req = await searchRequest;
  console.log('Search payload:', req.postData());

  // Wait for a specific response
  const searchResponse = page.waitForResponse(
    resp => resp.url().includes('/api/search') && resp.status() === 200
  );
  const resp = await searchResponse;
  const data = await resp.json();
  expect(data.results).toBeDefined();

  // Wait for all network activity to settle
  await page.waitForLoadState('networkidle');

  // Wait for no pending XHR/fetch requests
  await page.waitForFunction(() => {
    // Custom check â€” all loading spinners are gone
    return document.querySelectorAll('.loading').length === 0;
  });
});
```

## 7.11 Polling and Custom Wait Conditions

```typescript
test('custom polling patterns', async ({ page }) => {
  await page.goto('https://example.com/reports');

  // Trigger a long-running job
  await page.getByRole('button', { name: 'Generate Report' }).click();

  // Poll every 2 seconds until report is ready (max 60 seconds)
  await page.waitForFunction(
    () => {
      const status = document.querySelector('.report-status')?.textContent;
      return status === 'Ready' || status === 'Failed';
    },
    { timeout: 60000, polling: 2000 }
  );

  // Check final status
  const statusText = await page.locator('.report-status').textContent();
  expect(statusText).toBe('Ready');

  // Polling with page.evaluate() for complex conditions
  await page.waitForFunction(async () => {
    const response = await fetch('/api/job-status');
    const data = await response.json();
    return data.status === 'completed';
  }, { timeout: 120000, polling: 5000 });
});
```

---

# EXPANSION: Chapter 9 â€” Test Organization (Deep Dive)

## 9.5 Advanced Page Object Model Patterns

### Base Page Class

```typescript
// pages/BasePage.ts
import { Page, Locator, expect } from '@playwright/test';

export abstract class BasePage {
  protected readonly page: Page;

  // Common elements present on ALL pages
  protected readonly header: Locator;
  protected readonly footer: Locator;
  protected readonly loadingSpinner: Locator;
  protected readonly toastNotification: Locator;
  protected readonly userMenu: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.getByRole('banner');
    this.footer = page.getByRole('contentinfo');
    this.loadingSpinner = page.locator('.loading-spinner');
    this.toastNotification = page.locator('.toast');
    this.userMenu = page.getByRole('button', { name: /account/i });
  }

  // Common methods available on ALL pages
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 });
  }

  async expectToast(message: string, type: 'success' | 'error' | 'info' = 'success'): Promise<void> {
    await expect(this.toastNotification).toBeVisible({ timeout: 5000 });
    await expect(this.toastNotification).toContainText(message);
    if (type !== 'info') {
      await expect(this.toastNotification).toHaveClass(new RegExp(`toast-${type}`));
    }
  }

  async logout(): Promise<void> {
    await this.userMenu.click();
    await this.page.getByRole('menuitem', { name: 'Sign Out' }).click();
    await this.page.waitForURL('**/login');
  }

  async navigateTo(path: string): Promise<void> {
    await this.page.goto(path);
    await this.waitForPageLoad();
  }

  async getPageTitle(): Promise<string> {
    return this.page.title();
  }

  abstract getUrl(): string;

  async verifyOnCorrectPage(): Promise<void> {
    await expect(this.page).toHaveURL(new RegExp(this.getUrl()));
  }
}
```

### Specific Page Classes

```typescript
// pages/OrdersPage.ts
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

interface OrderRow {
  orderId: string;
  customer: string;
  status: string;
  amount: string;
  date: string;
}

export class OrdersPage extends BasePage {
  private readonly createOrderBtn: Locator;
  private readonly searchInput: Locator;
  private readonly statusFilter: Locator;
  private readonly ordersTable: Locator;
  private readonly paginationNext: Locator;

  constructor(page: Page) {
    super(page);
    this.createOrderBtn = page.getByRole('button', { name: 'Create Order' });
    this.searchInput = page.getByPlaceholder('Search orders...');
    this.statusFilter = page.getByLabel('Filter by Status');
    this.ordersTable = page.getByRole('table', { name: 'Orders' });
    this.paginationNext = page.getByRole('button', { name: 'Next page' });
  }

  getUrl(): string { return '/orders'; }

  async createOrder(): Promise<void> {
    await this.createOrderBtn.click();
    await this.page.waitForURL('**/orders/new');
  }

  async searchOrders(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchInput.press('Enter');
    await this.waitForPageLoad();
  }

  async filterByStatus(status: string): Promise<void> {
    await this.statusFilter.selectOption({ label: status });
    await this.waitForPageLoad();
  }

  async getOrderCount(): Promise<number> {
    return this.ordersTable.getByRole('row').count()
      .then(count => count - 1); // Subtract header row
  }

  async getOrderData(rowIndex: number): Promise<OrderRow> {
    const row = this.ordersTable.getByRole('row').nth(rowIndex + 1); // +1 for header
    const cells = row.getByRole('cell');

    return {
      orderId: (await cells.nth(0).textContent()) ?? '',
      customer: (await cells.nth(1).textContent()) ?? '',
      status: (await cells.nth(2).textContent()) ?? '',
      amount: (await cells.nth(3).textContent()) ?? '',
      date: (await cells.nth(4).textContent()) ?? '',
    };
  }

  async clickOrderById(orderId: string): Promise<void> {
    await this.ordersTable
      .getByRole('row')
      .filter({ hasText: orderId })
      .getByRole('link')
      .click();
  }

  async expectOrderInTable(orderId: string): Promise<void> {
    await expect(this.ordersTable.getByText(orderId)).toBeVisible();
  }
}
```

---

## 9.6 Advanced Fixture Patterns

### Fixtures with API setup

```typescript
// fixtures/index.ts â€” Central fixture file
import { test as base, expect, APIRequestContext } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { OrdersPage } from '../pages/OrdersPage';

// Define all custom fixture types
type Pages = {
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
  ordersPage: OrdersPage;
};

type TestHelpers = {
  createTestOrder: (productId: string) => Promise<string>; // returns orderId
  deleteTestOrder: (orderId: string) => Promise<void>;
  createTestUser: () => Promise<{ id: string; email: string; password: string }>;
};

type AllFixtures = Pages & TestHelpers;

export const test = base.extend<AllFixtures>({
  // Page object fixtures (automatically instantiated)
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },

  ordersPage: async ({ page }, use) => {
    await use(new OrdersPage(page));
  },

  // Helper fixtures with API calls
  createTestOrder: async ({ request }, use) => {
    const createdOrderIds: string[] = [];

    // Provide the helper function to the test
    await use(async (productId: string) => {
      const response = await request.post('/api/orders', {
        headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
        data: { productId, quantity: 1 },
      });
      expect(response.status()).toBe(201);
      const order = await response.json();
      createdOrderIds.push(order.id);
      return order.id;
    });

    // Auto-cleanup after test
    for (const orderId of createdOrderIds) {
      await request.delete(`/api/orders/${orderId}`, {
        headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
      });
    }
  },

  createTestUser: async ({ request }, use) => {
    const createdUserIds: string[] = [];

    await use(async () => {
      const password = `Test@${Date.now()}`;
      const response = await request.post('/api/users', {
        headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
        data: {
          email: `test-${Date.now()}@automation.com`,
          password,
          role: 'user',
        },
      });
      const user = await response.json();
      createdUserIds.push(user.id);
      return { id: user.id, email: user.email, password };
    });

    // Cleanup all created users
    for (const userId of createdUserIds) {
      await request.delete(`/api/users/${userId}`, {
        headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
      });
    }
  },
});

export { expect };
```

```typescript
// tests/orders.spec.ts â€” Using advanced fixtures
import { test, expect } from '../fixtures';

test('view created order in UI', async ({ page, ordersPage, createTestOrder }) => {
  // Create test data via API (fast)
  const orderId = await createTestOrder('PROD-001');

  // Verify in UI
  await page.goto('/orders');
  await ordersPage.expectOrderInTable(orderId);

  const orderData = await ordersPage.getOrderData(0);
  expect(orderData.orderId).toBe(orderId);
});
```

---

## 9.7 Test Tagging Strategy

```typescript
// Tag tests for selective execution
test('@smoke @critical login works', async ({ page }) => { /* ... */ });
test('@regression @auth password reset', async ({ page }) => { /* ... */ });
test('@api orders API returns 200', async ({ request }) => { /* ... */ });
test('@mobile viewport adapts on small screen', async ({ page }) => { /* ... */ });

// Run only smoke tests (fast â€” run on every PR)
// npx playwright test --grep @smoke

// Run regression tests (slower â€” run nightly)
// npx playwright test --grep @regression

// Exclude flaky tests
// npx playwright test --grep-invert @flaky
```

---

# EXPANSION: Chapter 10 â€” Assertions (Deep Dive)

## 10.5 Visual Regression Testing (Screenshot Comparison)

```typescript
test('visual regression â€” login page', async ({ page }) => {
  await page.goto('/login');

  // Take a screenshot and compare with baseline
  // First run: creates baseline. Subsequent runs: compares.
  await expect(page).toHaveScreenshot('login-page.png', {
    maxDiffPixels: 50,          // Allow up to 50 pixels different
    threshold: 0.1,             // 10% color threshold
    fullPage: true,
  });

  // Element-level screenshot comparison
  const loginForm = page.locator('.login-form');
  await expect(loginForm).toHaveScreenshot('login-form.png');
});

// Update baseline screenshots:
// npx playwright test --update-snapshots
```

## 10.6 API Response Assertions

```typescript
test('comprehensive API assertions', async ({ request }) => {
  const response = await request.get('/api/orders?page=1&limit=10');

  // Status
  expect(response.status()).toBe(200);
  expect(response.ok()).toBeTruthy();

  // Headers
  expect(response.headers()['content-type']).toContain('application/json');
  expect(response.headers()['x-total-count']).toBeDefined();

  // Body
  const body = await response.json();
  expect(body).toHaveProperty('data');
  expect(body).toHaveProperty('pagination');
  expect(Array.isArray(body.data)).toBeTruthy();
  expect(body.data.length).toBeLessThanOrEqual(10);

  // Each item in array
  for (const order of body.data) {
    expect(order).toHaveProperty('id');
    expect(order).toHaveProperty('status');
    expect(order).toHaveProperty('createdAt');
    expect(typeof order.id).toBe('string');
    expect(['pending', 'processing', 'shipped', 'delivered']).toContain(order.status);
  }

  // Pagination
  expect(body.pagination.currentPage).toBe(1);
  expect(body.pagination.limit).toBe(10);
  expect(typeof body.pagination.totalCount).toBe('number');
});
```

## 10.7 Negative Assertions â€” Testing Error States

```typescript
test('negative assertion patterns', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Sign In' }).click(); // Submit empty form

  // Verify error states
  await expect(page.getByLabel('Email')).toHaveClass(/input-error/);
  await expect(page.getByLabel('Password')).toHaveClass(/input-error/);
  await expect(page.getByText('Email is required')).toBeVisible();
  await expect(page.getByText('Password is required')).toBeVisible();

  // Verify we did NOT navigate away
  await expect(page).toHaveURL('/login');
  await expect(page).not.toHaveURL('/dashboard');

  // Verify success message is NOT shown
  await expect(page.getByText('Login successful')).toBeHidden();
  await expect(page.getByRole('alert', { name: 'Success' })).not.toBeVisible();

  // Verify submit button is still present (not replaced by spinner)
  await expect(page.getByRole('button', { name: 'Sign In' })).toBeEnabled();
});
```

---

# EXPANSION: Chapter 13 â€” API Testing (Deep Dive)

## 13.7 Creating a Reusable API Client

```typescript
// utils/ApiClient.ts
import { APIRequestContext, expect } from '@playwright/test';

interface RequestOptions {
  headers?: Record<string, string>;
  params?: Record<string, string>;
  data?: unknown;
  timeout?: number;
}

export class ApiClient {
  private readonly baseHeaders: Record<string, string>;

  constructor(
    private readonly request: APIRequestContext,
    private readonly baseUrl: string,
    private readonly authToken?: string
  ) {
    this.baseHeaders = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    };
  }

  async get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    const response = await this.request.get(`${this.baseUrl}${endpoint}`, {
      headers: { ...this.baseHeaders, ...options?.headers },
      params: options?.params,
      timeout: options?.timeout ?? 30000,
    });
    expect(response.ok(), `GET ${endpoint} failed: ${response.status()}`).toBeTruthy();
    return response.json() as Promise<T>;
  }

  async post<T>(endpoint: string, data: unknown, options?: RequestOptions): Promise<T> {
    const response = await this.request.post(`${this.baseUrl}${endpoint}`, {
      headers: { ...this.baseHeaders, ...options?.headers },
      data,
      timeout: options?.timeout ?? 30000,
    });
    expect(
      [200, 201].includes(response.status()),
      `POST ${endpoint} failed: ${response.status()}`
    ).toBeTruthy();
    return response.json() as Promise<T>;
  }

  async put<T>(endpoint: string, data: unknown, options?: RequestOptions): Promise<T> {
    const response = await this.request.put(`${this.baseUrl}${endpoint}`, {
      headers: { ...this.baseHeaders, ...options?.headers },
      data,
    });
    expect(response.ok(), `PUT ${endpoint} failed: ${response.status()}`).toBeTruthy();
    return response.json() as Promise<T>;
  }

  async delete(endpoint: string, options?: RequestOptions): Promise<void> {
    const response = await this.request.delete(`${this.baseUrl}${endpoint}`, {
      headers: { ...this.baseHeaders, ...options?.headers },
    });
    expect(
      [200, 204].includes(response.status()),
      `DELETE ${endpoint} failed: ${response.status()}`
    ).toBeTruthy();
  }
}

// Usage in tests
test('use API client', async ({ request }) => {
  const api = new ApiClient(request, process.env.API_BASE_URL!, process.env.API_TOKEN);

  interface User { id: string; email: string; name: string; }

  const user = await api.get<User>('/users/1');
  expect(user.email).toBeDefined();

  const newOrder = await api.post<{ id: string }>('/orders', {
    userId: user.id,
    productId: 'PROD-001',
    quantity: 2,
  });
  expect(newOrder.id).toBeDefined();

  await api.delete(`/orders/${newOrder.id}`);
});
```

## 13.8 Authentication Flows via API

```typescript
// utils/auth-api.ts
import { APIRequestContext } from '@playwright/test';

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export async function loginViaApi(
  request: APIRequestContext,
  email: string,
  password: string
): Promise<AuthTokens> {
  const response = await request.post('/api/auth/login', {
    data: { email, password },
  });

  if (!response.ok()) {
    throw new Error(`Login failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

export async function refreshToken(
  request: APIRequestContext,
  token: string
): Promise<AuthTokens> {
  const response = await request.post('/api/auth/refresh', {
    data: { refreshToken: token },
  });
  return response.json();
}

// In playwright.config.ts â€” use API login to set browser storage
// This is much faster than UI login
test('api-authenticated test', async ({ page, request }) => {
  // 1. Get auth token via API
  const { accessToken } = await loginViaApi(request, 'user@test.com', 'password');

  // 2. Set the token in localStorage before navigating
  await page.addInitScript((token: string) => {
    localStorage.setItem('auth_token', token);
  }, accessToken);

  // 3. Navigate directly â€” no login page needed
  await page.goto('/dashboard');
  await expect(page).toHaveURL('/dashboard'); // Already logged in!
});
```

## 13.9 API Contract Testing

```typescript
// Verify the API response matches the expected schema
import Joi from 'joi'; // npm install joi

const userSchema = Joi.object({
  id: Joi.string().uuid().required(),
  email: Joi.string().email().required(),
  name: Joi.string().min(1).max(100).required(),
  role: Joi.string().valid('admin', 'user', 'viewer').required(),
  createdAt: Joi.string().isoDate().required(),
  updatedAt: Joi.string().isoDate().required(),
  isActive: Joi.boolean().required(),
  profile: Joi.object({
    avatar: Joi.string().uri().allow(null),
    bio: Joi.string().max(500).allow(''),
  }).optional(),
});

test('user API schema is correct', async ({ request }) => {
  const response = await request.get('/api/users/1');
  const user = await response.json();

  const { error } = userSchema.validate(user, { abortEarly: false });
  if (error) {
    throw new Error(`Schema validation failed:\n${error.details.map(d => d.message).join('\n')}`);
  }
});
```

## 13.10 Mocking Third-Party APIs

```typescript
test('mock payment gateway', async ({ page }) => {
  // Mock Stripe/Razorpay API calls so tests don't make real payments
  await page.route('**/api.stripe.com/**', async (route) => {
    const url = route.request().url();

    if (url.includes('/payment_intents')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'pi_test_mock_123',
          status: 'succeeded',
          amount: 99900,
          currency: 'inr',
          client_secret: 'pi_test_mock_123_secret_abc',
        }),
      });
    } else {
      await route.continue(); // Pass through other requests
    }
  });

  // Mock OTP/SMS APIs
  await page.route('**/api.twilio.com/**', async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'sent' }) });
  });

  await page.goto('/checkout');
  await page.getByLabel('Card Number').fill('4242424242424242');
  await page.getByLabel('Expiry').fill('12/26');
  await page.getByLabel('CVV').fill('123');
  await page.getByRole('button', { name: 'Pay â‚¹999' }).click();

  await expect(page.getByText('Payment successful')).toBeVisible();
});
```

---

# EXPANSION: Chapter 14 â€” Cross-Browser Testing (Deep Dive)

## 14.4 Browser-Specific Quirks and Handling

```typescript
import { test, expect, BrowserName } from '@playwright/test';

test('handle browser-specific clipboard', async ({ page, browserName }) => {
  await page.goto('/');

  if (browserName === 'chromium') {
    // Grant clipboard permissions for Chrome
    await page.context().grantPermissions(['clipboard-read', 'clipboard-write']);
    await page.getByRole('button', { name: 'Copy Link' }).click();
    const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
    expect(clipboardText).toContain('https://');
  } else {
    // Firefox/WebKit â€” use keyboard shortcut
    await page.getByRole('button', { name: 'Copy Link' }).click();
    await expect(page.getByText('Link copied!')).toBeVisible();
  }
});

// Browser-specific CSS and rendering
test('check scroll behavior', async ({ page, browserName }) => {
  await page.goto('/products');

  if (browserName === 'webkit') {
    // Safari sometimes needs explicit scroll trigger
    await page.evaluate(() => {
      window.scrollTo({ top: 500, behavior: 'instant' });
    });
  } else {
    await page.mouse.wheel(0, 500);
  }

  await expect(page.getByTestId('back-to-top')).toBeVisible();
});
```

## 14.5 Testing Different Viewport Sizes

```typescript
test('responsive design testing', async ({ page }) => {
  await page.goto('/');

  const viewports = [
    { name: 'Mobile S', width: 320, height: 568 },
    { name: 'Mobile M', width: 375, height: 667 },
    { name: 'Mobile L', width: 425, height: 812 },
    { name: 'Tablet', width: 768, height: 1024 },
    { name: 'Laptop', width: 1280, height: 800 },
    { name: 'Full HD', width: 1920, height: 1080 },
    { name: '4K', width: 2560, height: 1440 },
  ];

  for (const vp of viewports) {
    await page.setViewportSize({ width: vp.width, height: vp.height });

    if (vp.width < 768) {
      // Mobile: hamburger menu should be visible, nav hidden
      await expect(page.getByRole('button', { name: 'Menu' })).toBeVisible();
      await expect(page.getByRole('navigation')).toBeHidden();
    } else {
      // Desktop: navigation always visible
      await expect(page.getByRole('navigation')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Menu' })).toBeHidden();
    }

    // Take screenshot for visual record
    await page.screenshot({
      path: `screenshots/responsive-${vp.name.replace(' ', '-')}.png`,
    });
  }
});
```

## 14.6 Geolocation and Permissions Testing

```typescript
test('location-based content', async ({ browser }) => {
  // Test with Mumbai location
  const mumbaiContext = await browser.newContext({
    geolocation: { latitude: 19.0760, longitude: 72.8777 },
    permissions: ['geolocation'],
    locale: 'en-IN',
  });
  const page = await mumbaiContext.newPage();
  await page.goto('https://example.com/stores');
  await page.getByRole('button', { name: 'Find Stores Near Me' }).click();
  await expect(page.getByText('Stores in Mumbai')).toBeVisible();
  await mumbaiContext.close();

  // Test with Delhi location
  const delhiContext = await browser.newContext({
    geolocation: { latitude: 28.6139, longitude: 77.2090 },
    permissions: ['geolocation'],
  });
  const page2 = await delhiContext.newPage();
  await page2.goto('https://example.com/stores');
  await page2.getByRole('button', { name: 'Find Stores Near Me' }).click();
  await expect(page2.getByText('Stores in Delhi')).toBeVisible();
  await delhiContext.close();
});

test('permission denied scenarios', async ({ browser }) => {
  const context = await browser.newContext({
    permissions: [], // No permissions granted
  });
  const page = await context.newPage();
  await page.goto('https://example.com/location-required');
  await page.getByRole('button', { name: 'Share Location' }).click();

  // App should handle permission denial gracefully
  await expect(page.getByText('Location permission denied')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Enter Location Manually' })).toBeVisible();
  await context.close();
});
```

---

# EXPANSION: Chapter 15 â€” Parallel Execution (Deep Dive)

## 15.5 Worker Isolation and Shared State

```typescript
// Each worker is completely isolated:
// - Separate browser instance
// - No shared memory
// - No shared global variables

// âŒ WRONG: Shared global state breaks parallel tests
let globalOrderId: string; // Different workers don't share this!
test('create order', async ({ request }) => {
  const order = await createOrder(request);
  globalOrderId = order.id; // Only in this worker
});
test('verify order', async ({ page }) => {
  // globalOrderId is undefined here (different worker possibly)
  await page.goto(`/orders/${globalOrderId}`);
});

// âœ… CORRECT: Pass state through test context or API
test.describe('Order lifecycle', () => {
  let orderId: string;

  // Both tests in same describe share the same worker
  test.describe.configure({ mode: 'serial' });

  test('create order', async ({ request }) => {
    const order = await createOrder(request);
    orderId = order.id;
  });

  test('verify order', async ({ page }) => {
    await page.goto(`/orders/${orderId}`);
  });
});
```

## 15.6 Test Prioritization

```typescript
// playwright.config.ts â€” Configure test order
export default defineConfig({
  // Run tests in random order to catch inter-test dependencies
  // (Not built-in, but achievable via custom plugins)

  projects: [
    // Smoke tests run first (separate project, faster feedback)
    {
      name: 'smoke',
      testMatch: '**/smoke/**/*.spec.ts',
      retries: 0,
      workers: 4,
    },
    // Regression tests run after smoke
    {
      name: 'regression',
      testMatch: '**/e2e/**/*.spec.ts',
      dependencies: ['smoke'],
      retries: 2,
      workers: 8,
    },
    // API tests always run (no browser needed â€” very fast)
    {
      name: 'api',
      testMatch: '**/api/**/*.spec.ts',
      workers: 10,
    },
  ],
});
```

---

# EXPANSION: Chapter 16 â€” Reporting (Deep Dive)

## 16.5 Attaching Custom Data to Reports

```typescript
test('attach evidence to report', async ({ page }) => {
  await page.goto('/orders');

  // Attach screenshot
  const screenshot = await page.screenshot({ fullPage: true });
  await test.info().attach('orders-page', { body: screenshot, contentType: 'image/png' });

  // Attach text data
  const orderCount = await page.getByRole('row').count();
  await test.info().attach('order-count', {
    body: `Total orders: ${orderCount}`,
    contentType: 'text/plain',
  });

  // Attach JSON data
  const orders = await page.evaluate(() => {
    return Array.from(document.querySelectorAll('tr')).map(row => row.textContent);
  });
  await test.info().attach('orders-data', {
    body: JSON.stringify(orders, null, 2),
    contentType: 'application/json',
  });

  // Add custom annotation
  test.info().annotations.push({
    type: 'TestRail',
    description: 'C12345',
  });
});
```

---

# EXPANSION: Chapter 18 â€” Debugging (Deep Dive)

## 18.7 VS Code Debugging Setup

```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Playwright: Debug Current Test File",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/node_modules/.bin/playwright",
      "args": ["test", "${file}", "--headed", "--debug"],
      "console": "integratedTerminal",
      "env": {
        "PWDEBUG": "1"
      }
    },
    {
      "name": "Playwright: Debug All Tests",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/node_modules/.bin/playwright",
      "args": ["test", "--headed"],
      "console": "integratedTerminal"
    }
  ]
}
```

## 18.8 Analyzing Failures from CI

```typescript
// When a test fails in CI, Playwright saves:
// 1. Screenshot (if screenshot: 'only-on-failure')
// 2. Video (if video: 'retain-on-failure')
// 3. Trace (if trace: 'on-first-retry')

// To view the trace locally from CI artifacts:
// 1. Download the test-results/ folder from CI
// 2. Run: npx playwright show-trace test-results/.../trace.zip

// Debugging a specific failure from report:
// npx playwright test --last-failed          â†’ re-run only failed tests
// npx playwright test --repeat-each=5        â†’ run tests 5 times to catch flakiness
```

## 18.9 Performance Profiling

```typescript
test('measure page performance', async ({ page }) => {
  await page.goto('/');

  // Get Core Web Vitals
  const metrics = await page.evaluate(() => {
    return new Promise<Record<string, number>>((resolve) => {
      const data: Record<string, number> = {};

      // First Contentful Paint
      new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.name === 'first-contentful-paint') {
            data.fcp = entry.startTime;
          }
        }
      }).observe({ entryTypes: ['paint'] });

      // Largest Contentful Paint
      new PerformanceObserver((list) => {
        const entries = list.getEntries();
        data.lcp = entries[entries.length - 1].startTime;
        resolve(data);
      }).observe({ entryTypes: ['largest-contentful-paint'] });

      setTimeout(() => resolve(data), 5000);
    });
  });

  console.log('FCP:', metrics.fcp, 'ms');
  console.log('LCP:', metrics.lcp, 'ms');

  // Assert performance thresholds
  expect(metrics.fcp).toBeLessThan(1800);  // FCP < 1.8s (Good)
  expect(metrics.lcp).toBeLessThan(2500);  // LCP < 2.5s (Good)
});
```

---

# EXPANSION: Chapter 19 â€” Best Practices (Deep Dive)

## 19.6 Environment-Specific Configuration

```typescript
// .env.local
// BASE_URL=http://localhost:3000
// API_BASE_URL=http://localhost:3001

// .env.staging
// BASE_URL=https://staging.example.com
// API_BASE_URL=https://api-staging.example.com

// .env.production
// BASE_URL=https://example.com
// API_BASE_URL=https://api.example.com

// playwright.config.ts
import dotenv from 'dotenv';
const ENV = process.env.TEST_ENV || 'local';
dotenv.config({ path: `.env.${ENV}` });

// Run for specific environment:
// TEST_ENV=staging npx playwright test
// TEST_ENV=production npx playwright test --grep @smoke
```

## 19.7 Handling Cookies and Local Storage

```typescript
// fixtures/storage-fixture.ts
export const test = base.extend({
  // Start each test with empty storage (full isolation)
  page: async ({ page }, use) => {
    // Clear all cookies and storage before test
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    await use(page);
  },
});

// Manually manage cookies
test('cookie-based authentication', async ({ page }) => {
  // Set auth cookie programmatically (faster than UI login)
  await page.context().addCookies([
    {
      name: 'session_token',
      value: process.env.SESSION_TOKEN!,
      domain: 'example.com',
      path: '/',
      httpOnly: true,
      secure: true,
    },
  ]);

  await page.goto('/dashboard');
  await expect(page).toHaveURL('/dashboard');
});
```

## 19.8 Accessibility Testing

```typescript
// npm install @axe-core/playwright
import AxeBuilder from '@axe-core/playwright';

test('accessibility audit â€” login page', async ({ page }) => {
  await page.goto('/login');

  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  // Attach results to report
  await test.info().attach('accessibility-results', {
    body: JSON.stringify(accessibilityScanResults, null, 2),
    contentType: 'application/json',
  });

  // Assert no violations
  expect(accessibilityScanResults.violations).toHaveLength(0);
});

// Focus order test
test('keyboard navigation order is logical', async ({ page }) => {
  await page.goto('/checkout');

  const focusOrder: string[] = [];

  // Tab through all focusable elements
  for (let i = 0; i < 10; i++) {
    await page.keyboard.press('Tab');
    const focused = await page.evaluate(() =>
      document.activeElement?.getAttribute('aria-label') ||
      document.activeElement?.getAttribute('placeholder') ||
      document.activeElement?.textContent?.trim() ||
      document.activeElement?.tagName
    );
    if (focused) focusOrder.push(focused);
  }

  console.log('Focus order:', focusOrder);
  // Verify logical order
  expect(focusOrder.indexOf('First Name')).toBeLessThan(focusOrder.indexOf('Last Name'));
  expect(focusOrder.indexOf('Email')).toBeLessThan(focusOrder.indexOf('Submit'));
});
```

---

# EXPANSION: Chapter 20 â€” Interview Questions (Complete Edition)

## 20.6 Conceptual Deep-Dive Questions

**Q16: How does Playwright achieve cross-browser testing without separate drivers?**

> Playwright ships with its own browser builds (Chromium, Firefox, WebKit) that are patched to expose a unified automation protocol. For Chromium, it uses CDP (Chrome DevTools Protocol). For Firefox and WebKit, Microsoft contributed patches to expose similar APIs. This means no chromedriver.exe, geckodriver.exe, or safaridriver management â€” `npx playwright install` downloads everything needed.

**Q17: Explain the difference between `page.route()` and `page.waitForResponse()`.**

> `page.route()` **intercepts** requests before they reach the server â€” you can modify, mock, or block them. `page.waitForResponse()` **observes** responses that actually happened â€” you can assert on them but not change them. Use `route()` for mocking and `waitForResponse()` for verifying real API calls.

**Q18: What is the difference between `context.storageState()` and cookies?**

> `context.storageState()` captures ALL browser storage: cookies, localStorage, sessionStorage, and IndexedDB â€” as a single JSON file. Regular cookies are just one part. `storageState()` is used for auth reuse because it captures the complete browser state needed for session persistence.

**Q19: How does Playwright's `locator()` differ from `$()` (page.$)?**

> `page.locator()` returns a **Locator object** â€” it's lazy (doesn't search the DOM yet), supports chaining, and applies auto-waiting when you perform actions on it. `page.$()` is a legacy method that immediately queries the DOM and returns an **ElementHandle** or null. ElementHandle doesn't auto-wait and has fewer capabilities. Always use Locators.

**Q20: What happens when `fullyParallel: true` is set?**

> By default, Playwright runs test **files** in parallel but tests **within a file** sequentially. With `fullyParallel: true`, ALL tests everywhere run in parallel â€” even multiple tests in the same file run simultaneously in different workers. This requires all tests to be truly independent (no shared state).

---

## 20.7 Framework Design Questions

**Q21: How would you design a Playwright framework for a team of 10 developers?**

```
Architecture decisions:
1. Single playwright.config.ts as source of truth
2. Multiple .env files per environment (local, staging, prod)
3. Page Object Model for all pages
4. Central fixtures file (fixtures/index.ts) exported as test
5. Shared auth states (auth/admin.json, auth/user.json)
6. Tagging strategy: @smoke, @regression, @api, @critical
7. API-first setup: create test data via API, verify in UI
8. CI: GitHub Actions with 4-way sharding
9. Allure reporting for management visibility
10. ESLint + TypeScript strict mode for code quality

Folder structure:
playwright-framework/
â”œâ”€â”€ pages/           POM classes
â”œâ”€â”€ fixtures/        Reusable fixtures
â”œâ”€â”€ tests/           Test files (e2e, api, smoke)
â”œâ”€â”€ utils/           Helpers
â”œâ”€â”€ test-data/       JSON data
â””â”€â”€ auth/            Saved auth states
```

**Q22: How do you handle test data in a shared environment where multiple teams run tests simultaneously?**

```
Strategies:
1. Prefix test data with unique IDs
   email: `automation-${Date.now()}@test.com`

2. Use test-specific data (create â†’ use â†’ delete)
   - Create in beforeEach via API
   - Delete in afterEach via API

3. Use read-only test data
   - Pre-created, never modified by tests
   - Tests only read/assert on this data

4. Feature flags
   - Isolate test data behind feature flags
   - Only automation tests see flagged data

5. Tenant isolation
   - Automation has its own tenant/org
   - Production data is never touched
```

**Q23: How would you implement retry logic that is smarter than just "retry 3 times"?**

```typescript
// Custom retry with exponential backoff
async function retryWithBackoff<T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  baseDelayMs: number = 1000
): Promise<T> {
  let lastError: Error;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      const delay = baseDelayMs * Math.pow(2, attempt);
      console.log(`Attempt ${attempt + 1} failed. Retrying in ${delay}ms...`);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  throw lastError!;
}

// Usage
const result = await retryWithBackoff(
  () => apiClient.post('/flaky-endpoint', data),
  3,
  500
);
```

---

## 20.8 Real-World Debugging Scenarios

**Scenario: "Test passes locally but fails in CI"**

```
Step-by-step debugging:
1. Check headless vs headed mode
   - CI runs headless â€” some elements behave differently
   - Add headless: false locally to reproduce

2. Check timing differences
   - CI machines are slower (shared CPU)
   - Increase timeouts for CI: actionTimeout: 45000

3. Check environment variables
   - Missing .env variables in CI?
   - Print process.env.BASE_URL in test setup

4. Check browser versions
   - Local browser â‰  CI browser
   - Use 'npx playwright install' in CI workflow

5. Check network conditions
   - CI may have slower internet
   - Mock external APIs in tests

6. Review trace from CI failure
   - Download trace.zip from CI artifacts
   - npx playwright show-trace trace.zip
   - Check DOM state at failure point
```

**Scenario: "500 tests take 2 hours in CI â€” too slow"**

```
Optimization plan:
1. Enable sharding across 4 agents (4x speed)
   --shard=1/4, 2/4, 3/4, 4/4

2. Replace UI login with storageState
   - 10 tests Ã— 30s UI login = 5 minutes saved
   - 100 tests Ã— 30s UI login = 50 minutes saved

3. Replace UI setup with API setup
   - Creating a user via UI: 45 seconds
   - Creating via API: 0.5 seconds

4. Group and parallelize independent tests
   - fullyParallel: true

5. Move slow tests to a separate suite
   - Run smoke (50 tests) on every PR: 5 minutes
   - Run full regression (500 tests) nightly

6. Cache browser downloads in CI
   - Cache node_modules and ~/.cache/ms-playwright
```

**Scenario: "Element exists in DOM but click() fails"**

```typescript
// Common causes and fixes:
test('debugging invisible overlay issue', async ({ page }) => {
  await page.goto('/dashboard');

  // Check if element is covered by another element
  const button = page.getByRole('button', { name: 'Submit' });

  // Get element's bounding box
  const box = await button.boundingBox();
  console.log('Button position:', box);

  // Check what element is at that position
  const elementAtPosition = await page.evaluate(({ x, y }) => {
    const el = document.elementFromPoint(x, y);
    return el ? `${el.tagName} .${el.className} #${el.id}` : 'none';
  }, { x: box!.x + box!.width/2, y: box!.y + box!.height/2 });
  console.log('Element at click position:', elementAtPosition);

  // Fix 1: Scroll element into view first
  await button.scrollIntoViewIfNeeded();
  await button.click();

  // Fix 2: Close any overlay first
  await page.locator('.modal-overlay').waitFor({ state: 'hidden' });
  await button.click();

  // Fix 3: Use force if you know it's safe
  await button.click({ force: true });
});
```

---

## 20.9 Behavioral/Process Questions

**Q24: How do you decide which tests to automate first?**

```
Priority matrix:
HIGH VALUE + HIGH RISK = Automate first
1. Smoke tests (core user flows)
   - Login, registration, critical purchase flow
   - Run on every PR

2. Regression tests for fixed bugs
   - Bugs that came back before
   - Automated to prevent recurrence

3. High-traffic user journeys
   - 80% of users follow 20% of paths
   - Automate the 20%

4. NOT good candidates for automation:
   - One-time features
   - UI constantly changing
   - Very complex UI interactions with low ROI
   - Exploratory testing (human judgment needed)
```

**Q25: How do you maintain a test suite over time?**

```
Maintenance strategy:
1. Regular review (monthly)
   - Remove obsolete tests
   - Update for UI changes
   - Fix consistently failing tests

2. Flaky test management
   - Tag with @flaky
   - Create a ticket to fix
   - Never ignore â€” investigate root cause

3. Ownership
   - Each dev team owns their feature tests
   - QA team owns framework and smoke tests

4. Documentation
   - README with setup instructions
   - CONTRIBUTING.md with standards
   - Comments for complex test logic

5. Code quality
   - ESLint for consistency
   - PR reviews for test code
   - TypeScript strict mode
```

---

## 20.10 Quick Code Challenges (Interview Exercises)

**Challenge 1: Write a POM for a login page**
```typescript
// See Chapter 9.4 for complete solution
```

**Challenge 2: Write a test that handles a file download and verifies its content**
```typescript
// See Chapter 6.13 for complete solution
```

**Challenge 3: Write a test that verifies an API call is made when a button is clicked**
```typescript
test('button triggers correct API call', async ({ page }) => {
  await page.goto('/checkout');

  const [response] = await Promise.all([
    page.waitForResponse(
      resp => resp.url().includes('/api/orders') && resp.request().method() === 'POST'
    ),
    page.getByRole('button', { name: 'Place Order' }).click(),
  ]);

  expect(response.status()).toBe(201);
  const body = await response.json();
  expect(body.orderId).toBeDefined();
});
```

**Challenge 4: Write a test for infinite scroll**
```typescript
test('infinite scroll loads more items', async ({ page }) => {
  await page.goto('/products');
  const initialCount = await page.getByRole('article').count();
  expect(initialCount).toBe(20);

  // Scroll to bottom
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));

  // Wait for new items
  await expect(page.getByRole('article')).toHaveCount(40, { timeout: 10000 });
});
```

**Challenge 5: Write a test for multi-step form with validation**
```typescript
test('multi-step registration form', async ({ page }) => {
  await page.goto('/register');

  // Step 1: Personal info
  await test.step('Fill personal information', async () => {
    await page.getByLabel('First Name').fill('John');
    await page.getByLabel('Last Name').fill('Doe');
    await page.getByLabel('Date of Birth').fill('1990-01-15');
    await page.getByRole('button', { name: 'Next' }).click();
    await expect(page.getByText('Step 2 of 3')).toBeVisible();
  });

  // Step 2: Contact info
  await test.step('Fill contact information', async () => {
    await page.getByLabel('Email').fill('john@test.com');
    await page.getByLabel('Phone').fill('+91-9876543210');
    await page.getByRole('button', { name: 'Next' }).click();
    await expect(page.getByText('Step 3 of 3')).toBeVisible();
  });

  // Step 3: Password
  await test.step('Set password', async () => {
    await page.getByLabel('Password').fill('Secure@123');
    await page.getByLabel('Confirm Password').fill('Secure@123');
    await page.getByRole('checkbox', { name: 'I agree to Terms' }).check();
    await page.getByRole('button', { name: 'Create Account' }).click();
  });

  await expect(page).toHaveURL('/welcome');
  await expect(page.getByText('Welcome, John!')).toBeVisible();
});
```

---

## 20.11 Comparison Cheat Sheet for Interviews

### Playwright vs Selenium Quick Reference

| Question | Playwright Answer | Selenium Answer |
|---|---|---|
| Protocol | WebSocket (CDP) | HTTP (WebDriver) |
| Wait strategy | Auto-waiting | Manual waits |
| Browser install | `npx playwright install` | Separate driver download |
| Multi-tab | Native | Complex |
| Speed | Fast | Slower |
| API testing | Built-in | Separate library |

### Key Playwright Commands

```
npx playwright test                  â†’ Run all
npx playwright test --headed         â†’ Show browser
npx playwright test --debug          â†’ Debug mode
npx playwright test --grep @smoke    â†’ Run by tag
npx playwright test --project=chrome â†’ Specific browser
npx playwright test --shard=1/4      â†’ Shard for CI
npx playwright show-report           â†’ View HTML report
npx playwright codegen URL           â†’ Record test
npx playwright show-trace trace.zip  â†’ View trace
```

### Most Common Locators (Priority Order)

```typescript
1. page.getByRole('button', { name: 'Submit' })    // BEST
2. page.getByLabel('Email')                         // BEST for inputs
3. page.getByPlaceholder('Enter email')             // GOOD
4. page.getByText('Sign In')                        // GOOD
5. page.getByTestId('submit-btn')                   // GOOD with dev support
6. page.locator('#id')                              // OK
7. page.locator('.class')                           // OK
8. page.locator('[data-id="123"]')                  // OK
9. page.locator('//xpath')                          // AVOID when possible
```

---

*This completes the comprehensive Playwright + TypeScript Training Guide.*
*Use this document as your single reference for learning, practice, and interview preparation.*

---

# ADDITIONAL PROGRAMS â€” Missing Topics

## Ch1: Playwright Architecture â€” Runnable Demo

```typescript
// tests/architecture-demo.spec.ts
import { test, expect, chromium } from '@playwright/test';

test('browser â†’ context â†’ page hierarchy demo', async () => {
  const browser = await chromium.launch({ headless: true });
  console.log('Browser:', browser.version());

  // Two ISOLATED sessions (like two different users)
  const adminCtx = await browser.newContext({ locale: 'en-IN' });
  const userCtx  = await browser.newContext({ locale: 'en-IN' });

  // Each context gets its own tab
  const adminPage = await adminCtx.newPage();
  const userPage  = await userCtx.newPage();

  await adminCtx.addCookies([{ name: 'role', value: 'admin', domain: 'example.com', path: '/' }]);
  await userCtx.addCookies ([{ name: 'role', value: 'user',  domain: 'example.com', path: '/' }]);

  // Contexts do NOT share cookies
  const adminCookies = await adminCtx.cookies('https://example.com');
  const userCookies  = await userCtx.cookies ('https://example.com');
  console.log('Admin cookie role:', adminCookies.find(c => c.name === 'role')?.value); // admin
  console.log('User  cookie role:', userCookies .find(c => c.name === 'role')?.value); // user

  await adminCtx.close();
  await userCtx.close();
  await browser.close();
});
```

## Ch8: Complete Multi-Tab Program

```typescript
// tests/multi-tab-complete.spec.ts
import { test, expect, Page } from '@playwright/test';

test.describe('Multi-tab handling', () => {
  test('ctrl+click opens new tab â€” verify and close', async ({ page, context }) => {
    await page.goto('https://playwright.dev/');
    const [newTab] = await Promise.all([
      context.waitForEvent('page'),
      page.getByRole('link', { name: 'Docs' }).click({ modifiers: ['Control'] }),
    ]);
    await newTab.waitForLoadState('domcontentloaded');
    await expect(newTab).toHaveURL(/docs/);
    await newTab.close();
    await expect(page).toHaveURL('https://playwright.dev/'); // back on original
  });

  test('two independent user sessions', async ({ browser }) => {
    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const pg1 = await ctx1.newPage();
    const pg2 = await ctx2.newPage();

    await pg1.goto('https://example.com');
    await pg2.goto('https://example.com');

    // Set cookie in ctx1 â€” ctx2 is unaffected
    await ctx1.addCookies([{ name: 'session', value: 'abc', domain: 'example.com', path: '/' }]);
    expect((await ctx1.cookies()).length).toBe(1);
    expect((await ctx2.cookies()).length).toBe(0); // isolated

    await ctx1.close();
    await ctx2.close();
  });

  test('handle window.open popup', async ({ page, context }) => {
    await page.goto('https://example.com');
    const popupPromise = context.waitForEvent('page');
    await page.evaluate(() => window.open('https://example.com', '_blank'));
    const popup: Page = await popupPromise;
    await popup.waitForLoadState('load');
    expect(popup.url()).toContain('example.com');
    await popup.close();
  });
});
```

## Ch15: Parallel Execution Patterns

```typescript
// tests/parallel-patterns.spec.ts
import { test, expect } from '@playwright/test';

// Default: tests in different files run in parallel
// Tests WITHIN this file run sequentially unless fullyParallel:true

test.describe('Parallel-safe tests (no shared state)', () => {
  test('A â€” homepage title', async ({ page }) => {
    await page.goto('https://example.com');
    await expect(page).toHaveTitle(/Example/);
  });

  test('B â€” heading visible', async ({ page }) => {
    await page.goto('https://example.com');
    await expect(page.getByRole('heading').first()).toBeVisible();
  });

  test('C â€” link count', async ({ page }) => {
    await page.goto('https://example.com');
    const links = await page.getByRole('link').count();
    expect(links).toBeGreaterThan(0);
  });
});

// Serial â€” order matters
test.describe('Serial checkout flow', () => {
  test.describe.configure({ mode: 'serial' });
  let cartId: string;

  test('1. add item to cart', async ({ page }) => {
    await page.goto('https://example.com');
    cartId = 'CART-' + Date.now();
    console.log('Cart created:', cartId);
  });

  test('2. apply coupon', async ({ page }) => {
    console.log('Applying coupon to cart:', cartId);
    expect(cartId).toBeDefined(); // Only works in serial mode
  });

  test('3. checkout', async ({ page }) => {
    console.log('Checking out cart:', cartId);
    expect(cartId).toBeDefined();
  });
});
```

## Ch17: CI/CD Helper Utilities

```typescript
// utils/ci-helpers.ts
export const isCI = () => !!process.env.CI;

export const getCIProvider = (): string => {
  if (process.env.GITHUB_ACTIONS) return 'GitHub Actions';
  if (process.env.JENKINS_URL)    return 'Jenkins';
  if (process.env.GITLAB_CI)      return 'GitLab CI';
  return 'Local';
};

export const getRunId = (): string =>
  process.env.GITHUB_RUN_ID || process.env.BUILD_NUMBER || Date.now().toString();
```

```typescript
// tests/smoke/health-check.spec.ts
import { test, expect } from '@playwright/test';
import { isCI, getCIProvider } from '../../utils/ci-helpers';

test.describe('Smoke Tests @smoke', () => {
  test.beforeAll(() => {
    console.log('Provider:', getCIProvider());
    console.log('BASE_URL:', process.env.BASE_URL);
    console.log('Is CI:', isCI());
  });

  test('app is reachable', async ({ page }) => {
    const resp = await page.goto(process.env.BASE_URL || 'https://example.com');
    expect(resp!.status()).toBeLessThan(400);
  });

  test('page loads under 5s', async ({ page }) => {
    const t = Date.now();
    await page.goto(process.env.BASE_URL || 'https://example.com');
    await page.waitForLoadState('networkidle');
    expect(Date.now() - t).toBeLessThan(5000);
  });

  test('API health endpoint', async ({ request }) => {
    const resp = await request.get(
      `${process.env.API_BASE_URL || 'https://example.com'}/health`
    );
    expect([200, 204]).toContain(resp.status());
  });
});
```

## Ch12: Complete Test Data Factory

```typescript
// utils/test-data-factory.ts
import { faker } from '@faker-js/faker';

export interface TestUser {
  firstName: string; lastName: string;
  email: string; password: string; phone: string;
  role: 'admin' | 'user' | 'viewer';
}

export const TestDataFactory = {
  user(role: TestUser['role'] = 'user'): TestUser {
    return {
      firstName: faker.person.firstName(),
      lastName:  faker.person.lastName(),
      email:     faker.internet.email({ provider: 'automation-test.com' }),
      password:  'Test@' + faker.string.alphanumeric(8),
      phone:     faker.phone.number('+91-##########'),
      role,
    };
  },
  admin(): TestUser { return this.user('admin'); },
};
```

```typescript
// tests/data-driven-registration.spec.ts
import { test, expect } from '@playwright/test';
import { TestDataFactory } from '../utils/test-data-factory';

// Generate fresh users for each scenario
const users = [
  TestDataFactory.user('user'),
  TestDataFactory.user('viewer'),
  TestDataFactory.admin(),
];

for (const u of users) {
  test(`register as ${u.role} â€” ${u.email}`, async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('First Name').fill(u.firstName);
    await page.getByLabel('Email').fill(u.email);
    await page.getByLabel('Password').fill(u.password);
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page.getByText('Registration successful')).toBeVisible();
  });
}

// Validation edge cases
const invalidCases = [
  { email: '',             password: 'Test@123', error: 'Email is required' },
  { email: 'notanemail',   password: 'Test@123', error: 'Invalid email' },
  { email: 'a@b.com',     password: '123',       error: 'Password too short' },
];

for (const c of invalidCases) {
  test(`validation: email="${c.email}" pw="${c.password}"`, async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('Email').fill(c.email);
    await page.getByLabel('Password').fill(c.password);
    await page.getByRole('button', { name: 'Register' }).click();
    await expect(page.getByText(c.error)).toBeVisible();
  });
}
```

## Complete End-to-End Order Flow Test

```typescript
// tests/e2e/order-flow.spec.ts
import { test, expect } from '@playwright/test';
import { TestDataFactory } from '../../utils/test-data-factory';

test.describe('E2E Order Flow @e2e', () => {
  test.describe.configure({ mode: 'serial' });
  const user = TestDataFactory.admin();
  let orderId: string;

  test.beforeAll(async ({ request }) => {
    const r = await request.post('/api/users', {
      headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
      data: user,
    });
    expect(r.status()).toBe(201);
  });

  test('1. login', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(user.email);
    await page.getByLabel('Password').fill(user.password);
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('2. add product to cart', async ({ page }) => {
    await page.goto('/products');
    await page.getByPlaceholder('Search...').fill('Laptop');
    await page.keyboard.press('Enter');
    await page.waitForLoadState('networkidle');
    await page.getByRole('article').first().hover();
    await page.getByRole('button', { name: 'Add to Cart' }).first().click();
    await expect(page.getByTestId('cart-count')).toHaveText('1');
  });

  test('3. checkout and capture order ID', async ({ page }) => {
    await page.goto('/cart');
    await page.getByRole('button', { name: 'Proceed to Checkout' }).click();

    // Fill shipping
    await page.getByLabel('Full Name').fill(`${user.firstName} ${user.lastName}`);
    await page.getByLabel('PIN Code').fill('400001');
    await page.getByLabel('Phone').fill(user.phone);

    // Payment via iframe
    const frame = page.frameLocator('iframe[name="payment"]');
    await frame.getByLabel('Card Number').fill('4111111111111111');
    await frame.getByLabel('Expiry').fill('12/26');
    await frame.getByLabel('CVV').fill('123');

    const [orderResp] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/orders') && r.status() === 201),
      page.getByRole('button', { name: 'Place Order' }).click(),
    ]);
    const data = await orderResp.json();
    orderId = data.id;
    console.log('Order:', orderId);

    await expect(page.getByText('Order Confirmed!')).toBeVisible();
    await expect(page.getByText(orderId)).toBeVisible();
  });

  test('4. verify order in account', async ({ page }) => {
    await page.goto('/account/orders');
    await expect(page.getByText(orderId)).toBeVisible();
    await page.locator('tr', { hasText: orderId }).getByRole('link', { name: 'View' }).click();
    await expect(page.getByText('Pending')).toBeVisible();
  });
});
```

---

*All 20 chapters now include complete, runnable TypeScript programs.*
*Total coverage: Architecture â†’ Setup â†’ TypeScript â†’ Locators â†’ UI â†’ Waits â†’ Tabs â†’ POM â†’ Assertions â†’ Screenshots â†’ Data â†’ API â†’ Cross-Browser â†’ Parallel â†’ Reports â†’ CI/CD â†’ Debug â†’ Best Practices â†’ Interview Q&A*
