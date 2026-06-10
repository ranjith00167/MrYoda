# Playwright with TypeScript: Complete Training Notes

## 1. Introduction

### What is Playwright
- Playwright is an open-source automation framework developed by Microsoft for end-to-end testing of web applications.
- It supports multiple browsers (Chromium, Firefox, WebKit) and provides APIs for modern web testing needs.
- Unlike Selenium, Playwright runs browser code directly, offering faster and more reliable tests.

### Why Playwright over Selenium / Cypress
- **Faster execution**: Direct browser integration eliminates WebDriver overhead.
- **Cross-browser support**: Native support for all major browsers without external drivers.
- **Modern features**: Built-in handling for single-page apps, network interception, and mobile emulation.
- **Better reliability**: Auto-waiting mechanisms reduce flaky tests compared to Selenium's manual waits.
- **API testing**: Integrated support for API testing alongside UI tests, unlike Cypress which focuses only on UI.
- **Community and support**: Backed by Microsoft with active development and strong community.

### Key Features of Playwright
- Cross-platform testing (Windows, macOS, Linux).
- Mobile device emulation and responsive testing.
- Network interception and mocking.
- Screenshot and video recording for debugging.
- Parallel test execution out of the box.
- Trace viewer for detailed test execution analysis.
- TypeScript support for better type safety and developer experience.

### Companies Using Playwright
- Microsoft (as creators and users).
- GitHub for their web platform testing.
- Netflix for UI automation.
- Adobe for cross-browser compatibility testing.
- Many startups and enterprises for modern web app testing.

### Architecture Overview
- **BrowserContext**: Isolated browser sessions with their own cookies, localStorage, and pages.
- **Page**: Represents a single tab or window in the browser.
- **Locator**: Modern way to find elements using user-visible attributes instead of selectors.
- **Test Runner**: Built-in test runner with fixtures and parallel execution.
- **Auto-waiting**: Automatic waiting for elements to be ready before actions.

## 2. Setup & Installation

### Node.js & npm Prerequisites
- Ensure Node.js (version 16 or higher) is installed.
- npm comes bundled with Node.js.
- Verify installation: `node --version` and `npm --version`.

### Installing Playwright with TypeScript
```bash
# Create a new project directory
mkdir playwright-ts-project
cd playwright-ts-project

# Initialize npm project
npm init -y

# Install Playwright with TypeScript
npm install @playwright/test typescript

# Install browsers (optional, but recommended)
npx playwright install
```

### Folder Structure Explanation
```
playwright-ts-project/
├── tests/
│   ├── example.spec.ts    # Test files
├── playwright.config.ts   # Configuration file
├── package.json
├── tsconfig.json          # TypeScript configuration
└── node_modules/
```

### playwright.config.ts Explained Line by Line
```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',           // Directory where tests are located
  fullyParallel: true,         // Run tests in parallel
  forbidOnly: !!process.env.CI, // Fail if test.only is used in CI
  retries: process.env.CI ? 2 : 0, // Retry failed tests in CI
  workers: process.env.CI ? 1 : undefined, // Number of parallel workers
  reporter: 'html',            // Generate HTML reports
  use: {
    baseURL: 'https://example.com', // Base URL for all tests
    trace: 'on-first-retry',    // Capture traces on first retry
    screenshot: 'only-on-failure', // Take screenshots on failure
  },
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
  ],
});
```

### Running the First Test
```bash
# Run all tests
npx playwright test

# Run specific test file
npx playwright test example.spec.ts

# Run in headed mode (visible browser)
npx playwright test --headed

# Generate HTML report
npx playwright show-report
```

## 3. TypeScript Basics for Playwright

### TypeScript vs JavaScript
- TypeScript is a superset of JavaScript that adds static typing.
- Provides better IDE support, early error detection, and improved maintainability.
- In Playwright, TypeScript helps catch type errors in test code and provides auto-completion.

### tsconfig.json Explanation
```json
{
  "compilerOptions": {
    "target": "ES2020",              // Compile to modern JavaScript
    "module": "commonjs",            // Module system
    "strict": true,                  // Enable strict type checking
    "esModuleInterop": true,         // Better import/export handling
    "skipLibCheck": true,            // Skip type checking of declaration files
    "forceConsistentCasingInFileNames": true,
    "outDir": "./dist",              // Output directory
    "rootDir": "./",                 // Source directory
    "resolveJsonModule": true        // Allow importing JSON files
  },
  "include": ["**/*.ts"],             // Include TypeScript files
  "exclude": ["node_modules"]         // Exclude node_modules
}
```

### Types, Interfaces Used in Playwright
```typescript
import { Page, BrowserContext, Locator, expect } from '@playwright/test';

// Page represents a browser tab
const page: Page;

// BrowserContext represents an isolated session
const context: BrowserContext;

// Locator represents a way to find elements
const button: Locator = page.getByRole('button', { name: 'Submit' });

// Custom interface for test data
interface User {
  name: string;
  email: string;
  age: number;
}
```

### async / await Explained Simply
- `async` marks a function as asynchronous.
- `await` pauses execution until a Promise resolves.
- Playwright APIs return Promises, so we use async/await for clean, synchronous-looking code.

```typescript
// Without async/await (callback hell)
page.click('button').then(() => {
  return page.fill('input', 'text');
}).then(() => {
  console.log('Done');
});

// With async/await (clean and readable)
test('example test', async ({ page }) => {
  await page.click('button');
  await page.fill('input', 'text');
  console.log('Done');
});
```

## 4. Writing First Test

### test(), expect()
```typescript
import { test, expect } from '@playwright/test';

test('my first test', async ({ page }) => {
  // Navigate to a page
  await page.goto('https://example.com');
  
  // Assert page title
  await expect(page).toHaveTitle('Example Domain');
});
```

### Using browser, context, page
```typescript
test('using browser context and page', async ({ browser }) => {
  // Create a new browser context (isolated session)
  const context = await browser.newContext();
  
  // Create a new page in the context
  const page = await context.newPage();
  
  // Use the page for testing
  await page.goto('https://example.com');
  
  // Clean up
  await context.close();
});
```

### Page Lifecycle
- **Navigation**: `page.goto(url)` loads a new page.
- **Interaction**: Click buttons, fill forms, etc.
- **Assertion**: Check expected state.
- **Cleanup**: Context and browser close automatically in most cases.

### Assertions in Playwright
```typescript
test('various assertions', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Text content assertion
  await expect(page.locator('h1')).toHaveText('Example Domain');
  
  // Visibility assertion
  await expect(page.locator('h1')).toBeVisible();
  
  // URL assertion
  await expect(page).toHaveURL('https://example.com/');
  
  // Element count assertion
  await expect(page.locator('p')).toHaveCount(2);
});
```

## 5. Locators (Very Detailed)

### getByRole
```typescript
// Find by ARIA role
const button = page.getByRole('button', { name: 'Submit' });
const heading = page.getByRole('heading', { name: 'Welcome' });
const checkbox = page.getByRole('checkbox', { name: 'Agree to terms' });
```

### getByText
```typescript
// Find by visible text content
const link = page.getByText('Click here');
const paragraph = page.getByText('Welcome to our site', { exact: false });
```

### getByPlaceholder
```typescript
// Find input by placeholder text
const searchInput = page.getByPlaceholder('Search...');
```

### getByLabel
```typescript
// Find input by associated label
const emailInput = page.getByLabel('Email address');
```

### CSS Selectors
```typescript
// CSS selector locators
const element = page.locator('#my-id');
const elements = page.locator('.my-class');
const nested = page.locator('div > span');
const attribute = page.locator('[data-testid="submit-button"]');
```

### XPath (When and When Not to Use)
```typescript
// XPath locators (use sparingly, prefer CSS or role-based locators)
const element = page.locator('//button[text()="Submit"]');
const byAttribute = page.locator('//*[@id="my-id"]');

// When to use XPath:
// - Complex hierarchical selections
// - When CSS selectors can't target the element
// - Legacy applications with poor accessibility

// When NOT to use XPath:
// - Simple selections (use getByRole, getByText, etc.)
// - Performance-critical tests (XPath is slower than CSS)
```

### Best Locator Practices
- Prefer user-facing locators (getByRole, getByText, getByLabel) over implementation details.
- Use data-testid attributes for elements without good semantic markup.
- Avoid fragile selectors like complex XPath or CSS with deep nesting.
- Test locators in browser dev tools first.
- Keep locators readable and maintainable.

## 6. Handling UI Elements

### Textboxes
```typescript
test('textbox interactions', async ({ page }) => {
  await page.goto('https://example.com/form');
  
  // Fill a textbox
  await page.getByLabel('First Name').fill('John');
  
  // Clear and type slowly
  await page.getByLabel('Last Name').clear();
  await page.getByLabel('Last Name').type('Doe', { delay: 100 });
  
  // Get value
  const value = await page.getByLabel('First Name').inputValue();
  expect(value).toBe('John');
});
```

### Buttons
```typescript
test('button interactions', async ({ page }) => {
  await page.goto('https://example.com/buttons');
  
  // Click a button
  await page.getByRole('button', { name: 'Submit' }).click();
  
  // Double click
  await page.getByRole('button', { name: 'Double Click Me' }).dblclick();
  
  // Right click (context menu)
  await page.getByRole('button', { name: 'Right Click Me' }).click({ button: 'right' });
});
```

### Dropdowns
```typescript
test('dropdown interactions', async ({ page }) => {
  await page.goto('https://example.com/dropdown');
  
  // Select by visible text
  await page.getByLabel('Country').selectOption('United States');
  
  // Select by value
  await page.getByLabel('Country').selectOption({ value: 'us' });
  
  // Select multiple options (if multi-select)
  await page.getByLabel('Skills').selectOption(['JavaScript', 'TypeScript']);
  
  // Get selected value
  const selectedValue = await page.getByLabel('Country').inputValue();
  expect(selectedValue).toBe('us');
});
```

### Checkboxes & Radio Buttons
```typescript
test('checkbox and radio interactions', async ({ page }) => {
  await page.goto('https://example.com/form');
  
  // Check a checkbox
  await page.getByRole('checkbox', { name: 'Subscribe to newsletter' }).check();
  
  // Uncheck a checkbox
  await page.getByRole('checkbox', { name: 'Subscribe to newsletter' }).uncheck();
  
  // Verify checkbox state
  const isChecked = await page.getByRole('checkbox', { name: 'Subscribe to newsletter' }).isChecked();
  expect(isChecked).toBe(true);
  
  // Select a radio button
  await page.getByRole('radio', { name: 'Male' }).check();
});
```

### Alerts, Confirms, Prompts
```typescript
test('handling dialogs', async ({ page }) => {
  await page.goto('https://example.com/dialogs');
  
  // Listen for dialog and accept it
  page.on('dialog', async dialog => {
    expect(dialog.message()).toBe('Are you sure?');
    await dialog.accept(); // or dialog.dismiss() to cancel
  });
  
  // Trigger the dialog
  await page.getByRole('button', { name: 'Delete' }).click();
});
```

### Frames & Iframes
```typescript
test('handling frames', async ({ page }) => {
  await page.goto('https://example.com/frames');
  
  // Switch to frame by locator
  const frame = page.frameLocator('#my-iframe');
  
  // Interact with elements inside the frame
  await frame.getByLabel('Username').fill('user');
  await frame.getByRole('button', { name: 'Login' }).click();
  
  // Switch back to main frame (usually automatic)
  // await page.locator('#main-content').click();
});
```

## 7. Wait Mechanisms

### Auto-waiting
- Playwright automatically waits for elements to be ready before actions.
- No need for explicit waits in most cases.
- Actions like click() wait for the element to be visible and enabled.

### Explicit Waits
```typescript
test('explicit waits', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Wait for element to be visible
  await page.locator('#dynamic-element').waitFor({ state: 'visible' });
  
  // Wait for element to disappear
  await page.locator('#loading-spinner').waitFor({ state: 'hidden' });
  
  // Wait for custom condition
  await page.waitForFunction(() => {
    return document.querySelector('#counter').textContent === '10';
  });
});
```

### waitForSelector
```typescript
test('waitForSelector examples', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Wait for selector to appear
  await page.waitForSelector('#my-element');
  
  // Wait with timeout
  await page.waitForSelector('#my-element', { timeout: 10000 });
  
  // Wait for selector to disappear
  await page.waitForSelector('#my-element', { state: 'hidden' });
});
```

### waitForLoadState
```typescript
test('waitForLoadState examples', async ({ page }) => {
  // Wait for DOM to be ready
  await page.goto('https://example.com', { waitUntil: 'domcontentloaded' });
  
  // Wait for page to be fully loaded (default)
  await page.goto('https://example.com', { waitUntil: 'load' });
  
  // Wait for network to be idle
  await page.goto('https://example.com', { waitUntil: 'networkidle' });
  
  // Explicit wait for load state
  await page.waitForLoadState('networkidle');
});
```

### Common Wait Issues & Fixes
- **Issue**: Element not found immediately after navigation.
  **Fix**: Use `waitUntil: 'domcontentloaded'` in goto().
  
- **Issue**: AJAX content not loaded.
  **Fix**: Wait for specific element or use `waitForLoadState('networkidle')`.
  
- **Issue**: Animation interfering with clicks.
  **Fix**: Wait for element to be stable using `waitFor()` with custom condition.
  
- **Issue**: Race conditions between actions.
  **Fix**: Use sequential awaits or wait for expected state changes.

## 8. Handling Multiple Pages & Tabs

### New Tab Handling
```typescript
test('handling new tabs', async ({ context }) => {
  const page = await context.newPage();
  await page.goto('https://example.com');
  
  // Click link that opens in new tab
  const [newPage] = await Promise.all([
    context.waitForEvent('page'), // Wait for new page event
    page.getByText('Open in new tab').click() // Trigger new tab
  ]);
  
  // Switch to new tab
  await newPage.waitForLoadState();
  
  // Interact with new tab
  expect(await newPage.title()).toBe('New Tab Title');
});
```

### Window Switching
```typescript
test('window switching', async ({ context }) => {
  const page1 = await context.newPage();
  await page1.goto('https://example.com/page1');
  
  const page2 = await context.newPage();
  await page2.goto('https://example.com/page2');
  
  // Switch between windows
  await page1.bringToFront();
  await page1.getByText('Button on page 1').click();
  
  await page2.bringToFront();
  await page2.getByText('Button on page 2').click();
});
```

### Popups
```typescript
test('handling popups', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Handle popup window
  const [popup] = await Promise.all([
    page.waitForEvent('popup'), // Wait for popup event
    page.getByText('Open Popup').click() // Trigger popup
  ]);
  
  // Interact with popup
  await popup.waitForLoadState();
  await popup.getByLabel('Name').fill('John Doe');
  await popup.getByRole('button', { name: 'Submit' }).click();
  
  // Close popup
  await popup.close();
});
```

## 9. Test Organization

### test.describe
```typescript
import { test } from '@playwright/test';

test.describe('User Management', () => {
  test('should create new user', async ({ page }) => {
    // Test implementation
  });
  
  test('should edit existing user', async ({ page }) => {
    // Test implementation
  });
  
  test('should delete user', async ({ page }) => {
    // Test implementation
  });
});
```

### beforeAll, afterAll
```typescript
test.describe('Database Tests', () => {
  test.beforeAll(async () => {
    // Setup database connection
    // Create test data
    // One time setup before all tests in this describe block
  });
  
  test.afterAll(async () => {
    // Cleanup database
    // Close connections
    // One time cleanup after all tests
  });
  
  test('test 1', async () => {
    // Test implementation
  });
  
  test('test 2', async () => {
    // Test implementation
  });
});
```

### beforeEach, afterEach
```typescript
test.describe('Login Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to login page
    await page.goto('/login');
    // Common setup for each test
  });
  
  test.afterEach(async ({ page }) => {
    // Logout if logged in
    // Clear cookies if needed
    // Cleanup after each test
  });
  
  test('valid login', async ({ page }) => {
    await page.getByLabel('Username').fill('user');
    await page.getByLabel('Password').fill('pass');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page).toHaveURL('/dashboard');
  });
  
  test('invalid login', async ({ page }) => {
    await page.getByLabel('Username').fill('invalid');
    await page.getByLabel('Password').fill('invalid');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByText('Invalid credentials')).toBeVisible();
  });
});
```

### Fixtures
```typescript
// fixtures.ts
import { test as base } from '@playwright/test';

type TestFixtures = {
  authenticatedPage: Page;
};

export const test = base.extend<TestFixtures>({
  authenticatedPage: async ({ browser }, use) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    
    // Login logic
    await page.goto('/login');
    await page.getByLabel('Username').fill('testuser');
    await page.getByLabel('Password').fill('password');
    await page.getByRole('button', { name: 'Login' }).click();
    
    await use(page); // Provide the fixture to the test
    
    await context.close(); // Cleanup
  },
});

// test file
test('authenticated test', async ({ authenticatedPage }) => {
  await authenticatedPage.goto('/dashboard');
  await expect(authenticatedPage.getByText('Welcome')).toBeVisible();
});
```

### Page Object Model (POM) with TypeScript
```typescript
// pages/LoginPage.ts
export class LoginPage {
  constructor(private page: Page) {}
  
  async goto() {
    await this.page.goto('/login');
  }
  
  async login(username: string, password: string) {
    await this.page.getByLabel('Username').fill(username);
    await this.page.getByLabel('Password').fill(password);
    await this.page.getByRole('button', { name: 'Login' }).click();
  }
  
  async getErrorMessage() {
    return this.page.getByText('Invalid credentials').textContent();
  }
}

// pages/DashboardPage.ts
export class DashboardPage {
  constructor(private page: Page) {}
  
  async getWelcomeMessage() {
    return this.page.getByText('Welcome').textContent();
  }
  
  async logout() {
    await this.page.getByRole('button', { name: 'Logout' }).click();
  }
}

// test file
test('login flow', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const dashboardPage = new DashboardPage(page);
  
  await loginPage.goto();
  await loginPage.login('user', 'pass');
  
  await expect(page).toHaveURL('/dashboard');
  expect(await dashboardPage.getWelcomeMessage()).toContain('Welcome');
});
```

### Example Framework Design
```
playwright-framework/
├── pages/                 # Page Object classes
│   ├── BasePage.ts       # Common functionality
│   ├── LoginPage.ts
│   └── DashboardPage.ts
├── tests/                 # Test files
│   ├── auth/
│   │   ├── login.spec.ts
│   │   └── logout.spec.ts
│   └── e2e/
│       └── user-journey.spec.ts
├── fixtures/              # Custom fixtures
│   └── authenticated.ts
├── utils/                 # Helper utilities
│   ├── api-helpers.ts
│   └── test-data.ts
├── playwright.config.ts   # Configuration
├── package.json
└── tsconfig.json
```

## 10. Assertions (Deep Dive)

### Soft vs Hard Assertions
```typescript
import { test, expect } from '@playwright/test';

test('hard vs soft assertions', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Hard assertion - stops test on failure
  await expect(page).toHaveTitle('Example Domain');
  
  // Soft assertion - continues test, collects failures
  await expect.soft(page.locator('h1')).toHaveText('Wrong Text');
  await expect.soft(page.locator('p')).toHaveCount(3);
  
  // Check if any soft assertions failed
  expect(test.info().errors).toHaveLength(0); // This will fail if soft assertions failed
});
```

### UI Assertions
```typescript
test('UI assertions', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Visibility
  await expect(page.locator('h1')).toBeVisible();
  await expect(page.locator('.hidden')).toBeHidden();
  
  // Text content
  await expect(page.locator('h1')).toHaveText('Example Domain');
  await expect(page.locator('p')).toContainText('example');
  
  // Attributes
  await expect(page.locator('img')).toHaveAttribute('alt', 'Example image');
  
  // CSS properties
  await expect(page.locator('button')).toHaveCSS('background-color', 'rgb(0, 123, 255)');
  
  // Element state
  await expect(page.locator('input')).toBeEnabled();
  await expect(page.locator('input[disabled]')).toBeDisabled();
  await expect(page.locator('input[type="checkbox"]')).toBeChecked();
});
```

### URL & Title Verification
```typescript
test('URL and title verification', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Exact URL match
  await expect(page).toHaveURL('https://example.com/');
  
  // URL contains
  await expect(page).toHaveURL(/example\.com/);
  
  // Title assertions
  await expect(page).toHaveTitle('Example Domain');
  await expect(page).toHaveTitle(/Example/);
});
```

### Custom Assertions
```typescript
// Custom assertion function
async function expectElementToHaveColor(locator: Locator, expectedColor: string) {
  const actualColor = await locator.evaluate(el => getComputedStyle(el).color);
  expect(actualColor).toBe(expectedColor);
}

test('custom assertions', async ({ page }) => {
  await page.goto('https://example.com');
  
  const button = page.getByRole('button', { name: 'Submit' });
  await expectElementToHaveColor(button, 'rgb(255, 0, 0)');
  
  // Custom matcher with timeout
  await expect.poll(async () => {
    return await page.locator('#counter').textContent();
  }).toBe('10');
});
```

## 11. Screenshots, Videos, Traces

### Screenshot Options
```typescript
test('screenshot examples', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Full page screenshot
  await page.screenshot({ path: 'fullpage.png' });
  
  // Element screenshot
  await page.locator('header').screenshot({ path: 'header.png' });
  
  // Screenshot with clipping
  await page.screenshot({ 
    path: 'clipped.png',
    clip: { x: 0, y: 0, width: 800, height: 600 }
  });
  
  // Screenshot on failure (configured in playwright.config.ts)
});
```

### Video Recording
```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    video: 'on-first-retry', // 'on', 'off', 'retain-on-failure', 'on-first-retry'
  },
});

// In test
test('video recording', async ({ page }) => {
  await page.goto('https://example.com');
  // Test actions - video will be recorded automatically
  await page.getByRole('button').click();
});
```

### Trace Viewer
```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    trace: 'on-first-retry', // 'on', 'off', 'retain-on-failure', 'on-first-retry'
  },
});

// After test run
// View traces
npx playwright show-trace test-results/*/trace.zip
```

### Debugging Failed Tests
```typescript
test('debugging failed test', async ({ page }) => {
  await page.goto('https://example.com');
  
  // Pause execution for debugging
  await page.pause();
  
  // Add console logging
  page.on('console', msg => console.log('PAGE LOG:', msg.text()));
  
  // Add network monitoring
  page.on('request', request => console.log('Request:', request.url()));
  page.on('response', response => console.log('Response:', response.url(), response.status()));
  
  await page.getByRole('button').click();
});
```

## 12. Test Data Handling

### JSON Test Data
```typescript
// test-data/users.json
{
  "validUser": {
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  },
  "adminUser": {
    "username": "admin",
    "password": "admin123",
    "email": "admin@example.com"
  }
}

// test-data/products.json
[
  {
    "name": "Laptop",
    "price": 999.99,
    "category": "Electronics"
  },
  {
    "name": "Book",
    "price": 19.99,
    "category": "Education"
  }
]

// Usage in test
import users from '../test-data/users.json';
import products from '../test-data/products.json';

test('login with test data', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Username').fill(users.validUser.username);
  await page.getByLabel('Password').fill(users.validUser.password);
  await page.getByRole('button', { name: 'Login' }).click();
});
```

### Environment Variables
```typescript
// .env file
BASE_URL=https://staging.example.com
USERNAME=testuser
PASSWORD=password123
API_KEY=your-api-key

// playwright.config.ts
import 'dotenv/config';

export default defineConfig({
  use: {
    baseURL: process.env.BASE_URL,
  },
});

// In test
test('using env variables', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Username').fill(process.env.USERNAME!);
  await page.getByLabel('Password').fill(process.env.PASSWORD!);
  await page.getByRole('button', { name: 'Login' }).click();
});
```

### .env Usage
```typescript
// .env
TEST_ENV=staging
BROWSER=headless
TIMEOUT=30000

// playwright.config.ts
export default defineConfig({
  timeout: parseInt(process.env.TIMEOUT || '30000'),
  use: {
    headless: process.env.BROWSER === 'headless',
  },
  projects: [
    {
      name: 'staging',
      use: { baseURL: 'https://staging.example.com' },
      testMatch: process.env.TEST_ENV === 'staging' ? '**/*.spec.ts' : '**/smoke.spec.ts',
    },
  ],
});
```

### Dynamic Data Generation
```typescript
// utils/test-data-generator.ts
export class TestDataGenerator {
  static generateRandomEmail(): string {
    const random = Math.random().toString(36).substring(2, 15);
    return `test${random}@example.com`;
  }
  
  static generateRandomName(): string {
    const names = ['John', 'Jane', 'Bob', 'Alice', 'Charlie'];
    return names[Math.floor(Math.random() * names.length)];
  }
  
  static generateRandomPhone(): string {
    return `+1${Math.floor(Math.random() * 9000000000) + 1000000000}`;
  }
}

// Usage in test
test('register new user', async ({ page }) => {
  const email = TestDataGenerator.generateRandomEmail();
  const name = TestDataGenerator.generateRandomName();
  
  await page.goto('/register');
  await page.getByLabel('Name').fill(name);
  await page.getByLabel('Email').fill(email);
  await page.getByRole('button', { name: 'Register' }).click();
  
  await expect(page.getByText('Registration successful')).toBeVisible();
});
```

## 13. API Testing with Playwright

### GET, POST, PUT, DELETE Examples
```typescript
test('API testing examples', async ({ request }) => {
  // GET request
  const getResponse = await request.get('/api/users');
  expect(getResponse.ok()).toBeTruthy();
  const users = await getResponse.json();
  expect(users).toHaveLength(10);
  
  // POST request
  const newUser = { name: 'John Doe', email: 'john@example.com' };
  const postResponse = await request.post('/api/users', {
    data: newUser
  });
  expect(postResponse.status()).toBe(201);
  const createdUser = await postResponse.json();
  expect(createdUser.name).toBe('John Doe');
  
  // PUT request
  const updateData = { name: 'John Smith' };
  const putResponse = await request.put(`/api/users/${createdUser.id}`, {
    data: updateData
  });
  expect(putResponse.status()).toBe(200);
  
  // DELETE request
  const deleteResponse = await request.delete(`/api/users/${createdUser.id}`);
  expect(deleteResponse.status()).toBe(204);
});
```

### API Assertions
```typescript
test('API assertions', async ({ request }) => {
  // Response status assertions
  const response = await request.get('/api/users/1');
  expect(response.status()).toBe(200);
  expect(response.ok()).toBeTruthy(); // 200-299 range
  
  // Response headers
  expect(response.headers()['content-type']).toContain('application/json');
  
  // Response body assertions
  const user = await response.json();
  expect(user).toHaveProperty('id');
  expect(user).toHaveProperty('name');
  expect(user.name).toBe('John Doe');
  expect(user.email).toMatch(/^[^\s@]+@[^\s@]+\.[^\s@]+$/); // Email regex
  
  // Response time assertion
  expect(response.request().timing().responseEnd).toBeLessThan(1000); // Less than 1 second
});
```

### UI + API Combined Testing
```typescript
test('UI and API combined', async ({ page, request }) => {
  // Create user via API
  const newUser = { name: 'Jane Doe', email: 'jane@example.com' };
  const apiResponse = await request.post('/api/users', { data: newUser });
  const userId = (await apiResponse.json()).id;
  
  // Verify user appears in UI
  await page.goto('/users');
  await expect(page.getByText('Jane Doe')).toBeVisible();
  
  // Edit user via UI
  await page.getByText('Jane Doe').click();
  await page.getByLabel('Name').fill('Jane Smith');
  await page.getByRole('button', { name: 'Save' }).click();
  
  // Verify change via API
  const updatedUserResponse = await request.get(`/api/users/${userId}`);
  const updatedUser = await updatedUserResponse.json();
  expect(updatedUser.name).toBe('Jane Smith');
});
```

## 14. Cross-Browser Testing

### Chromium, Firefox, WebKit
```typescript
// playwright.config.ts
export default defineConfig({
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
  ],
});

// Running specific browser
npx playwright test --project=firefox
npx playwright test --project=chromium
npx playwright test --project=webkit
```

### Device Emulation
```typescript
// playwright.config.ts
export default defineConfig({
  projects: [
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'tablet',
      use: { ...devices['iPad Pro'] },
    },
  ],
});

// Custom device configuration
const customDevice = {
  name: 'Custom Device',
  userAgent: 'Custom/1.0',
  viewport: { width: 1280, height: 720 },
  deviceScaleFactor: 1,
  isMobile: false,
  hasTouch: false,
  defaultBrowserType: 'chromium',
};

export default defineConfig({
  projects: [
    {
      name: 'custom',
      use: { ...customDevice },
    },
  ],
});
```

### Mobile Testing Basics
```typescript
test('mobile testing', async ({ browser }) => {
  // Create mobile context
  const context = await browser.newContext({
    ...devices['iPhone 12'], // Use predefined device
    // Or custom mobile config
    viewport: { width: 375, height: 667 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15',
    isMobile: true,
    hasTouch: true,
  });
  
  const page = await context.newPage();
  await page.goto('https://example.com');
  
  // Test mobile-specific interactions
  await page.tap('button'); // Use tap instead of click for touch devices
  
  // Test responsive design
  await expect(page.locator('nav')).toBeVisible();
  
  await context.close();
});
```

## 15. Parallel Execution

### Workers
```typescript
// playwright.config.ts
export default defineConfig({
  workers: 4, // Number of parallel workers (default: number of CPU cores)
  
  // Or use different workers per project
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox', 
      use: { ...devices['Desktop Firefox'] },
      workers: 2, // Specific workers for this project
    },
  ],
});
```

### Sharding
```typescript
// playwright.config.ts
export default defineConfig({
  shard: { current: 1, total: 3 }, // Run 1st shard of 3 total
});

// Running with sharding
npx playwright test --shard=1/3  // Run first third of tests
npx playwright test --shard=2/3  // Run second third of tests
npx playwright test --shard=3/3  // Run final third of tests
```

### Retry Logic
```typescript
// playwright.config.ts
export default defineConfig({
  retries: 2, // Retry failed tests up to 2 times
  
  // Or conditional retries
  retries: process.env.CI ? 3 : 0, // More retries in CI
  
  // Custom retry logic
  use: {
    // Retry on specific errors
    actionTimeout: 10000,
    navigationTimeout: 30000,
  },
});

// Conditional retries in test
test('flaky test with custom retry', async ({ page }) => {
  // This test will be retried if it fails
  await page.goto('https://unreliable-site.com');
  await expect(page.locator('.unreliable-element')).toBeVisible();
});
```

### Timeout Settings
```typescript
// playwright.config.ts - Global timeouts
export default defineConfig({
  timeout: 60000, // Global test timeout (60 seconds)
  expect: {
    timeout: 10000, // Expect timeout (10 seconds)
  },
  use: {
    actionTimeout: 10000,    // Action timeout (10 seconds)
    navigationTimeout: 30000, // Navigation timeout (30 seconds)
  },
});

// Test-level timeouts
test('test with custom timeout', async ({ page }) => {
  test.setTimeout(120000); // 2 minutes for this specific test
  
  await page.goto('https://slow-site.com');
  await page.waitForSelector('#slow-element', { timeout: 60000 });
});
```

## 16. Reporting

### HTML Reports
```typescript
// playwright.config.ts
export default defineConfig({
  reporter: 'html', // Generate HTML reports
  
  // Or multiple reporters
  reporter: [
    ['html'],           // HTML report
    ['json', { outputFile: 'results.json' }], // JSON report
    ['junit', { outputFile: 'results.xml' }], // JUnit XML report
  ],
});

// Generate and view report
npx playwright test
npx playwright show-report
```

### Custom Reporters
```typescript
// custom-reporter.ts
import { Reporter } from '@playwright/test/reporter';

class CustomReporter implements Reporter {
  onTestEnd(test, result) {
    console.log(`Test ${test.title} finished with status: ${result.status}`);
    if (result.status === 'failed') {
      console.log(`Error: ${result.error?.message}`);
    }
  }
  
  onEnd(result) {
    console.log(`Test run finished: ${result.passed}/${result.total} tests passed`);
  }
}

export default CustomReporter;

// playwright.config.ts
export default defineConfig({
  reporter: [
    ['html'],
    ['./custom-reporter.ts'],
  ],
});
```

### CI-Friendly Reports
```typescript
// playwright.config.ts for CI
export default defineConfig({
  reporter: [
    ['html', { open: 'never' }], // Don't auto-open in CI
    ['junit', { outputFile: 'test-results.xml' }], // For Azure DevOps, Jenkins
    ['github'], // GitHub Actions reporter
  ],
  
  // CI-specific settings
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  
  // Output settings for CI
  outputDir: 'test-results/',
  snapshotDir: 'snapshots/',
});
```

## 17. CI/CD Integration

### GitHub Actions Example
```yaml
# .github/workflows/playwright.yml
name: Playwright Tests
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
    
    - name: Install dependencies
      run: npm ci
    
    - name: Install Playwright browsers
      run: npx playwright install --with-deps
    
    - name: Run Playwright tests
      run: npx playwright test
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: playwright-report
        path: playwright-report/
        retention-days: 30
```

### Jenkins Overview
```groovy
// Jenkins Pipeline
pipeline {
    agent any
    
    stages {
        stage('Setup') {
            steps {
                sh 'npm install'
                sh 'npx playwright install --with-deps'
            }
        }
        
        stage('Test') {
            steps {
                sh 'npx playwright test --reporter=junit'
            }
            post {
                always {
                    junit 'test-results.xml'
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'playwright-report',
                        reportFiles: 'index.html',
                        reportName: 'Playwright Report'
                    ])
                }
            }
        }
    }
}
```

### Headless Execution
```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    headless: true, // Run in headless mode (default in CI)
  },
});

// Command line override
npx playwright test --headed  // Force headed mode
npx playwright test --headless // Force headless mode
```

### Best CI Practices
- Use sharding for faster execution on multiple agents.
- Configure appropriate timeouts for CI environments.
- Use retry logic for flaky tests.
- Store and compare screenshots/artifacts.
- Integrate with test management tools.
- Set up proper notifications for failures.
- Use environment-specific configurations.
- Archive test artifacts for debugging.

## 18. Debugging & Troubleshooting

### Debug Mode
```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    // Enable debugging
    headless: false, // Run in headed mode
    slowMo: 1000,   // Slow down actions by 1 second
  },
});

// Run in debug mode
npx playwright test --debug

// Debug specific test
npx playwright test specific-test.spec.ts --debug
```

### PWDEBUG
```bash
# Set environment variable
PWDEBUG=1 npx playwright test

# Or set before running
export PWDEBUG=1
npx playwright test
```

### Slow Motion
```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    slowMo: 500, // Add 500ms delay between actions
  },
});

// Or in test
test('slow motion test', async ({ page }) => {
  await page.route('**/*', async route => {
    await new Promise(resolve => setTimeout(resolve, 100)); // Add delay
    await route.continue();
  });
  
  await page.goto('https://example.com');
});
```

### Common Errors & Solutions
- **TimeoutError**: Element not found within timeout.
  **Solution**: Increase timeout or check locator accuracy.
  
- **ElementHandle is disposed**: Element was removed from DOM.
  **Solution**: Wait for element stability or use different locator.
  
- **net::ERR_CONNECTION_REFUSED**: Cannot connect to application.
  **Solution**: Check if application is running and accessible.
  
- **Browser closed unexpectedly**: Browser crashed.
  **Solution**: Reduce parallel workers or check system resources.
  
- **Flaky tests**: Tests pass/fail randomly.
  **Solution**: Add proper waits, use retry logic, stabilize test data.

## 19. Best Practices

### Clean Code Practices
- Use descriptive test names and variable names.
- Keep tests focused on single responsibility.
- Use Page Object Model for reusable page logic.
- Avoid hard-coded waits; rely on auto-waiting.
- Group related tests in describe blocks.
- Use TypeScript interfaces for test data structures.

### Naming Conventions
```typescript
// Good test names
test('should display error message when login fails')
test('should redirect to dashboard after successful login')
test('should save user profile changes')

// Good variable names
const submitButton = page.getByRole('button', { name: 'Submit' });
const errorMessage = page.getByText('Invalid credentials');

// Good file names
// login.spec.ts, user-registration.spec.ts, checkout-flow.spec.ts
```

### Stability Tips
- Use stable locators (prefer role/text over CSS/XPath).
- Wait for elements to be in stable state before interacting.
- Handle async operations properly with await.
- Use fixtures for common setup/cleanup.
- Avoid dependencies between tests.
- Test on clean data state.

### Performance Tips
- Run tests in parallel when possible.
- Use sharding for large test suites.
- Minimize screenshot/video recording in development.
- Use selective test execution (--grep option).
- Optimize locator strategies for speed.
- Cache browser downloads in CI.

## 20. Interview Questions & Scenarios

### Beginner Questions
1. What is Playwright and how does it differ from Selenium?
2. How do you write your first Playwright test?
3. What are locators in Playwright and how do you use them?
4. Explain the difference between page, context, and browser.
5. How do you handle waiting in Playwright?

### Advanced Questions
1. How would you implement a Page Object Model in Playwright with TypeScript?
2. Explain Playwright's auto-waiting mechanism and when you might need explicit waits.
3. How do you handle API testing alongside UI testing in Playwright?
4. Describe your approach to cross-browser testing and parallel execution.
5. How do you debug failing tests in Playwright?

### Real Project Scenarios
**Scenario 1: E-commerce Checkout Flow**
```typescript
test.describe('E-commerce Checkout', () => {
  test('complete purchase flow', async ({ page }) => {
    // Navigate to product page
    await page.goto('/products/laptop');
    
    // Add to cart
    await page.getByRole('button', { name: 'Add to Cart' }).click();
    
    // Go to checkout
    await page.getByRole('link', { name: 'Checkout' }).click();
    
    // Fill shipping info
    await page.getByLabel('Full Name').fill('John Doe');
    await page.getByLabel('Address').fill('123 Main St');
    await page.getByLabel('City').fill('Anytown');
    await page.selectOption('select[name="state"]', 'CA');
    await page.getByLabel('ZIP Code').fill('12345');
    
    // Continue to payment
    await page.getByRole('button', { name: 'Continue to Payment' }).click();
    
    // Fill payment info
    await page.frameLocator('#payment-frame').getByLabel('Card Number').fill('4111111111111111');
    await page.frameLocator('#payment-frame').getByLabel('Expiration').fill('12/25');
    await page.frameLocator('#payment-frame').getByLabel('CVV').fill('123');
    
    // Complete purchase
    await page.getByRole('button', { name: 'Complete Purchase' }).click();
    
    // Verify success
    await expect(page.getByText('Order Confirmed')).toBeVisible();
    await expect(page).toHaveURL(/\/order-confirmation/);
  });
});
```

**Scenario 2: API-Driven UI Testing**
```typescript
test.describe('User Management System', () => {
  let testUserId: number;
  
  test.beforeAll(async ({ request }) => {
    // Create test user via API
    const response = await request.post('/api/users', {
      data: {
        name: 'Test User',
        email: `test${Date.now()}@example.com`,
        role: 'user'
      }
    });
    const user = await response.json();
    testUserId = user.id;
  });
  
  test('admin can edit user details', async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.getByLabel('Email').fill('admin@example.com');
    await page.getByLabel('Password').fill('adminpass');
    await page.getByRole('button', { name: 'Login' }).click();
    
    // Navigate to user management
    await page.getByRole('link', { name: 'User Management' }).click();
    
    // Find and edit test user
    await page.getByText('Test User').click();
    await page.getByLabel('Role').selectOption('admin');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    
    // Verify change via API
    const { request } = await import('@playwright/test');
    const apiRequest = await request.newContext();
    const userResponse = await apiRequest.get(`/api/users/${testUserId}`);
    const updatedUser = await userResponse.json();
    expect(updatedUser.role).toBe('admin');
  });
  
  test.afterAll(async ({ request }) => {
    // Cleanup test user
    await request.delete(`/api/users/${testUserId}`);
  });
});
```

**Scenario 3: Mobile-Responsive Testing**
```typescript
test.describe('Mobile Responsiveness', () => {
  test('navigation works on mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12'],
    });
    const page = await context.newPage();
    
    await page.goto('/');
    
    // Check mobile menu is present
    await expect(page.locator('.mobile-menu-toggle')).toBeVisible();
    
    // Open mobile menu
    await page.locator('.mobile-menu-toggle').tap();
    
    // Check menu items are visible
    await expect(page.getByRole('link', { name: 'Products' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'About' })).toBeVisible();
    
    // Navigate to products page
    await page.getByRole('link', { name: 'Products' }).tap();
    
    // Verify page loaded correctly
    await expect(page).toHaveURL('/products');
    await expect(page.getByRole('heading', { name: 'Our Products' })).toBeVisible();
    
    await context.close();
  });
});
```

### Framework Design Questions
1. How would you structure a large-scale Playwright framework?
2. What strategies do you use for test data management?
3. How do you handle environment-specific configurations?
4. Describe your approach to CI/CD integration for Playwright tests.
5. How do you ensure test maintainability and reusability?

---

**Note**: This comprehensive guide covers all major Playwright concepts with practical examples. Focus on understanding the core concepts and practicing with real applications. Remember that Playwright is constantly evolving, so stay updated with the official documentation. 