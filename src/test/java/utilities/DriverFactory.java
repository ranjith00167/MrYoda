package utilities;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.PageLoadStrategy;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static WebDriver getDriver(String browserName) {

        if (driver.get() == null) {

            boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "false"));

            if (browserName.equalsIgnoreCase("chrome")) {

                WebDriverManager.chromedriver().setup();

                ChromeOptions options = new ChromeOptions();

                // ✅ Preferences
                Map<String, Object> prefs = new HashMap<>();
                Map<String, Object> profile = new HashMap<>();

                Map<String, Object> defaultContentSettings = new HashMap<>();
                defaultContentSettings.put("geolocation", 1);
                defaultContentSettings.put("media_stream_mic", 1);
                defaultContentSettings.put("notifications", 2);

                profile.put("default_content_setting_values", defaultContentSettings);
                prefs.put("profile", profile);

                options.setExperimentalOption("prefs", prefs);

                // ✅ Core stability flags
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-popup-blocking");
                options.addArguments("--disable-infobars");

                // 🔥 CRITICAL for headless stability
                options.addArguments("--disable-renderer-backgrounding");
                options.addArguments("--disable-background-timer-throttling");
                options.addArguments("--disable-backgrounding-occluded-windows");

                // ✅ Media permissions
                options.addArguments("--use-fake-ui-for-media-stream");
                options.addArguments("--use-fake-device-for-media-stream");

                // ✅ Page load strategy (faster + stable)
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

                if (isHeadless) {

                    options.addArguments("--headless=new");
                    options.addArguments("--window-size=1920,1080");

                    options.addArguments("--disable-gpu");
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");

                }

                WebDriver webDriver = new ChromeDriver(options);

                // ✅ Only maximize in headed mode
                if (!isHeadless) {
                    webDriver.manage().window().maximize();
                }

                // ❌ Remove implicit wait (use explicit waits only)
                webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

                driver.set(webDriver);
            }

            else {
                throw new IllegalArgumentException("Unsupported browser: " + browserName);
            }
        }

        return driver.get();
    }

    public static void quitDriver() {

        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
}