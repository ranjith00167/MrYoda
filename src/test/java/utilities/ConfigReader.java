package utilities;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static Properties properties = new Properties();

    static {
        try {
            // Load from classpath (e.g., src/main/resources/config.properties)
        	InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties");

            if (input != null) {
                properties.load(input);
            } else {
                throw new RuntimeException("❌ config.properties file not found in resources folder");
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load config.properties: " + e.getMessage(), e);
        }
    }

    // Get value by key
    public static String get(String key) {
        return properties.getProperty(key);
    }

    // Optional: Get with default value if key not found
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
