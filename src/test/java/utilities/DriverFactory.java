package utilities;

import org.openqa.selenium.WebDriver;

import org.openqa.selenium.chrome.ChromeDriver;

import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.edge.EdgeDriver;

import org.openqa.selenium.edge.EdgeOptions;

import org.openqa.selenium.firefox.FirefoxDriver;

import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static WebDriver getDriver(String browserName) {

        if (driver.get() == null) {

            boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "false"));

            switch (browserName.toLowerCase()) {

                case "chrome":
    WebDriverManager.chromedriver().setup();

    ChromeOptions chromeOptions = new ChromeOptions();

    Map<String, Object> prefs = new HashMap<>();
    Map<String, Object> profile = new HashMap<>();

    // ─── default_content_setting_values ───────────────────────────────────
    // 1 = Allow, 2 = Block, 3 = Ask
    Map<String, Object> defaultContentSettings = new HashMap<>();
    defaultContentSettings.put("geolocation",        1);  // ✅ Allow location
    defaultContentSettings.put("media_stream_mic",   1);  // ✅ Allow microphone
    defaultContentSettings.put("media_stream_camera",2);  // 🚫 Block camera (not needed)
    defaultContentSettings.put("notifications",      2);  // 🚫 Block notification popup
    profile.put("default_content_setting_values", defaultContentSettings);

    // ─── managed_default_content_settings (policy-level override) ─────────
    Map<String, Object> managedContentSettings = new HashMap<>();
    managedContentSettings.put("geolocation",      1);   // ✅ Allow location
    managedContentSettings.put("media_stream_mic", 1);   // ✅ Allow microphone
    profile.put("managed_default_content_settings", managedContentSettings);

    prefs.put("profile", profile);

    chromeOptions.setExperimentalOption("prefs", prefs);

    // ─── Chrome flags ───────────────────────────────────────────────────────
    // Bypass OS-level mic permission dialog (uses fake device — no popup)
    chromeOptions.addArguments("--use-fake-ui-for-media-stream");
    chromeOptions.addArguments("--use-fake-device-for-media-stream");
    // Ensure geolocation API is available on staging origin
    chromeOptions.addArguments("--unsafely-treat-insecure-origin-as-secure=https://staging-mryoda.yodaprojects.com");
    // Suppress notification permission popup
    chromeOptions.addArguments("--disable-notifications");
    // Don't show "Chrome is being controlled by automated software" bar
    chromeOptions.addArguments("--disable-infobars");
    chromeOptions.addArguments("--disable-popup-blocking");

    if (isHeadless) {
        chromeOptions.addArguments("--headless=new");
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--start-maximized");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-allow-origins=*");
    }

    driver.set(new ChromeDriver(chromeOptions));
    break;

                case "firefox":

                    WebDriverManager.firefoxdriver().setup();

                    FirefoxOptions firefoxOptions = new FirefoxOptions();

                    if (isHeadless) {

                        firefoxOptions.addArguments("--headless");

                    }

                    driver.set(new FirefoxDriver(firefoxOptions));

                    break;

                case "edge":

                    WebDriverManager.edgedriver().setup();

                    EdgeOptions edgeOptions = new EdgeOptions();

                    if (isHeadless) {

                        edgeOptions.addArguments("headless");

                        edgeOptions.addArguments("window-size=1920,1080");

                    }

                    driver.set(new EdgeDriver(edgeOptions));

                    break;

                default:

                    throw new IllegalArgumentException("Unsupported browser: " + browserName);

            }

            driver.get().manage().window().maximize();

            driver.get().manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

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
