import re

file_path = r'c:\Users\RANJITH\MrYoda\src\test\java\utilities\BaseClass.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace the static driver field declaration with getter
# Look for 'public static WebDriver driver;' allowing for whitespace
content = re.sub(
    r'public\s+static\s+WebDriver\s+driver\s*;', 
    '// public static WebDriver driver; // Replaced by getDriver()\\n    public static WebDriver getDriver() { return utilities.DriverFactory.getDriver(); }', 
    content
)

# 2. Rename method parameters 'WebDriver driver' to 'WebDriver driver_ignored' so we don't accidentally replace the declaration keyword usage
# This regex looks for 'WebDriver' followed by whitespace and 'driver', ensuring we capture the whole parameter decl
content = re.sub(r'\bWebDriver\s+driver\b', 'WebDriver driver_ignored', content)

# 3. Replace usages of 'driver' variable with 'getDriver()'
# We rely on word boundaries \b. 
# We must ensure we don't replace 'driver_ignored' (handled by \b since _ is part of word chars usually? Yes).
# Also ensure we don't replace 'getDriver' (handled).
# Also 'webdriver' in comments? 'WebDriver'? \b handles casing if strict. Content is case sensitive.
content = re.sub(r'\bdriver\b', 'getDriver()', content)

# 4. Fix double getDriver() calls if BaseClass.driver was used
# BaseClass.driver -> BaseClass.getDriver()
# Then BaseClass.getDriver(). -> BaseClass.getDriver()(). (Wait, 'driver' replacement adds '()')
# BaseClass.driver -> BaseClass.getDriver()()
# We should fix 'getDriver()()' -> 'getDriver()' just in case
content = content.replace('getDriver()()', 'getDriver()')

# 5. Fix possible 'BaseClass.getDriver()' issues if any
# If original was BaseClass.driver, it became BaseClass.getDriver().
# If original was driver.findElement, it became getDriver().findElement.

# One edge case: 'driver = ' assignments.
# 'getDriver() = ' is invalid.
# usages like 'driver = new ChromeDriver()' should be removed or handled.
# But BaseClass shouldn't be assigning driver (except in init).
# We commented out assignment in Hooks.
# Check if BaseClass assigns driver.
# If so, it will create invalid code 'getDriver() = ...'.
# We can regex replace 'getDriver\(\)\s*=' with '// getDriver() =' to comment it out or fix manually.
content = re.sub(r'getDriver\(\)\s*=', '// getDriver() =', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
