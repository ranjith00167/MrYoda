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
