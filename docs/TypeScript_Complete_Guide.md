# TypeScript â€” Complete Training Guide (300+ Pages)

> **Target Audience:** Beginners, JavaScript developers, QA engineers, and mid-level developers preparing for interviews.
> **Prerequisites:** Basic JavaScript knowledge.

---

## Table of Contents

1. Introduction to TypeScript
2. Setup & Installation
3. Basic Types
4. Variables & Type Inference
5. Functions in TypeScript
6. Interfaces
7. Type Aliases
8. Classes & OOP
9. Generics
10. Enums
11. Advanced Types
12. Type Guards & Narrowing
13. Utility Types
14. Modules & Namespaces
15. Decorators
16. Async/Await & Promises
17. Error Handling
18. TypeScript with Node.js
19. TypeScript Configuration (tsconfig.json)
20. Interview Questions & Best Practices

---

# Chapter 1: Introduction to TypeScript

## 1.1 What is TypeScript?

TypeScript is a **strongly-typed superset of JavaScript** developed and maintained by **Microsoft**. It adds optional static typing to JavaScript and compiles down to plain JavaScript.

> Think of TypeScript as JavaScript with a safety net. It catches bugs while you type code â€” before your program even runs.

**Key points:**
- Every valid JavaScript file is also a valid TypeScript file
- TypeScript is compiled (transpiled) to JavaScript
- Browsers and Node.js run the compiled JavaScript â€” not TypeScript directly
- TypeScript adds: types, interfaces, generics, decorators, enums, and more

## 1.2 Why TypeScript?

### The Problem with JavaScript

```typescript
// JavaScript â€” No type safety
function calculateTotal(price, quantity) {
  return price * quantity;
}

calculateTotal(100, 5);       // 500 âœ…
calculateTotal("100", 5);     // "100100001000010000100001" ðŸ˜± Bug!
calculateTotal(100, "hello"); // NaN ðŸ˜± Silent failure!
```

### TypeScript Catches Bugs at Compile Time

```typescript
// TypeScript â€” Type safe
function calculateTotal(price: number, quantity: number): number {
  return price * quantity;
}

calculateTotal(100, 5);       // 500 âœ…
calculateTotal("100", 5);     // âŒ Error: Argument of type 'string' not assignable to 'number'
calculateTotal(100, "hello"); // âŒ Error: Argument of type 'string' not assignable to 'number'
```

### Benefits of TypeScript

| Benefit | Description |
|---|---|
| **Early bug detection** | Errors caught at compile time, not runtime |
| **IntelliSense** | VS Code auto-completes types, methods, properties |
| **Refactoring** | Rename a variable â€” all references update safely |
| **Self-documenting** | Types serve as inline documentation |
| **Scalability** | Large codebases become maintainable |
| **Better collaboration** | Teams understand each other's code via types |
| **Industry standard** | Used by Angular, NestJS, Playwright, and more |

## 1.3 TypeScript vs JavaScript

```typescript
// JavaScript
function greet(name) {
  return "Hello, " + name.toUpperCase();
}
greet(42); // Runtime error: name.toUpperCase is not a function

// TypeScript
function greet(name: string): string {
  return "Hello, " + name.toUpperCase();
}
greet(42); // Compile-time error â€” caught BEFORE running
```

## 1.4 How TypeScript Compilation Works

```
TypeScript (.ts files)
       â†“
TypeScript Compiler (tsc)
       â†“
JavaScript (.js files)
       â†“
Node.js / Browser runs the JavaScript
```

```typescript
// hello.ts (TypeScript source)
const message: string = "Hello, TypeScript!";
console.log(message);

// After tsc hello.ts â†’ hello.js (compiled output)
// const message = "Hello, TypeScript!";
// console.log(message);
```

## 1.5 Companies Using TypeScript

- **Microsoft** (TypeScript creator â€” VS Code, Azure)
- **Google** (Angular framework)
- **Meta** (React + TypeScript)
- **Airbnb**, **Slack**, **Asana**, **Lyft**
- **Playwright** (100% TypeScript)
- **NestJS**, **Prisma**, **TypeORM**

---

# Chapter 2: Setup & Installation

## 2.1 Prerequisites

```bash
# Verify Node.js is installed
node --version   # v18.x.x or higher
npm --version    # 9.x.x or higher
```

## 2.2 Installing TypeScript

```bash
# Install globally (available everywhere)
npm install -g typescript

# Verify installation
tsc --version    # Version 5.x.x

# Install locally in a project (recommended for teams)
npm install typescript --save-dev
```

## 2.3 Your First TypeScript Project

```bash
# Create project folder
mkdir typescript-training
cd typescript-training

# Initialize Node project
npm init -y

# Install TypeScript locally
npm install typescript --save-dev

# Create TypeScript config
npx tsc --init
```

## 2.4 tsconfig.json â€” Complete Explanation

```json
{
  "compilerOptions": {
    // â”€â”€â”€ Target & Output â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "target": "ES2020",           // Which JS version to compile to
    "module": "commonjs",          // Module system (commonjs for Node.js)
    "outDir": "./dist",            // Where compiled .js files go
    "rootDir": "./src",            // Where your .ts source files are

    // â”€â”€â”€ Type Checking â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "strict": true,                // Enable ALL strict checks (recommended)
    "strictNullChecks": true,      // null/undefined must be handled explicitly
    "strictFunctionTypes": true,   // Strict function parameter type checking
    "noImplicitAny": true,         // Error if type is inferred as 'any'
    "noImplicitThis": true,        // Error if 'this' has implicit 'any' type

    // â”€â”€â”€ Module Resolution â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "esModuleInterop": true,       // Allow default imports from CommonJS modules
    "allowSyntheticDefaultImports": true,
    "moduleResolution": "node",    // How modules are resolved
    "baseUrl": "./src",            // Base for non-relative imports
    "paths": {                     // Module path aliases
      "@utils/*": ["utils/*"],
      "@models/*": ["models/*"]
    },

    // â”€â”€â”€ Source Maps & Debugging â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "sourceMap": true,             // Generate .map files for debugging
    "declaration": true,           // Generate .d.ts type declaration files
    "declarationDir": "./types",   // Where .d.ts files go

    // â”€â”€â”€ Additional Checks â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "noUnusedLocals": true,        // Error on unused variables
    "noUnusedParameters": true,    // Error on unused function parameters
    "noImplicitReturns": true,     // All code paths must return a value
    "skipLibCheck": true,          // Skip type checking of node_modules

    // â”€â”€â”€ Experimental â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    "experimentalDecorators": true, // Enable decorators (needed for NestJS)
    "emitDecoratorMetadata": true
  },
  "include": ["src/**/*.ts"],       // Which files to compile
  "exclude": ["node_modules", "dist", "**/*.spec.ts"]
}
```

## 2.5 Project Folder Structure

```
typescript-training/
â”œâ”€â”€ src/                    # TypeScript source files
â”‚   â”œâ”€â”€ index.ts            # Entry point
â”‚   â”œâ”€â”€ models/             # Type definitions, interfaces
â”‚   â”‚   â””â”€â”€ user.ts
â”‚   â”œâ”€â”€ services/           # Business logic
â”‚   â”‚   â””â”€â”€ user-service.ts
â”‚   â””â”€â”€ utils/              # Helper functions
â”‚       â””â”€â”€ validators.ts
â”œâ”€â”€ dist/                   # Compiled JavaScript (auto-generated)
â”œâ”€â”€ tests/                  # Test files
â”œâ”€â”€ tsconfig.json           # TypeScript config
â””â”€â”€ package.json
```

## 2.6 npm Scripts for TypeScript

```json
{
  "scripts": {
    "build": "tsc",
    "build:watch": "tsc --watch",
    "start": "node dist/index.js",
    "dev": "ts-node src/index.ts",
    "clean": "rimraf dist"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "ts-node": "^10.0.0",
    "@types/node": "^20.0.0"
  }
}
```

```bash
# Run TypeScript directly (no compile step needed for development)
npm install ts-node --save-dev
npx ts-node src/index.ts
```

---

# Chapter 3: Basic Types

## 3.1 Primitive Types

```typescript
// â”€â”€â”€ string â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let firstName: string = "John";
let lastName: string = 'Doe';
let template: string = `Hello, ${firstName} ${lastName}!`; // Template literal

// â”€â”€â”€ number â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let age: number = 25;
let price: number = 99.99;
let hex: number = 0xFF;             // Hexadecimal
let binary: number = 0b1010;        // Binary
let octal: number = 0o755;          // Octal
let million: number = 1_000_000;    // Numeric separator (readable)

// â”€â”€â”€ boolean â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let isActive: boolean = true;
let isDeleted: boolean = false;

// â”€â”€â”€ null and undefined â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let nothing: null = null;
let notAssigned: undefined = undefined;

// With strict null checks (recommended):
let userName: string | null = null;     // Can be string OR null
let userAge: number | undefined;        // Can be number OR undefined
```

## 3.2 Array Types

```typescript
// Method 1: Type[]
let numbers: number[] = [1, 2, 3, 4, 5];
let names: string[] = ["Alice", "Bob", "Charlie"];
let flags: boolean[] = [true, false, true];

// Method 2: Array<Type>
let scores: Array<number> = [95, 87, 92];
let items: Array<string> = ["apple", "banana", "cherry"];

// Readonly array (cannot be modified)
let readonlyNums: readonly number[] = [1, 2, 3];
// readonlyNums.push(4); // âŒ Error: Property 'push' does not exist on readonly array

// Multi-dimensional array
let matrix: number[][] = [
  [1, 2, 3],
  [4, 5, 6],
  [7, 8, 9],
];

// Array of objects
interface Product { id: number; name: string; price: number; }
let products: Product[] = [
  { id: 1, name: "Laptop", price: 50000 },
  { id: 2, name: "Mouse",  price: 1500  },
];
```

## 3.3 Tuple Types

Tuples are arrays with a **fixed number of elements where each element has a known type**.

```typescript
// Tuple â€” fixed structure
let person: [string, number] = ["Alice", 30];
let person2: [string, number] = [30, "Alice"]; // âŒ Error: wrong order!

// Accessing tuple elements
const [name, age] = person;     // Destructuring
console.log(name);  // "Alice"
console.log(age);   // 30

// Named tuple (TypeScript 4.0+)
type Coordinate = [x: number, y: number, z?: number];
const point: Coordinate = [10, 20];
const point3D: Coordinate = [10, 20, 30];

// Tuple with rest elements
type StringsAndNumber = [string, string, ...number[]];
const data: StringsAndNumber = ["hello", "world", 1, 2, 3, 4, 5];

// Real-world use case: function returning multiple values
function parseDate(dateStr: string): [number, number, number] {
  const parts = dateStr.split('-').map(Number);
  return [parts[0], parts[1], parts[2]]; // [year, month, day]
}
const [year, month, day] = parseDate("2025-12-25");
```

## 3.4 Special Types

```typescript
// â”€â”€â”€ any â€” Opt out of type checking (avoid!) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let anything: any = 42;
anything = "now a string";   // No error
anything = true;              // No error
anything.toUpperCase();       // No error (but might fail at runtime)

// â”€â”€â”€ unknown â€” Type-safe version of any â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let input: unknown = "hello";
// input.toUpperCase();        // âŒ Error: must narrow type first
if (typeof input === "string") {
  console.log(input.toUpperCase()); // âœ… Safe after type check
}

// â”€â”€â”€ void â€” Function returns nothing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
function logMessage(msg: string): void {
  console.log(msg);
  // No return statement (or return; is fine)
}

// â”€â”€â”€ never â€” Function never returns â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
function throwError(message: string): never {
  throw new Error(message);
  // This point is never reached
}

function infiniteLoop(): never {
  while (true) {}
}

// â”€â”€â”€ object â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let obj: object = { key: "value" };
// obj.key  // âŒ Error: Property 'key' does not exist on type 'object'
// Use specific interface/type instead of 'object'

// â”€â”€â”€ symbol â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
const sym1: symbol = Symbol("id");
const sym2: symbol = Symbol("id");
console.log(sym1 === sym2); // false â€” symbols are always unique

// â”€â”€â”€ bigint â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
const bigNumber: bigint = 9007199254740992n;
const result: bigint = bigNumber + 1n;
```

## 3.5 Literal Types

```typescript
// String literal type â€” only one specific value allowed
let direction: "left" | "right" | "up" | "down";
direction = "left";    // âœ…
direction = "forward"; // âŒ Error

// Number literal type
let diceRoll: 1 | 2 | 3 | 4 | 5 | 6;
diceRoll = 3;  // âœ…
diceRoll = 7;  // âŒ Error

// Boolean literal
let alwaysTrue: true = true;

// Combining literal types in functions
function move(direction: "north" | "south" | "east" | "west", steps: number): string {
  return `Moving ${steps} steps ${direction}`;
}
move("north", 5);     // âœ…
move("diagonal", 3);  // âŒ Error

// Const assertion â€” narrows to literal type
const config = {
  host: "localhost",
  port: 3000,
} as const;
// config.host is type "localhost" (not string)
// config.port is type 3000 (not number)
// config.host = "other"; // âŒ Error: cannot reassign
```

---

# Chapter 4: Variables & Type Inference

## 4.1 var, let, const in TypeScript

```typescript
// var â€” function-scoped (avoid in TypeScript)
var oldStyle: string = "avoid this";

// let â€” block-scoped (use for mutable variables)
let counter: number = 0;
counter++;  // âœ…

// const â€” block-scoped, cannot be reassigned (use by default)
const MAX_SIZE: number = 100;
// MAX_SIZE = 200; // âŒ Error: Cannot assign to 'MAX_SIZE' (const)

// const with objects â€” object itself cannot be reassigned,
// but its properties CAN be mutated
const user = { name: "Alice", age: 25 };
user.name = "Bob";     // âœ… Mutating property is fine
user.age = 30;         // âœ…
// user = { name: "Charlie" }; // âŒ Cannot reassign const

// Readonly to prevent mutation too
const frozenUser: Readonly<{ name: string; age: number }> = { name: "Alice", age: 25 };
// frozenUser.name = "Bob"; // âŒ Cannot assign to 'name' (readonly)
```

## 4.2 Type Inference â€” TypeScript Guesses the Type

```typescript
// TypeScript infers types from initial values
let name = "Alice";           // inferred as string
let count = 42;               // inferred as number
let active = true;            // inferred as boolean
let items = [1, 2, 3];        // inferred as number[]
let mixed = [1, "hello"];     // inferred as (string | number)[]

// TypeScript infers return types of functions
function add(a: number, b: number) {
  return a + b;  // Return type inferred as number
}

function greet(name: string) {
  return `Hello, ${name}!`;  // Return type inferred as string
}

// Inference in destructuring
const point = { x: 10, y: 20 };
const { x, y } = point;  // x: number, y: number (inferred)

// Inference in array destructuring
const [first, second] = [1, "hello"];  // first: number, second: string
```

## 4.3 Type Assertions

```typescript
// Type assertion â€” "I know better than TypeScript"
// Use when TypeScript can't infer the type but you know what it is

// Method 1: as keyword (preferred)
const input = document.getElementById("username") as HTMLInputElement;
console.log(input.value);  // Now TypeScript knows it's an input

// Method 2: angle-bracket syntax (not usable in JSX/TSX)
const input2 = <HTMLInputElement>document.getElementById("username");

// Asserting unknown types
function processInput(input: unknown): string {
  return (input as string).toUpperCase();  // Assert and use
}

// Double assertion (use sparingly â€” bypass type system)
const value = "hello" as unknown as number; // Bypass â€” dangerous!

// Non-null assertion (!) â€” tells TypeScript "this is not null"
const element = document.getElementById("btn")!;  // ! means "trust me, it exists"
element.addEventListener("click", () => console.log("clicked"));
// Without ! you'd get: Object is possibly null

// Const assertion
const directions = ["north", "south", "east", "west"] as const;
// Type is: readonly ["north", "south", "east", "west"]
// Not: string[]
```

## 4.4 Destructuring with Types

```typescript
// Object destructuring with types
interface Config {
  host: string;
  port: number;
  ssl: boolean;
}

const config: Config = { host: "localhost", port: 3000, ssl: false };
const { host, port, ssl } = config;

// Destructuring with rename
const { host: serverHost, port: serverPort } = config;
console.log(serverHost, serverPort);

// Destructuring with default values
function setup({ host = "localhost", port = 3000 }: Partial<Config>): void {
  console.log(`Connecting to ${host}:${port}`);
}
setup({});                     // Uses defaults
setup({ host: "prod.com" });   // Custom host, default port

// Array destructuring with types
function getMinMax(numbers: number[]): [number, number] {
  return [Math.min(...numbers), Math.max(...numbers)];
}
const [min, max] = getMinMax([5, 1, 8, 3, 9, 2]);
console.log(min, max);  // 1, 9

// Rest in destructuring
const [first, ...rest] = [1, 2, 3, 4, 5];
// first: number = 1
// rest: number[] = [2, 3, 4, 5]

const { name, ...otherProps } = { name: "Alice", age: 25, city: "Mumbai" };
// name: string = "Alice"
// otherProps: { age: number; city: string }
```

---

# Chapter 5: Functions in TypeScript

## 5.1 Function Type Annotations

```typescript
// Basic function with types
function greet(name: string, age: number): string {
  return `Hello ${name}, you are ${age} years old.`;
}

// Arrow function with types
const multiply = (a: number, b: number): number => a * b;

// Function that returns void
const logError = (message: string): void => {
  console.error(`[ERROR] ${message}`);
};

// Function that never returns
const crash = (message: string): never => {
  throw new Error(message);
};
```

## 5.2 Optional and Default Parameters

```typescript
// Optional parameter â€” use ? suffix
function createUser(name: string, email: string, phone?: string): object {
  return {
    name,
    email,
    phone: phone ?? "Not provided", // ?? = nullish coalescing
  };
}

createUser("Alice", "alice@test.com");              // âœ… phone is optional
createUser("Bob", "bob@test.com", "+91-9876543210"); // âœ… with phone

// Default parameters
function createOrder(
  productId: string,
  quantity: number = 1,
  discount: number = 0
): object {
  return { productId, quantity, discount };
}

createOrder("PROD-001");           // quantity=1, discount=0
createOrder("PROD-001", 5);        // quantity=5, discount=0
createOrder("PROD-001", 5, 10);    // quantity=5, discount=10

// Important: Optional params must come AFTER required params
// function wrong(a?: string, b: string): void {} // âŒ Error
function correct(a: string, b?: string): void {}  // âœ…
```

## 5.3 Rest Parameters

```typescript
// Rest parameters â€” collect remaining arguments into an array
function sum(...numbers: number[]): number {
  return numbers.reduce((total, n) => total + n, 0);
}

console.log(sum(1, 2, 3));         // 6
console.log(sum(1, 2, 3, 4, 5));   // 15
console.log(sum());                 // 0

// Mix of regular and rest parameters
function buildUrl(base: string, ...paths: string[]): string {
  return [base, ...paths].join("/");
}

buildUrl("https://api.example.com", "users", "123", "orders");
// "https://api.example.com/users/123/orders"

// Rest with spread
function logAll(label: string, ...values: unknown[]): void {
  console.log(`[${label}]`, ...values);
}
logAll("Debug", "user:", { id: 1 }, "status:", "active");
```

## 5.4 Function Overloading

```typescript
// Overloading â€” same function name, different parameter types/counts
function process(input: string): string;             // Overload 1
function process(input: number): number;             // Overload 2
function process(input: string[]): string[];         // Overload 3
function process(input: string | number | string[]): string | number | string[] {
  // Implementation signature (not visible to callers)
  if (typeof input === "string") return input.toUpperCase();
  if (typeof input === "number") return input * 2;
  return input.map(s => s.toUpperCase());
}

const r1 = process("hello");     // Returns string: "HELLO"
const r2 = process(21);          // Returns number: 42
const r3 = process(["a", "b"]);  // Returns string[]: ["A", "B"]

// Real-world overload example
function createElement(tag: "a"): HTMLAnchorElement;
function createElement(tag: "div"): HTMLDivElement;
function createElement(tag: "input"): HTMLInputElement;
function createElement(tag: string): HTMLElement {
  return document.createElement(tag);
}

const link = createElement("a");   // Type: HTMLAnchorElement
const div = createElement("div");  // Type: HTMLDivElement
```

## 5.5 Function Types and Higher-Order Functions

```typescript
// Defining a function type
type Transformer<T, U> = (value: T) => U;
type Predicate<T> = (value: T) => boolean;
type EventHandler = (event: Event) => void;

// Using function types as parameters
function transform<T, U>(items: T[], fn: Transformer<T, U>): U[] {
  return items.map(fn);
}

const numbers = [1, 2, 3, 4, 5];
const doubled = transform(numbers, n => n * 2);      // [2, 4, 6, 8, 10]
const asStrings = transform(numbers, n => `${n}`);   // ["1","2","3","4","5"]

// Callback functions with types
function fetchData(
  url: string,
  onSuccess: (data: unknown) => void,
  onError: (error: Error) => void
): void {
  // Simulate async operation
  setTimeout(() => {
    try {
      onSuccess({ id: 1, name: "Alice" });
    } catch (err) {
      onError(new Error("Fetch failed"));
    }
  }, 1000);
}

fetchData(
  "/api/user",
  (data) => console.log("Got:", data),
  (err) => console.error("Failed:", err.message)
);

// Functions returning functions (closures)
function createMultiplier(factor: number): (value: number) => number {
  return (value: number) => value * factor;
}

const double = createMultiplier(2);
const triple = createMultiplier(3);

console.log(double(5));  // 10
console.log(triple(5));  // 15
```

## 5.6 Generic Functions

```typescript
// Generic function â€” works with any type
function identity<T>(value: T): T {
  return value;
}

identity<string>("hello");    // Returns string
identity<number>(42);         // Returns number
identity(true);               // TypeScript infers: boolean

// Generic with constraints
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

const user = { id: 1, name: "Alice", email: "alice@test.com" };
getProperty(user, "name");   // âœ… Returns string
getProperty(user, "id");     // âœ… Returns number
// getProperty(user, "phone"); // âŒ Error: "phone" not in type

// Array utility generics
function first<T>(arr: T[]): T | undefined {
  return arr[0];
}

function last<T>(arr: T[]): T | undefined {
  return arr[arr.length - 1];
}

function unique<T>(arr: T[]): T[] {
  return [...new Set(arr)];
}

const names = ["Alice", "Bob", "Alice", "Charlie"];
console.log(unique(names)); // ["Alice", "Bob", "Charlie"]
```

## 5.7 Arrow Functions vs Regular Functions

```typescript
// Arrow function â€” 'this' is lexically bound (from enclosing scope)
class Timer {
  private seconds: number = 0;

  start(): void {
    // Arrow function â€” 'this' correctly refers to Timer instance
    setInterval(() => {
      this.seconds++;  // âœ… 'this' is Timer
      console.log(this.seconds);
    }, 1000);
  }

  startBroken(): void {
    // Regular function â€” 'this' is undefined in strict mode
    setInterval(function() {
      // this.seconds++;  // âŒ 'this' is undefined!
    }, 1000);
  }
}

// Implicit return in arrow functions
const square = (n: number): number => n * n;
const isEven = (n: number): boolean => n % 2 === 0;
const getFullName = (first: string, last: string): string => `${first} ${last}`;

// Multi-line arrow function
const processUser = (id: number): string => {
  const prefix = id > 100 ? "VIP" : "Regular";
  return `${prefix}-${id}`;
};
```

## 5.8 Real-World Function Examples

```typescript
// Validation utility functions
function isValidEmail(email: string): boolean {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}

function isValidPhone(phone: string): boolean {
  return /^\+?[1-9]\d{9,14}$/.test(phone);
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

// Functional programming utilities
function pipe<T>(...fns: Array<(arg: T) => T>): (arg: T) => T {
  return (value: T) => fns.reduce((acc, fn) => fn(acc), value);
}

const processName = pipe<string>(
  s => s.trim(),
  s => s.toLowerCase(),
  s => s.charAt(0).toUpperCase() + s.slice(1)
);
console.log(processName("  ALICE  "));  // "Alice"

// Memoization
function memoize<T extends (...args: unknown[]) => unknown>(fn: T): T {
  const cache = new Map<string, unknown>();
  return ((...args: unknown[]) => {
    const key = JSON.stringify(args);
    if (cache.has(key)) return cache.get(key);
    const result = fn(...args);
    cache.set(key, result);
    return result;
  }) as T;
}

const expensiveCalculation = memoize((n: number): number => {
  console.log(`Computing for ${n}...`);
  return n * n;
});

expensiveCalculation(5);  // Computes: 25
expensiveCalculation(5);  // Returns from cache: 25 (no log)
expensiveCalculation(6);  // Computes: 36
```

---

*Part 1 complete â€” Chapters 1â€“5: Introduction, Setup, Basic Types, Variables, Functions*

---

# Chapter 6: Interfaces

## 6.1 What is an Interface?

An interface defines the **shape (structure) of an object** â€” what properties and methods it must have.

```typescript
// Define an interface
interface User {
  id: number;
  name: string;
  email: string;
}

// Use the interface
const user: User = {
  id: 1,
  name: "Alice",
  email: "alice@example.com",
};

// TypeScript ensures the object matches the interface
const invalid: User = {
  id: 1,
  name: "Bob",
  // email missing â†’ âŒ Error: Property 'email' is missing
};
```

## 6.2 Optional and Readonly Properties

```typescript
interface Product {
  id: number;
  name: string;
  price: number;
  description?: string;     // Optional (may or may not exist)
  readonly sku: string;      // Cannot be changed after creation
  readonly createdAt: Date;
}

const laptop: Product = {
  id: 1,
  name: "MacBook Pro",
  price: 150000,
  sku: "MBP-2024-001",
  createdAt: new Date(),
  // description is optional â€” OK to omit
};

// laptop.sku = "NEW-SKU"; // âŒ Error: Cannot assign to 'sku' (readonly)
laptop.price = 140000;     // âœ… price is not readonly

// Accessing optional property safely
const desc = laptop.description?.toUpperCase() ?? "No description";
```

## 6.3 Index Signatures

```typescript
// Interface with dynamic keys
interface StringMap {
  [key: string]: string;
}

const translations: StringMap = {
  hello: "à¤¨à¤®à¤¸à¥à¤¤à¥‡",
  goodbye: "à¤…à¤²à¤µà¤¿à¤¦à¤¾",
  thanks: "à¤§à¤¨à¥à¤¯à¤µà¤¾à¤¦",
};
translations["morning"] = "à¤¸à¥à¤ªà¥à¤°à¤­à¤¾à¤¤"; // âœ… Dynamic key

// Numeric index signature
interface NumberArray {
  [index: number]: string;
  length: number;
}

// Combining fixed and dynamic properties
interface Config {
  host: string;
  port: number;
  [key: string]: string | number; // Allow extra properties
}

const config: Config = {
  host: "localhost",
  port: 3000,
  timeout: 5000,   // Extra property â€” allowed by index signature
  region: "ap-south-1",
};
```

## 6.4 Interface Extending (Inheritance)

```typescript
interface Animal {
  name: string;
  age: number;
  makeSound(): void;
}

interface Pet extends Animal {
  owner: string;
  isVaccinated: boolean;
}

interface Dog extends Pet {
  breed: string;
  fetch(): void;
}

const myDog: Dog = {
  name: "Buddy",
  age: 3,
  owner: "Alice",
  isVaccinated: true,
  breed: "Labrador",
  makeSound() { console.log("Woof!"); },
  fetch() { console.log("Fetching!"); },
};

// Multiple inheritance
interface Flyable { fly(): void; }
interface Swimmable { swim(): void; }

interface Duck extends Flyable, Swimmable {
  quack(): void;
}
```

## 6.5 Interface for Functions

```typescript
// Interface describing a function shape
interface MathOperation {
  (a: number, b: number): number;
}

const add: MathOperation = (a, b) => a + b;
const subtract: MathOperation = (a, b) => a - b;
const multiply: MathOperation = (a, b) => a * b;

// Interface for a class (implements)
interface Serializable {
  serialize(): string;
  deserialize(data: string): void;
}

class UserModel implements Serializable {
  constructor(public name: string, public email: string) {}

  serialize(): string {
    return JSON.stringify({ name: this.name, email: this.email });
  }

  deserialize(data: string): void {
    const parsed = JSON.parse(data);
    this.name = parsed.name;
    this.email = parsed.email;
  }
}
```

## 6.6 Merging Interfaces (Declaration Merging)

```typescript
// Same interface declared twice â€” they merge!
interface Window {
  title: string;
}

interface Window {
  scrollY: number;
}

// Result: Window has both title and scrollY
// This is how third-party type definitions extend built-in types
const w: Window = {
  title: "My App",
  scrollY: 0,
};
```

---

# Chapter 7: Type Aliases

## 7.1 Type vs Interface

```typescript
// Type alias â€” can represent any type
type UserId = number;
type UserName = string;
type UserStatus = "active" | "inactive" | "banned";

// Object type
type Address = {
  street: string;
  city: string;
  state: string;
  pinCode: string;
};

// Difference: type cannot be re-opened/merged (interface can)
// type MyType = { a: string };
// type MyType = { b: number }; // âŒ Error: duplicate identifier

// When to use which:
// Use interface for: objects, classes, extensible shapes
// Use type for: primitives, unions, intersections, tuples, computed types
```

## 7.2 Union Types

```typescript
// Union â€” value can be ONE of these types
type StringOrNumber = string | number;
type Status = "pending" | "active" | "closed" | "cancelled";
type ID = string | number;

function formatId(id: ID): string {
  return typeof id === "string" ? id : id.toString();
}

// Union with objects
interface Circle { kind: "circle"; radius: number; }
interface Rectangle { kind: "rectangle"; width: number; height: number; }
interface Triangle { kind: "triangle"; base: number; height: number; }

type Shape = Circle | Rectangle | Triangle;

function calculateArea(shape: Shape): number {
  switch (shape.kind) {
    case "circle":    return Math.PI * shape.radius ** 2;
    case "rectangle": return shape.width * shape.height;
    case "triangle":  return 0.5 * shape.base * shape.height;
  }
}
```

## 7.3 Intersection Types

```typescript
// Intersection â€” value must satisfy ALL types
type Timestamps = {
  createdAt: Date;
  updatedAt: Date;
};

type SoftDelete = {
  deletedAt: Date | null;
  isDeleted: boolean;
};

type UserBase = {
  id: number;
  name: string;
  email: string;
};

// Combine multiple types
type FullUser = UserBase & Timestamps & SoftDelete;

const user: FullUser = {
  id: 1,
  name: "Alice",
  email: "alice@test.com",
  createdAt: new Date("2024-01-01"),
  updatedAt: new Date("2024-06-15"),
  deletedAt: null,
  isDeleted: false,
};
```

## 7.4 Mapped Types

```typescript
// Make all properties optional
type Partial<T> = {
  [K in keyof T]?: T[K];
};

// Make all properties required
type Required<T> = {
  [K in keyof T]-?: T[K]; // -? removes optional
};

// Make all properties readonly
type Readonly<T> = {
  readonly [K in keyof T]: T[K];
};

// Custom mapped type â€” make all string
type Stringify<T> = {
  [K in keyof T]: string;
};

interface User {
  id: number;
  name: string;
  age: number;
}

type StringifiedUser = Stringify<User>;
// { id: string; name: string; age: string; }

// Pick specific properties
type UserPreview = Pick<User, "id" | "name">;
// { id: number; name: string; }

// Omit specific properties
type UserWithoutId = Omit<User, "id">;
// { name: string; age: number; }
```

## 7.5 Conditional Types

```typescript
// Conditional type â€” like ternary operator for types
type IsString<T> = T extends string ? "yes" : "no";

type A = IsString<string>;   // "yes"
type B = IsString<number>;   // "no"
type C = IsString<"hello">;  // "yes"

// NonNullable â€” remove null and undefined
type NonNullable<T> = T extends null | undefined ? never : T;

type MaybeString = string | null | undefined;
type DefiniteString = NonNullable<MaybeString>; // string

// Infer â€” extract type from within another type
type ReturnType<T> = T extends (...args: unknown[]) => infer R ? R : never;

function getUser(): { id: number; name: string } {
  return { id: 1, name: "Alice" };
}

type UserReturn = ReturnType<typeof getUser>;
// { id: number; name: string }

// Array element type
type ArrayElement<T> = T extends Array<infer E> ? E : never;
type NumberType = ArrayElement<number[]>; // number
type StringType = ArrayElement<string[]>; // string
```

## 7.6 Template Literal Types

```typescript
// TypeScript 4.1+
type Greeting = `Hello, ${string}!`;
const g: Greeting = "Hello, World!";    // âœ…
// const g2: Greeting = "Hi, World!";   // âŒ doesn't start with "Hello, "

// Event names pattern
type EventName = "click" | "focus" | "blur" | "change";
type EventHandler = `on${Capitalize<EventName>}`;
// "onClick" | "onFocus" | "onBlur" | "onChange"

// CSS property pattern
type CSSProperty = "margin" | "padding" | "border";
type CSSDirection = "Top" | "Right" | "Bottom" | "Left";
type CSSShorthand = `${CSSProperty}${CSSDirection}`;
// "marginTop" | "marginRight" | "paddingTop" | etc.

// API endpoint pattern
type Method = "get" | "post" | "put" | "delete";
type Resource = "users" | "orders" | "products";
type ApiEndpoint = `/${Resource}`;
// "/users" | "/orders" | "/products"

// Real use: type-safe event system
type ButtonEvents = {
  [K in `on${Capitalize<"click" | "hover" | "focus">}`]: () => void;
};
// { onClick: () => void; onHover: () => void; onFocus: () => void; }
```

---

# Chapter 8: Classes & Object-Oriented Programming

## 8.1 Class Basics

```typescript
class Person {
  // Properties
  name: string;
  age: number;
  email: string;

  // Constructor
  constructor(name: string, age: number, email: string) {
    this.name = name;
    this.age = age;
    this.email = email;
  }

  // Methods
  greet(): string {
    return `Hi, I'm ${this.name} and I'm ${this.age} years old.`;
  }

  isAdult(): boolean {
    return this.age >= 18;
  }
}

// Create instances
const alice = new Person("Alice", 30, "alice@test.com");
console.log(alice.greet());    // "Hi, I'm Alice and I'm 30 years old."
console.log(alice.isAdult());  // true
```

## 8.2 Access Modifiers

```typescript
class BankAccount {
  public accountNumber: string;      // Accessible everywhere (default)
  protected balance: number;          // Accessible in class + subclasses
  private pin: string;               // Accessible ONLY in this class
  readonly bankName: string;         // Cannot be changed after creation

  constructor(accountNumber: string, initialBalance: number, pin: string) {
    this.accountNumber = accountNumber;
    this.balance = initialBalance;
    this.pin = pin;
    this.bankName = "TypeScript Bank";
  }

  public getBalance(): number {
    return this.balance;  // Can access private balance from within class
  }

  private validatePin(enteredPin: string): boolean {
    return this.pin === enteredPin;  // Private â€” only used internally
  }

  public withdraw(amount: number, enteredPin: string): void {
    if (!this.validatePin(enteredPin)) {
      throw new Error("Invalid PIN");
    }
    if (amount > this.balance) {
      throw new Error("Insufficient funds");
    }
    this.balance -= amount;
    console.log(`Withdrew â‚¹${amount}. Remaining: â‚¹${this.balance}`);
  }
}

const account = new BankAccount("ACC-001", 10000, "1234");
console.log(account.accountNumber); // âœ… Public
console.log(account.getBalance());  // âœ… Public method
// console.log(account.balance);    // âŒ Protected
// console.log(account.pin);        // âŒ Private
account.withdraw(2000, "1234");     // âœ… Correct PIN
```

## 8.3 Shorthand Constructor Parameters

```typescript
// Long way
class Product {
  public name: string;
  public price: number;
  private stock: number;

  constructor(name: string, price: number, stock: number) {
    this.name = name;
    this.price = price;
    this.stock = stock;
  }
}

// Short way â€” TypeScript shorthand
class Product2 {
  constructor(
    public name: string,
    public price: number,
    private stock: number,
    readonly sku: string,
  ) {} // No body needed! TypeScript auto-assigns
}

const p = new Product2("Laptop", 50000, 10, "LAP-001");
console.log(p.name);   // "Laptop"
console.log(p.price);  // 50000
// console.log(p.stock); // âŒ private
```

## 8.4 Inheritance

```typescript
// Base class
class Vehicle {
  constructor(
    public make: string,
    public model: string,
    public year: number,
    protected fuelLevel: number = 100
  ) {}

  describe(): string {
    return `${this.year} ${this.make} ${this.model}`;
  }

  refuel(amount: number): void {
    this.fuelLevel = Math.min(100, this.fuelLevel + amount);
    console.log(`Fuel level: ${this.fuelLevel}%`);
  }

  protected consumeFuel(amount: number): void {
    this.fuelLevel = Math.max(0, this.fuelLevel - amount);
  }
}

// Derived class â€” extends Vehicle
class Car extends Vehicle {
  private doors: number;

  constructor(make: string, model: string, year: number, doors: number = 4) {
    super(make, model, year); // Must call super() first
    this.doors = doors;
  }

  // Override parent method
  describe(): string {
    return `${super.describe()} (${this.doors}-door car)`;
  }

  drive(km: number): void {
    this.consumeFuel(km * 0.08); // 8% per km (accessible â€” protected)
    console.log(`Drove ${km} km. Fuel: ${this.fuelLevel}%`);
  }
}

class ElectricCar extends Car {
  private batteryLevel: number;

  constructor(make: string, model: string, year: number) {
    super(make, model, year);
    this.batteryLevel = 100;
  }

  describe(): string {
    return `${super.describe()} [Electric]`;
  }

  charge(amount: number): void {
    this.batteryLevel = Math.min(100, this.batteryLevel + amount);
    console.log(`Battery: ${this.batteryLevel}%`);
  }
}

const tesla = new ElectricCar("Tesla", "Model 3", 2024);
console.log(tesla.describe()); // "2024 Tesla Model 3 (4-door car) [Electric]"
tesla.charge(20);
```

## 8.5 Abstract Classes

```typescript
// Abstract class â€” cannot be instantiated directly
abstract class Shape {
  abstract getArea(): number;       // Must be implemented by subclasses
  abstract getPerimeter(): number;  // Must be implemented by subclasses

  // Concrete method â€” shared by all subclasses
  describe(): string {
    return `${this.constructor.name}: Area=${this.getArea().toFixed(2)}, Perimeter=${this.getPerimeter().toFixed(2)}`;
  }
}

class Circle extends Shape {
  constructor(private radius: number) {
    super();
  }

  getArea(): number {
    return Math.PI * this.radius ** 2;
  }

  getPerimeter(): number {
    return 2 * Math.PI * this.radius;
  }
}

class Rectangle extends Shape {
  constructor(private width: number, private height: number) {
    super();
  }

  getArea(): number {
    return this.width * this.height;
  }

  getPerimeter(): number {
    return 2 * (this.width + this.height);
  }
}

// const s = new Shape(); // âŒ Error: Cannot create abstract class

const shapes: Shape[] = [
  new Circle(5),
  new Rectangle(4, 6),
];

shapes.forEach(s => console.log(s.describe()));
```

## 8.6 Static Members

```typescript
class MathUtils {
  // Static property â€” belongs to the class, not instances
  static readonly PI: number = 3.14159265;

  // Static method â€” called on class, not instance
  static circleArea(radius: number): number {
    return MathUtils.PI * radius ** 2;
  }

  static clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }
}

MathUtils.circleArea(5);           // âœ… Called on class
// new MathUtils().circleArea(5);  // âŒ Not on instance

// Singleton pattern using static
class AppConfig {
  private static instance: AppConfig;
  private settings: Map<string, string> = new Map();

  private constructor() {} // Private â€” cannot be created with new

  static getInstance(): AppConfig {
    if (!AppConfig.instance) {
      AppConfig.instance = new AppConfig();
    }
    return AppConfig.instance;
  }

  set(key: string, value: string): void {
    this.settings.set(key, value);
  }

  get(key: string): string | undefined {
    return this.settings.get(key);
  }
}

const config1 = AppConfig.getInstance();
const config2 = AppConfig.getInstance();
console.log(config1 === config2); // true â€” same instance
config1.set("theme", "dark");
console.log(config2.get("theme")); // "dark" â€” same object
```

## 8.7 Implementing Interfaces

```typescript
interface Printable {
  print(): void;
  toJSON(): object;
}

interface Validatable {
  validate(): boolean;
  getErrors(): string[];
}

class Invoice implements Printable, Validatable {
  private errors: string[] = [];

  constructor(
    public id: string,
    public amount: number,
    public customerId: string
  ) {}

  print(): void {
    console.log(`Invoice #${this.id} â€” Amount: â‚¹${this.amount}`);
  }

  toJSON(): object {
    return { id: this.id, amount: this.amount, customerId: this.customerId };
  }

  validate(): boolean {
    this.errors = [];
    if (this.amount <= 0) this.errors.push("Amount must be positive");
    if (!this.customerId) this.errors.push("Customer ID required");
    if (!this.id) this.errors.push("Invoice ID required");
    return this.errors.length === 0;
  }

  getErrors(): string[] {
    return this.errors;
  }
}

const invoice = new Invoice("INV-001", 5000, "CUST-123");
if (!invoice.validate()) {
  console.log("Errors:", invoice.getErrors());
} else {
  invoice.print();
}
```

---

# Chapter 9: Generics

## 9.1 Why Generics?

```typescript
// Without generics â€” duplicate code for each type
function getFirstNumber(arr: number[]): number | undefined { return arr[0]; }
function getFirstString(arr: string[]): string | undefined { return arr[0]; }
function getFirstBoolean(arr: boolean[]): boolean | undefined { return arr[0]; }

// With generics â€” one function handles all types
function getFirst<T>(arr: T[]): T | undefined {
  return arr[0];
}

getFirst<number>([1, 2, 3]);     // number | undefined
getFirst<string>(["a", "b"]);    // string | undefined
getFirst([true, false]);          // TypeScript infers: boolean | undefined
```

## 9.2 Generic Interfaces

```typescript
// Generic interface
interface Repository<T> {
  getById(id: number): T | undefined;
  getAll(): T[];
  save(item: T): void;
  delete(id: number): boolean;
}

interface User { id: number; name: string; email: string; }
interface Product { id: number; name: string; price: number; }

// Implementation for User
class UserRepository implements Repository<User> {
  private users: User[] = [];

  getById(id: number): User | undefined {
    return this.users.find(u => u.id === id);
  }

  getAll(): User[] {
    return [...this.users];
  }

  save(user: User): void {
    const index = this.users.findIndex(u => u.id === user.id);
    if (index >= 0) {
      this.users[index] = user;
    } else {
      this.users.push(user);
    }
  }

  delete(id: number): boolean {
    const index = this.users.findIndex(u => u.id === id);
    if (index >= 0) {
      this.users.splice(index, 1);
      return true;
    }
    return false;
  }
}

// Reusable for ANY entity
class ProductRepository implements Repository<Product> {
  private products: Product[] = [];
  getById(id: number) { return this.products.find(p => p.id === id); }
  getAll() { return [...this.products]; }
  save(p: Product) { this.products.push(p); }
  delete(id: number) { const i = this.products.findIndex(p => p.id === id); if (i >= 0) { this.products.splice(i, 1); return true; } return false; }
}
```

## 9.3 Generic Classes

```typescript
// Generic Stack (LIFO data structure)
class Stack<T> {
  private items: T[] = [];

  push(item: T): void {
    this.items.push(item);
  }

  pop(): T | undefined {
    return this.items.pop();
  }

  peek(): T | undefined {
    return this.items[this.items.length - 1];
  }

  isEmpty(): boolean {
    return this.items.length === 0;
  }

  get size(): number {
    return this.items.length;
  }

  toArray(): T[] {
    return [...this.items];
  }
}

const numStack = new Stack<number>();
numStack.push(1);
numStack.push(2);
numStack.push(3);
console.log(numStack.pop());  // 3
console.log(numStack.peek()); // 2

const strStack = new Stack<string>();
strStack.push("hello");
strStack.push("world");

// Generic Queue (FIFO)
class Queue<T> {
  private items: T[] = [];

  enqueue(item: T): void { this.items.push(item); }
  dequeue(): T | undefined { return this.items.shift(); }
  front(): T | undefined { return this.items[0]; }
  get size(): number { return this.items.length; }
}
```

## 9.4 Generic Constraints

```typescript
// Constraint: T must have a length property
function logLength<T extends { length: number }>(item: T): void {
  console.log(`Length: ${item.length}`);
}

logLength("hello");       // âœ… string has length
logLength([1, 2, 3]);     // âœ… array has length
logLength({ length: 5 }); // âœ… object with length
// logLength(42);          // âŒ number has no length

// Constraint: K must be a key of T
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

const user = { id: 1, name: "Alice", email: "alice@test.com" };
getProperty(user, "name");   // âœ… string
getProperty(user, "id");     // âœ… number
// getProperty(user, "age"); // âŒ not a key of user

// Multiple constraints
interface Identifiable { id: number; }
interface Nameable { name: string; }

function displayItem<T extends Identifiable & Nameable>(item: T): string {
  return `[${item.id}] ${item.name}`;
}

displayItem({ id: 1, name: "Alice", email: "alice@test.com" }); // âœ…
// displayItem({ id: 1 }); // âŒ Missing name
```

---

# Chapter 10: Enums

## 10.1 Numeric Enums (Default)

```typescript
// Numeric enum â€” values auto-increment from 0
enum Direction {
  North,   // 0
  South,   // 1
  East,    // 2
  West,    // 3
}

let dir: Direction = Direction.North;
console.log(dir);              // 0
console.log(Direction[0]);     // "North" â€” reverse mapping
console.log(Direction.North);  // 0

// Custom starting value
enum Priority {
  Low = 1,    // 1
  Medium,     // 2 (auto-increments)
  High,       // 3
  Critical,   // 4
}

// Custom values
enum HttpStatus {
  OK = 200,
  Created = 201,
  BadRequest = 400,
  Unauthorized = 401,
  NotFound = 404,
  ServerError = 500,
}

function handleResponse(status: HttpStatus): string {
  switch (status) {
    case HttpStatus.OK: return "Success";
    case HttpStatus.Created: return "Resource created";
    case HttpStatus.NotFound: return "Not found";
    case HttpStatus.ServerError: return "Server error";
    default: return "Unknown status";
  }
}
```

## 10.2 String Enums (Recommended)

```typescript
// String enums â€” more readable, no reverse mapping
enum UserRole {
  Admin = "ADMIN",
  Manager = "MANAGER",
  Employee = "EMPLOYEE",
  Guest = "GUEST",
}

enum OrderStatus {
  Pending = "PENDING",
  Confirmed = "CONFIRMED",
  Processing = "PROCESSING",
  Shipped = "SHIPPED",
  Delivered = "DELIVERED",
  Cancelled = "CANCELLED",
  Refunded = "REFUNDED",
}

interface Order {
  id: string;
  status: OrderStatus;
  userId: string;
}

function canCancelOrder(order: Order): boolean {
  return [OrderStatus.Pending, OrderStatus.Confirmed].includes(order.status);
}

const order: Order = {
  id: "ORD-001",
  status: OrderStatus.Confirmed,
  userId: "USR-123",
};

console.log(canCancelOrder(order)); // true
console.log(order.status);          // "CONFIRMED"
```

## 10.3 Const Enums

```typescript
// Const enum â€” inlined at compile time (no enum object in JS output)
const enum Color {
  Red = "RED",
  Green = "GREEN",
  Blue = "BLUE",
}

const myColor = Color.Red; // Compiled to: const myColor = "RED";
// No Color object exists at runtime â€” just the literal value

// Use const enum for:
// - Better performance (no runtime lookup)
// - Smaller bundle size
// Avoid if you need to iterate over enum values

// Iterating over a regular enum
enum Season {
  Spring = "SPRING",
  Summer = "SUMMER",
  Autumn = "AUTUMN",
  Winter = "WINTER",
}

const allSeasons = Object.values(Season);
console.log(allSeasons); // ["SPRING", "SUMMER", "AUTUMN", "WINTER"]
```

---

*Part 2 complete â€” Chapters 6â€“10: Interfaces, Type Aliases, Classes, Generics, Enums*

---

# Chapter 11: Advanced Types

## 11.1 Union Types â€” Deep Dive

```typescript
// Discriminated unions â€” powerful pattern for type-safe code
interface LoginSuccess {
  status: "success";
  user: { id: number; name: string };
  token: string;
}

interface LoginFailure {
  status: "failure";
  error: string;
  attempts: number;
}

interface LoginBlocked {
  status: "blocked";
  reason: string;
  unblockAt: Date;
}

type LoginResult = LoginSuccess | LoginFailure | LoginBlocked;

function handleLogin(result: LoginResult): void {
  switch (result.status) {
    case "success":
      console.log(`Welcome ${result.user.name}! Token: ${result.token}`);
      break;
    case "failure":
      console.log(`Login failed: ${result.error}. Attempts: ${result.attempts}`);
      break;
    case "blocked":
      console.log(`Account blocked: ${result.reason}. Try after ${result.unblockAt}`);
      break;
  }
}

// Exhaustiveness check â€” compiler ensures all cases handled
function getStatusMessage(result: LoginResult): string {
  switch (result.status) {
    case "success": return "Logged in";
    case "failure": return "Failed";
    case "blocked": return "Blocked";
    default:
      const _exhaustive: never = result; // âŒ Error if a case is missing
      return _exhaustive;
  }
}
```

## 11.2 Intersection Types â€” Deep Dive

```typescript
// Combine multiple types into one
type Timestamped = { createdAt: Date; updatedAt: Date };
type SoftDeletable = { deletedAt: Date | null; isDeleted: boolean };
type Auditable = { createdBy: string; updatedBy: string };

type BaseEntity = {
  id: string;
  version: number;
};

// Full entity has all combined properties
type FullEntity = BaseEntity & Timestamped & SoftDeletable & Auditable;

const entity: FullEntity = {
  id: "ENT-001",
  version: 1,
  createdAt: new Date(),
  updatedAt: new Date(),
  deletedAt: null,
  isDeleted: false,
  createdBy: "admin",
  updatedBy: "admin",
};

// Mixin pattern with intersection
type Constructor<T = {}> = new (...args: any[]) => T;

function withTimestamp<T extends Constructor>(Base: T) {
  return class extends Base {
    createdAt = new Date();
    updatedAt = new Date();
  };
}
```

## 11.3 Indexed Access Types

```typescript
interface User {
  id: number;
  name: string;
  address: {
    street: string;
    city: string;
    country: string;
    coordinates: {
      lat: number;
      lng: number;
    };
  };
  orders: { id: string; total: number }[];
}

// Access nested types
type UserAddress = User["address"];             // { street; city; country; coordinates }
type UserCity = User["address"]["city"];         // string
type UserCoords = User["address"]["coordinates"]; // { lat: number; lng: number }
type OrderType = User["orders"][number];         // { id: string; total: number }
type OrderId = User["orders"][number]["id"];     // string

// Use with keyof
type UserKeys = keyof User; // "id" | "name" | "address" | "orders"

function getUserField<K extends keyof User>(user: User, field: K): User[K] {
  return user[field];
}
```

## 11.4 Recursive Types

```typescript
// JSON type â€” recursive
type JSONValue =
  | string
  | number
  | boolean
  | null
  | JSONValue[]
  | { [key: string]: JSONValue };

const data: JSONValue = {
  name: "Alice",
  age: 30,
  active: true,
  tags: ["admin", "user"],
  address: {
    city: "Mumbai",
    coordinates: [19.07, 72.87],
  },
};

// File system tree
interface FileNode {
  name: string;
  type: "file";
  size: number;
}

interface FolderNode {
  name: string;
  type: "folder";
  children: TreeNode[]; // Recursive!
}

type TreeNode = FileNode | FolderNode;

const projectTree: TreeNode = {
  name: "src",
  type: "folder",
  children: [
    { name: "index.ts", type: "file", size: 1024 },
    {
      name: "utils",
      type: "folder",
      children: [
        { name: "helpers.ts", type: "file", size: 512 },
      ],
    },
  ],
};

// Recursive function with recursive type
function getTotalSize(node: TreeNode): number {
  if (node.type === "file") return node.size;
  return node.children.reduce((sum, child) => sum + getTotalSize(child), 0);
}

console.log(getTotalSize(projectTree)); // 1536
```

---

# Chapter 12: Type Guards & Narrowing

## 12.1 typeof Guards

```typescript
function formatValue(value: string | number | boolean): string {
  // TypeScript narrows the type inside each branch
  if (typeof value === "string") {
    return value.toUpperCase(); // TypeScript knows: string
  }
  if (typeof value === "number") {
    return value.toFixed(2);    // TypeScript knows: number
  }
  return value ? "Yes" : "No"; // TypeScript knows: boolean
}

console.log(formatValue("hello")); // "HELLO"
console.log(formatValue(3.14));    // "3.14"
console.log(formatValue(true));    // "Yes"
```

## 12.2 instanceof Guards

```typescript
class ApiError extends Error {
  constructor(public statusCode: number, message: string) {
    super(message);
  }
}

class ValidationError extends Error {
  constructor(public fields: string[], message: string) {
    super(message);
  }
}

class NetworkError extends Error {
  constructor(public retryAfter: number, message: string) {
    super(message);
  }
}

function handleError(error: Error): string {
  if (error instanceof ApiError) {
    return `API Error ${error.statusCode}: ${error.message}`;
  }
  if (error instanceof ValidationError) {
    return `Validation Error in fields: ${error.fields.join(", ")}`;
  }
  if (error instanceof NetworkError) {
    return `Network Error â€” retry after ${error.retryAfter}ms`;
  }
  return `Unknown Error: ${error.message}`;
}
```

## 12.3 in Operator Guard

```typescript
interface Car { drive(): void; wheels: number; }
interface Boat { sail(): void; propeller: boolean; }
interface Plane { fly(): void; altitude: number; }

type Vehicle = Car | Boat | Plane;

function operate(vehicle: Vehicle): void {
  if ("drive" in vehicle) {
    vehicle.drive(); // TypeScript knows: Car
  } else if ("sail" in vehicle) {
    vehicle.sail();  // TypeScript knows: Boat
  } else {
    vehicle.fly();   // TypeScript knows: Plane
  }
}
```

## 12.4 Custom Type Guards (is keyword)

```typescript
interface Fish { swim(): void; name: string; }
interface Bird { fly(): void; name: string; }

// Custom type guard â€” returns a type predicate
function isFish(animal: Fish | Bird): animal is Fish {
  return "swim" in animal;
}

function isBird(animal: Fish | Bird): animal is Bird {
  return "fly" in animal;
}

function move(animal: Fish | Bird): void {
  if (isFish(animal)) {
    animal.swim(); // TypeScript knows it's Fish
  } else {
    animal.fly();  // TypeScript knows it's Bird
  }
}

// Real-world: API response validation
interface SuccessResponse { success: true; data: unknown; }
interface ErrorResponse { success: false; error: string; }
type ApiResponse = SuccessResponse | ErrorResponse;

function isSuccess(response: ApiResponse): response is SuccessResponse {
  return response.success === true;
}

async function fetchUser(): Promise<string> {
  const response: ApiResponse = { success: true, data: { name: "Alice" } };
  if (isSuccess(response)) {
    return JSON.stringify(response.data); // Safe â€” we know it's SuccessResponse
  }
  throw new Error(response.error); // Safe â€” we know it's ErrorResponse
}
```

## 12.5 Assertion Functions

```typescript
// Assertion function â€” throws if condition is false
function assertDefined<T>(value: T | undefined | null, name: string): asserts value is T {
  if (value === undefined || value === null) {
    throw new Error(`${name} is not defined`);
  }
}

function assertString(value: unknown): asserts value is string {
  if (typeof value !== "string") {
    throw new TypeError(`Expected string, got ${typeof value}`);
  }
}

function processConfig(config: unknown): void {
  assertString(config);
  // After assertion, TypeScript knows config is string
  console.log(config.toUpperCase()); // âœ… Safe
}

function getUser(id: number): { name: string } | undefined {
  return id === 1 ? { name: "Alice" } : undefined;
}

const user = getUser(1);
assertDefined(user, "user");
console.log(user.name); // âœ… TypeScript knows user is not undefined
```

---

# Chapter 13: Utility Types

## 13.1 Built-in Utility Types

```typescript
interface User {
  id: number;
  name: string;
  email: string;
  age: number;
  role: "admin" | "user";
  address: {
    city: string;
    country: string;
  };
}

// â”€â”€â”€ Partial<T> â€” all properties become optional â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type UpdateUserDTO = Partial<User>;
// { id?: number; name?: string; email?: string; ... }

function updateUser(id: number, updates: Partial<User>): void {
  console.log(`Updating user ${id} with:`, updates);
}
updateUser(1, { name: "Alice Updated" }); // âœ… Only name
updateUser(1, { email: "new@email.com", age: 26 }); // âœ… Some fields

// â”€â”€â”€ Required<T> â€” all properties become required â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type StrictUser = Required<User>;
// All optional properties (if any) become required

// â”€â”€â”€ Readonly<T> â€” all properties become readonly â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type FrozenUser = Readonly<User>;
const frozenUser: FrozenUser = { id: 1, name: "Alice", email: "a@b.com", age: 25, role: "user", address: { city: "Mumbai", country: "IN" } };
// frozenUser.name = "Bob"; // âŒ Error: Cannot assign to 'name'

// â”€â”€â”€ Pick<T, K> â€” select specific properties â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type UserPreview = Pick<User, "id" | "name" | "role">;
// { id: number; name: string; role: "admin" | "user" }

// â”€â”€â”€ Omit<T, K> â€” exclude specific properties â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type UserWithoutId = Omit<User, "id">;
// { name: string; email: string; age: number; role: ...; address: ... }

type CreateUserDTO = Omit<User, "id" | "role">;
// { name: string; email: string; age: number; address: ... }

// â”€â”€â”€ Record<K, V> â€” create object type with specific key-value â”€
type StatusMap = Record<string, boolean>;
const featureFlags: StatusMap = {
  darkMode: true,
  notifications: false,
  analytics: true,
};

type RolePermissions = Record<User["role"], string[]>;
const permissions: RolePermissions = {
  admin: ["read", "write", "delete", "manage"],
  user: ["read", "write"],
};
```

## 13.2 Extract and Exclude

```typescript
type AllTypes = string | number | boolean | null | undefined | object;

// â”€â”€â”€ Exclude â€” remove types from union â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type Primitives = Exclude<AllTypes, object | null | undefined>;
// string | number | boolean

// â”€â”€â”€ Extract â€” keep only matching types â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type Nullable = Extract<AllTypes, null | undefined>;
// null | undefined

// Real-world use: filter event types
type AllEvents = "click" | "focus" | "blur" | "submit" | "reset" | "input";
type FormEvents = Extract<AllEvents, "submit" | "reset" | "input">;
// "submit" | "reset" | "input"

type NonFormEvents = Exclude<AllEvents, FormEvents>;
// "click" | "focus" | "blur"
```

## 13.3 ReturnType, Parameters, InstanceType

```typescript
function createUser(name: string, age: number): { id: number; name: string; age: number } {
  return { id: Date.now(), name, age };
}

// â”€â”€â”€ ReturnType â€” get function's return type â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type CreatedUser = ReturnType<typeof createUser>;
// { id: number; name: string; age: number }

// â”€â”€â”€ Parameters â€” get function's parameter types as tuple â”€â”€â”€â”€â”€
type CreateUserParams = Parameters<typeof createUser>;
// [name: string, age: number]

const params: CreateUserParams = ["Alice", 30];
const user = createUser(...params); // Spread tuple as arguments

// â”€â”€â”€ Awaited â€” unwrap Promise type â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
async function fetchData(): Promise<{ items: string[] }> {
  return { items: ["a", "b", "c"] };
}

type FetchResult = Awaited<ReturnType<typeof fetchData>>;
// { items: string[] }  (Promise unwrapped)

// â”€â”€â”€ NonNullable â€” remove null and undefined â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
type MaybeUser = string | null | undefined;
type DefiniteUser = NonNullable<MaybeUser>; // string
```

## 13.4 Building Custom Utility Types

```typescript
// Make specific properties optional
type PartialBy<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

interface User { id: number; name: string; email: string; age: number; }
type CreateUser = PartialBy<User, "id" | "age">;
// { name: string; email: string; id?: number; age?: number }

// Make specific properties required
type RequiredBy<T, K extends keyof T> = T & Required<Pick<T, K>>;

// Deep readonly (recursive)
type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object ? DeepReadonly<T[K]> : T[K];
};

const deepConfig: DeepReadonly<{ db: { host: string; port: number } }> = {
  db: { host: "localhost", port: 5432 },
};
// deepConfig.db.host = "other"; // âŒ Error: readonly

// Deep partial (recursive)
type DeepPartial<T> = {
  [K in keyof T]?: T[K] extends object ? DeepPartial<T[K]> : T[K];
};

// Nullable version
type Nullable<T> = { [K in keyof T]: T[K] | null };
```

---

# Chapter 14: Modules & Namespaces

## 14.1 ES Modules (import/export)

```typescript
// â”€â”€â”€ Named exports (models/user.ts) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
export interface User {
  id: number;
  name: string;
  email: string;
}

export type UserRole = "admin" | "user" | "viewer";

export function createUser(name: string, email: string): User {
  return { id: Date.now(), name, email };
}

export const DEFAULT_ROLE: UserRole = "user";

// â”€â”€â”€ Named imports (in another file) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// import { User, UserRole, createUser, DEFAULT_ROLE } from './models/user';

// â”€â”€â”€ Rename on import â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// import { User as UserModel, createUser as newUser } from './models/user';

// â”€â”€â”€ Import all â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// import * as UserModule from './models/user';
// const user = UserModule.createUser("Alice", "alice@test.com");
```

```typescript
// â”€â”€â”€ Default export (services/auth-service.ts) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
export default class AuthService {
  private token: string | null = null;

  async login(email: string, password: string): Promise<boolean> {
    // Simulate API call
    this.token = `token-${Date.now()}`;
    return true;
  }

  isAuthenticated(): boolean {
    return this.token !== null;
  }

  getToken(): string | null {
    return this.token;
  }
}

// â”€â”€â”€ Default import â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// import AuthService from './services/auth-service';
// const auth = new AuthService();
// await auth.login("user@test.com", "password");
```

## 14.2 Re-exports (Barrel Files)

```typescript
// models/index.ts â€” barrel file (re-exports everything)
export { User, UserRole, createUser } from './user';
export { Product, ProductCategory } from './product';
export { Order, OrderStatus } from './order';
export type { Config } from './config';

// Now consumers import from one place:
// import { User, Product, Order } from './models';
```

## 14.3 Type-Only Imports

```typescript
// Import ONLY the type (not the runtime value)
import type { User, UserRole } from './models/user';

// This is removed at compile time â€” zero runtime cost
// Use when you only need the type for annotations, not the actual class/function

// Mixed import
import { createUser } from './models/user';
import type { User } from './models/user';

// Or combined syntax (TypeScript 4.5+)
import { createUser, type User, type UserRole } from './models/user';
```

## 14.4 Namespaces

```typescript
// Namespaces â€” organize code (mostly used in .d.ts declaration files)
namespace Validation {
  export interface Rule {
    field: string;
    message: string;
    validate(value: unknown): boolean;
  }

  export function isEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  export function isPhone(value: string): boolean {
    return /^\+?[1-9]\d{9,14}$/.test(value);
  }

  export function isRequired(value: unknown): boolean {
    return value !== null && value !== undefined && value !== "";
  }

  export function minLength(value: string, min: number): boolean {
    return value.length >= min;
  }
}

// Usage
console.log(Validation.isEmail("test@example.com")); // true
console.log(Validation.isPhone("+919876543210"));     // true
console.log(Validation.minLength("hello", 3));        // true

// Note: Prefer ES modules over namespaces in modern TypeScript
```

---

# Chapter 15: Decorators

## 15.1 What are Decorators?

Decorators add metadata or modify classes, methods, and properties. They are widely used in frameworks like **NestJS**, **Angular**, and **TypeORM**.

```typescript
// Enable in tsconfig.json:
// "experimentalDecorators": true
// "emitDecoratorMetadata": true
```

## 15.2 Class Decorators

```typescript
// A decorator is a function that receives the class constructor
function Logger(constructor: Function) {
  console.log(`[LOG] Class created: ${constructor.name}`);
}

@Logger
class UserService {
  getUsers(): string[] {
    return ["Alice", "Bob"];
  }
}
// Output: [LOG] Class created: UserService

// Decorator factory â€” returns a decorator (allows passing parameters)
function Component(config: { selector: string; template: string }) {
  return function (constructor: Function) {
    console.log(`Component registered: ${config.selector}`);
    (constructor as any).selector = config.selector;
    (constructor as any).template = config.template;
  };
}

@Component({ selector: "app-header", template: "<h1>Header</h1>" })
class HeaderComponent {
  // Angular-style component registration
}
```

## 15.3 Method Decorators

```typescript
// Log method calls
function Log(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
  const original = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`[${propertyKey}] called with:`, args);
    const result = original.apply(this, args);
    console.log(`[${propertyKey}] returned:`, result);
    return result;
  };
}

// Measure execution time
function MeasureTime(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
  const original = descriptor.value;
  descriptor.value = function (...args: any[]) {
    const start = performance.now();
    const result = original.apply(this, args);
    const end = performance.now();
    console.log(`[${propertyKey}] took ${(end - start).toFixed(2)}ms`);
    return result;
  };
}

// Retry on failure
function Retry(attempts: number = 3) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    const original = descriptor.value;
    descriptor.value = async function (...args: any[]) {
      for (let i = 0; i < attempts; i++) {
        try {
          return await original.apply(this, args);
        } catch (error) {
          console.log(`[${propertyKey}] Attempt ${i + 1} failed`);
          if (i === attempts - 1) throw error;
        }
      }
    };
  };
}

class OrderService {
  @Log
  @MeasureTime
  calculateTotal(items: number[]): number {
    return items.reduce((sum, n) => sum + n, 0);
  }

  @Retry(3)
  async fetchOrder(id: string): Promise<object> {
    // Simulate flaky API
    if (Math.random() < 0.5) throw new Error("Network error");
    return { id, status: "delivered" };
  }
}

const service = new OrderService();
service.calculateTotal([100, 200, 300]); // Logs + measures time
```

## 15.4 Property Decorators

```typescript
// Validate property values
function MinLength(min: number) {
  return function (target: any, propertyKey: string) {
    let value: string;
    Object.defineProperty(target, propertyKey, {
      get: () => value,
      set: (newValue: string) => {
        if (newValue.length < min) {
          throw new Error(`${propertyKey} must be at least ${min} characters`);
        }
        value = newValue;
      },
    });
  };
}

function MaxValue(max: number) {
  return function (target: any, propertyKey: string) {
    let value: number;
    Object.defineProperty(target, propertyKey, {
      get: () => value,
      set: (newValue: number) => {
        if (newValue > max) {
          throw new Error(`${propertyKey} must not exceed ${max}`);
        }
        value = newValue;
      },
    });
  };
}

class Product {
  @MinLength(3)
  name: string = "";

  @MaxValue(100000)
  price: number = 0;
}

const product = new Product();
product.name = "Laptop";       // âœ…
// product.name = "LP";        // âŒ Error: must be at least 3 characters
product.price = 50000;         // âœ…
// product.price = 200000;     // âŒ Error: must not exceed 100000
```

---

# Chapter 16: Async/Await & Promises

## 16.1 Promises â€” Foundation

```typescript
// A Promise represents a future value
function fetchUser(id: number): Promise<{ id: number; name: string }> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (id > 0) {
        resolve({ id, name: "Alice" });
      } else {
        reject(new Error("Invalid ID"));
      }
    }, 1000);
  });
}

// Using .then()
fetchUser(1)
  .then(user => console.log("Got user:", user.name))
  .catch(err => console.error("Error:", err.message));
```

## 16.2 Async/Await â€” Clean Syntax

```typescript
// async function always returns a Promise
async function getUser(id: number): Promise<{ id: number; name: string }> {
  const response = await fetch(`https://api.example.com/users/${id}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

// Calling async functions
async function main(): Promise<void> {
  try {
    const user = await getUser(1);
    console.log("User:", user.name);
  } catch (error) {
    if (error instanceof Error) {
      console.error("Failed:", error.message);
    }
  }
}

main();
```

## 16.3 Sequential vs Parallel Execution

```typescript
// Sequential â€” each waits for the previous (slow)
async function sequential(): Promise<void> {
  const user = await fetchUser(1);        // Wait 1s
  const orders = await fetchOrders(1);     // Wait 1s
  const notifications = await fetchNotifications(1); // Wait 1s
  // Total: 3 seconds
}

// Parallel â€” all start simultaneously (fast)
async function parallel(): Promise<void> {
  const [user, orders, notifications] = await Promise.all([
    fetchUser(1),           // All three
    fetchOrders(1),         // start at the
    fetchNotifications(1),  // same time
  ]);
  // Total: ~1 second (max of the three)
}

// Promise.allSettled â€” don't fail if one rejects
async function resilient(): Promise<void> {
  const results = await Promise.allSettled([
    fetchUser(1),
    fetchUser(-1), // This will reject
    fetchUser(3),
  ]);

  results.forEach((result, i) => {
    if (result.status === "fulfilled") {
      console.log(`User ${i}: ${result.value.name}`);
    } else {
      console.log(`User ${i} failed: ${result.reason.message}`);
    }
  });
}

// Promise.race â€” first to settle wins
async function fastest(): Promise<void> {
  const result = await Promise.race([
    fetchFromServer1(),
    fetchFromServer2(),
    fetchFromServer3(),
  ]);
  console.log("Fastest response:", result);
}

// Helper stubs
async function fetchOrders(userId: number) { return [{ id: "ORD-1" }]; }
async function fetchNotifications(userId: number) { return [{ msg: "Hello" }]; }
async function fetchFromServer1() { return "Server 1"; }
async function fetchFromServer2() { return "Server 2"; }
async function fetchFromServer3() { return "Server 3"; }
```

## 16.4 Typed Async Patterns

```typescript
// Generic async function
async function fetchData<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json() as Promise<T>;
}

interface User { id: number; name: string; }
interface Product { id: number; price: number; }

const user = await fetchData<User>("/api/users/1");
const product = await fetchData<Product>("/api/products/1");

// Async iterator
async function* generateNumbers(): AsyncGenerator<number> {
  for (let i = 1; i <= 5; i++) {
    await new Promise(r => setTimeout(r, 500));
    yield i;
  }
}

async function consumeNumbers(): Promise<void> {
  for await (const num of generateNumbers()) {
    console.log(num); // 1, 2, 3, 4, 5 (each after 500ms)
  }
}
```

## 16.5 Common Async Mistakes

```typescript
// âŒ Forgetting await
async function bad(): Promise<void> {
  const user = fetchUser(1); // Returns Promise, not the value!
  console.log(user); // Promise { <pending> } â€” NOT the user object
}

// âœ… Fix: Always await
async function good(): Promise<void> {
  const user = await fetchUser(1);
  console.log(user); // { id: 1, name: "Alice" }
}

// âŒ await inside forEach (doesn't work)
async function badLoop(ids: number[]): Promise<void> {
  ids.forEach(async (id) => {
    const user = await fetchUser(id); // Runs in parallel uncontrolled!
    console.log(user);
  });
  console.log("Done"); // Prints BEFORE any user!
}

// âœ… Fix: Use for...of for sequential
async function goodLoop(ids: number[]): Promise<void> {
  for (const id of ids) {
    const user = await fetchUser(id);
    console.log(user);
  }
  console.log("Done"); // Prints AFTER all users
}

// âœ… Fix: Use Promise.all for parallel
async function parallelLoop(ids: number[]): Promise<void> {
  const users = await Promise.all(ids.map(id => fetchUser(id)));
  users.forEach(u => console.log(u));
  console.log("Done");
}
```

---

# Chapter 17: Error Handling

## 17.1 Try/Catch with TypeScript

```typescript
// Basic try/catch
async function loadData(): Promise<void> {
  try {
    const response = await fetch("/api/data");
    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`);
    }
    const data = await response.json();
    console.log("Data loaded:", data);
  } catch (error) {
    // error is 'unknown' in TypeScript (since 4.4)
    if (error instanceof Error) {
      console.error("Error:", error.message);
      console.error("Stack:", error.stack);
    } else {
      console.error("Unknown error:", error);
    }
  } finally {
    console.log("Cleanup â€” always runs");
  }
}
```

## 17.2 Custom Error Classes

```typescript
// Base custom error
class AppError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly statusCode: number = 500,
    public readonly isOperational: boolean = true
  ) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

// Specific error types
class NotFoundError extends AppError {
  constructor(resource: string, id: string | number) {
    super(`${resource} with ID ${id} not found`, "NOT_FOUND", 404);
  }
}

class ValidationError extends AppError {
  constructor(public readonly fields: Record<string, string>) {
    super("Validation failed", "VALIDATION_ERROR", 400);
  }
}

class UnauthorizedError extends AppError {
  constructor(message: string = "Authentication required") {
    super(message, "UNAUTHORIZED", 401);
  }
}

class RateLimitError extends AppError {
  constructor(public readonly retryAfter: number) {
    super(`Rate limit exceeded. Retry after ${retryAfter}s`, "RATE_LIMIT", 429);
  }
}

// Usage
function getUser(id: number): object {
  if (id <= 0) throw new ValidationError({ id: "Must be positive" });
  if (id === 999) throw new NotFoundError("User", id);
  return { id, name: "Alice" };
}

try {
  getUser(999);
} catch (error) {
  if (error instanceof NotFoundError) {
    console.log(`${error.code}: ${error.message}`); // NOT_FOUND: User with ID 999 not found
  } else if (error instanceof ValidationError) {
    console.log("Invalid fields:", error.fields);
  }
}
```

## 17.3 Result Pattern (No Exceptions)

```typescript
// Result type â€” explicit success or failure (no throw)
type Result<T, E = Error> =
  | { success: true; data: T }
  | { success: false; error: E };

function divide(a: number, b: number): Result<number, string> {
  if (b === 0) return { success: false, error: "Division by zero" };
  return { success: true, data: a / b };
}

const result = divide(10, 3);
if (result.success) {
  console.log("Result:", result.data);   // TypeScript knows: number
} else {
  console.log("Error:", result.error);   // TypeScript knows: string
}

// Async result
async function fetchUserSafe(id: number): Promise<Result<{ name: string }>> {
  try {
    const resp = await fetch(`/api/users/${id}`);
    if (!resp.ok) return { success: false, error: new Error(`HTTP ${resp.status}`) };
    const data = await resp.json();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: error instanceof Error ? error : new Error(String(error)) };
  }
}
```

---

# Chapter 18: TypeScript with Node.js

## 18.1 Setting Up TypeScript + Node.js

```bash
# Initialize project
mkdir ts-node-project && cd ts-node-project
npm init -y
npm install typescript ts-node @types/node --save-dev
npx tsc --init
```

## 18.2 File System Operations

```typescript
import fs from 'fs';
import path from 'path';

// Read file
function readConfig(filePath: string): object {
  const absolutePath = path.resolve(filePath);
  const content: string = fs.readFileSync(absolutePath, 'utf-8');
  return JSON.parse(content);
}

// Write file
function writeLog(message: string): void {
  const logPath = path.join(__dirname, 'app.log');
  const timestamp = new Date().toISOString();
  fs.appendFileSync(logPath, `[${timestamp}] ${message}\n`);
}

// Check if file exists
function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath);
}

// List directory contents
function listFiles(dirPath: string): string[] {
  return fs.readdirSync(dirPath).filter(f => {
    return fs.statSync(path.join(dirPath, f)).isFile();
  });
}

// Async file operations
async function readFileAsync(filePath: string): Promise<string> {
  return fs.promises.readFile(filePath, 'utf-8');
}

async function writeFileAsync(filePath: string, content: string): Promise<void> {
  await fs.promises.writeFile(filePath, content, 'utf-8');
}
```

## 18.3 HTTP Server with TypeScript

```typescript
import http from 'http';

interface ApiResponse<T> {
  status: number;
  data?: T;
  error?: string;
}

const users = [
  { id: 1, name: "Alice", email: "alice@test.com" },
  { id: 2, name: "Bob", email: "bob@test.com" },
];

const server = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'application/json');

  if (req.method === 'GET' && req.url === '/api/users') {
    const response: ApiResponse<typeof users> = { status: 200, data: users };
    res.writeHead(200);
    res.end(JSON.stringify(response));
    return;
  }

  if (req.method === 'GET' && req.url?.startsWith('/api/users/')) {
    const id = parseInt(req.url.split('/')[3]);
    const user = users.find(u => u.id === id);
    if (user) {
      res.writeHead(200);
      res.end(JSON.stringify({ status: 200, data: user }));
    } else {
      res.writeHead(404);
      res.end(JSON.stringify({ status: 404, error: "User not found" }));
    }
    return;
  }

  res.writeHead(404);
  res.end(JSON.stringify({ status: 404, error: "Route not found" }));
});

server.listen(3000, () => {
  console.log('Server running on http://localhost:3000');
});
```

## 18.4 Environment Variables with Types

```typescript
// types/env.d.ts â€” Declare environment variable types
declare global {
  namespace NodeJS {
    interface ProcessEnv {
      NODE_ENV: "development" | "production" | "test";
      PORT: string;
      DATABASE_URL: string;
      JWT_SECRET: string;
      API_KEY: string;
    }
  }
}

export {};

// Usage â€” now fully typed
const port: number = parseInt(process.env.PORT);       // TypeScript knows it's string
const dbUrl: string = process.env.DATABASE_URL;         // No 'possibly undefined' error
const env: string = process.env.NODE_ENV;               // "development" | "production" | "test"

// Safe config loader
function loadConfig() {
  const required = ["PORT", "DATABASE_URL", "JWT_SECRET"] as const;
  for (const key of required) {
    if (!process.env[key]) {
      throw new Error(`Missing env variable: ${key}`);
    }
  }
  return {
    port: parseInt(process.env.PORT),
    dbUrl: process.env.DATABASE_URL,
    jwtSecret: process.env.JWT_SECRET,
    isDev: process.env.NODE_ENV === "development",
  };
}
```

---

# Chapter 19: TypeScript Configuration (tsconfig.json)

## 19.1 Strict Mode Options Explained

```json
{
  "compilerOptions": {
    "strict": true
    // Equivalent to enabling ALL of these:
    // "noImplicitAny": true,
    // "strictNullChecks": true,
    // "strictFunctionTypes": true,
    // "strictBindCallApply": true,
    // "strictPropertyInitialization": true,
    // "noImplicitThis": true,
    // "alwaysStrict": true,
    // "useUnknownInCatchVariables": true
  }
}
```

```typescript
// noImplicitAny â€” must annotate when TypeScript can't infer
function bad(x) { return x * 2; }        // âŒ 'x' has implicit 'any'
function good(x: number) { return x * 2; } // âœ…

// strictNullChecks â€” null/undefined are distinct types
let name: string = "Alice";
// name = null;    // âŒ Error: null not assignable to string
let maybeName: string | null = null; // âœ… explicitly allow null

// strictPropertyInitialization â€” class properties must be initialized
class Bad {
  name: string; // âŒ Error: not assigned in constructor
}
class Good {
  name: string;
  constructor(name: string) {
    this.name = name; // âœ…
  }
}
class AlsoGood {
  name!: string; // ! = definite assignment assertion (I'll assign it later)
}
```

## 19.2 Path Aliases

```json
{
  "compilerOptions": {
    "baseUrl": "./src",
    "paths": {
      "@models/*": ["models/*"],
      "@services/*": ["services/*"],
      "@utils/*": ["utils/*"],
      "@config": ["config/index"]
    }
  }
}
```

```typescript
// Without aliases â€” ugly relative imports
import { User } from '../../../models/user';
import { validate } from '../../utils/validators';

// With aliases â€” clean imports
import { User } from '@models/user';
import { validate } from '@utils/validators';
import config from '@config';
```

## 19.3 Project References (Monorepo)

```json
// tsconfig.json (root)
{
  "references": [
    { "path": "./packages/shared" },
    { "path": "./packages/api" },
    { "path": "./packages/web" }
  ]
}

// packages/shared/tsconfig.json
{
  "compilerOptions": {
    "composite": true,
    "outDir": "./dist",
    "rootDir": "./src"
  }
}
```

---

# Chapter 20: Interview Questions & Best Practices

## 20.1 Beginner Questions

**Q1: What is TypeScript?**
> TypeScript is a statically-typed superset of JavaScript developed by Microsoft. It adds optional type annotations that are checked at compile time and removed in the output JavaScript.

**Q2: What are the basic types in TypeScript?**
> `string`, `number`, `boolean`, `null`, `undefined`, `void`, `never`, `any`, `unknown`, `object`, `symbol`, `bigint`, arrays, tuples, and enums.

**Q3: What is the difference between `any` and `unknown`?**
```typescript
let a: any = "hello";
a.toUpperCase();     // âœ… No error (but unsafe)

let b: unknown = "hello";
// b.toUpperCase();  // âŒ Error: must check type first
if (typeof b === "string") {
  b.toUpperCase();   // âœ… Safe after check
}
// Use `unknown` for type-safe code, `any` only as last resort.
```

**Q4: Difference between interface and type?**
```typescript
// Interface â€” extendable, mergeable, for objects/classes
interface User { name: string; }
interface User { age: number; } // Merges! User now has name AND age

// Type â€” for unions, intersections, primitives, computed types
type ID = string | number;            // Union â€” can't do with interface
type UserKeys = keyof User;           // Computed â€” can't do with interface
type Pair<T> = [T, T];               // Tuple â€” can't do with interface
```

**Q5: What is `never` type?**
> `never` represents values that never occur: functions that always throw, infinite loops, or exhaustive switch branches that should be unreachable.

## 20.2 Intermediate Questions

**Q6: Explain generics with an example.**
```typescript
function wrapInArray<T>(value: T): T[] {
  return [value];
}
wrapInArray<string>("hello"); // string[]
wrapInArray(42);               // number[] (inferred)
// Generics allow reusable code that works with any type while maintaining type safety.
```

**Q7: What are utility types?**
> Built-in type transformations: `Partial<T>` (all optional), `Required<T>` (all required), `Readonly<T>` (all readonly), `Pick<T,K>` (select properties), `Omit<T,K>` (exclude properties), `Record<K,V>` (key-value map), `ReturnType<T>`, `Parameters<T>`.

**Q8: What are type guards?**
```typescript
// Type guards narrow types at runtime:
typeof x === "string"           // typeof guard
x instanceof MyClass            // instanceof guard
"property" in object            // in guard
function isUser(x: any): x is User {}  // custom type guard
```

**Q9: Explain `keyof` and `typeof`.**
```typescript
interface User { id: number; name: string; email: string; }
type Keys = keyof User;  // "id" | "name" | "email"

const config = { port: 3000, host: "localhost" };
type Config = typeof config; // { port: number; host: string }
```

**Q10: What are decorators?**
> Functions that attach metadata or modify classes, methods, or properties at design time. Used extensively in Angular (`@Component`) and NestJS (`@Controller`, `@Injectable`). Require `experimentalDecorators: true` in tsconfig.

## 20.3 Advanced Questions

**Q11: Explain conditional types.**
```typescript
type IsString<T> = T extends string ? "yes" : "no";
type A = IsString<string>;   // "yes"
type B = IsString<number>;   // "no"

// With infer â€” extract type from another type
type UnwrapPromise<T> = T extends Promise<infer U> ? U : T;
type X = UnwrapPromise<Promise<string>>; // string
type Y = UnwrapPromise<number>;          // number
```

**Q12: What is a discriminated union?**
```typescript
interface Circle { kind: "circle"; radius: number; }
interface Rect   { kind: "rect";   width: number; height: number; }
type Shape = Circle | Rect;

function area(s: Shape): number {
  switch (s.kind) {
    case "circle": return Math.PI * s.radius ** 2;
    case "rect":   return s.width * s.height;
  }
}
// The `kind` property is the discriminant â€” TypeScript narrows type in each case.
```

**Q13: What is declaration merging?**
> When two declarations with the same name exist, TypeScript merges them. Interfaces merge (properties combine), namespaces merge with classes/functions/enums. This is how `.d.ts` files extend built-in types.

**Q14: How do you create a type-safe event emitter?**
```typescript
type EventMap = {
  login: { userId: string; timestamp: Date };
  logout: { userId: string };
  error: { message: string; code: number };
};

class TypedEventEmitter<T extends Record<string, unknown>> {
  private listeners: Partial<{ [K in keyof T]: Array<(data: T[K]) => void> }> = {};

  on<K extends keyof T>(event: K, handler: (data: T[K]) => void): void {
    if (!this.listeners[event]) this.listeners[event] = [];
    this.listeners[event]!.push(handler);
  }

  emit<K extends keyof T>(event: K, data: T[K]): void {
    this.listeners[event]?.forEach(handler => handler(data));
  }
}

const emitter = new TypedEventEmitter<EventMap>();
emitter.on("login", (data) => {
  console.log(data.userId);    // âœ… TypeScript knows the shape
  // console.log(data.code);   // âŒ Error: 'code' doesn't exist on login event
});
emitter.emit("login", { userId: "U-1", timestamp: new Date() }); // âœ…
// emitter.emit("login", { wrong: true }); // âŒ Error
```

**Q15: Explain template literal types.**
```typescript
type HTTPMethod = "GET" | "POST" | "PUT" | "DELETE";
type Endpoint = "/users" | "/orders";
type Route = `${HTTPMethod} ${Endpoint}`;
// "GET /users" | "GET /orders" | "POST /users" | ... (8 combinations)
```

## 20.4 Best Practices

```typescript
// 1. Enable strict mode always
// "strict": true in tsconfig.json

// 2. Never use 'any' â€” use 'unknown' instead
function process(input: unknown): void { /* check type first */ }

// 3. Prefer 'interface' for objects, 'type' for unions/computed
interface User { name: string; }
type ID = string | number;

// 4. Use readonly when values shouldn't change
interface Config { readonly host: string; readonly port: number; }

// 5. Use const assertions for literal types
const ROLES = ["admin", "user", "viewer"] as const;
type Role = typeof ROLES[number]; // "admin" | "user" | "viewer"

// 6. Use discriminated unions for state management
type State =
  | { status: "loading" }
  | { status: "success"; data: unknown }
  | { status: "error"; message: string };

// 7. Prefer explicit return types for public functions
function getUser(id: number): User | undefined { /* ... */ return undefined; }

// 8. Use exhaustiveness checks in switch statements
function assertNever(x: never): never {
  throw new Error(`Unexpected value: ${x}`);
}

// 9. Use path aliases for clean imports
// import { User } from '@models/user';

// 10. Use generic constraints, not 'any'
function getLength<T extends { length: number }>(item: T): number {
  return item.length;
}
```

## 20.5 Common TypeScript Mistakes

```typescript
// âŒ Mistake 1: Using == instead of ===
if (value == null) {} // Compares both null AND undefined (sometimes ok)
if (value === null) {} // Only null (stricter)

// âŒ Mistake 2: Forgetting to handle null
function getName(user: User | null): string {
  // return user.name; // âŒ Object possibly null
  return user?.name ?? "Unknown"; // âœ…
}

// âŒ Mistake 3: Type assertion instead of type guard
const input = value as string; // Dangerous â€” what if it's not string?
if (typeof value === "string") { /* safe */ } // âœ… Runtime check

// âŒ Mistake 4: Not using generic constraints
function bad<T>(obj: T): void {
  // console.log(obj.name); // âŒ Property 'name' doesn't exist on T
}
function good<T extends { name: string }>(obj: T): void {
  console.log(obj.name); // âœ… Guaranteed by constraint
}

// âŒ Mistake 5: Mutable enum values
enum Status { Active, Inactive }
let s: Status = 999; // âœ… TypeScript allows any number! Use string enums instead.
enum SafeStatus { Active = "ACTIVE", Inactive = "INACTIVE" } // âœ… String enum
```

## 20.6 Quick Reference Cheat Sheet

```typescript
// Types
let s: string; let n: number; let b: boolean;
let arr: number[]; let tuple: [string, number];
let obj: { key: string }; let fn: (x: number) => string;

// Union & Intersection
type A = string | number;        // OR
type B = TypeA & TypeB;          // AND

// Generics
function id<T>(val: T): T { return val; }
interface Box<T> { value: T; }
class Stack<T> { items: T[] = []; push(item: T) { this.items.push(item); } }

// Utility Types
Partial<T>  Required<T>  Readonly<T>
Pick<T,K>   Omit<T,K>    Record<K,V>
ReturnType<F>  Parameters<F>  NonNullable<T>
Extract<T,U>   Exclude<T,U>   Awaited<T>

// Type Guards
typeof x === "string"
x instanceof MyClass
"prop" in obj
function isX(v: any): v is X { return true; }

// Key operators
keyof T          // union of keys
typeof variable  // type of a value
T[K]             // indexed access
T extends U ? X : Y  // conditional
```

---

*TypeScript Complete Training Guide â€” 20 Chapters â€” End*
