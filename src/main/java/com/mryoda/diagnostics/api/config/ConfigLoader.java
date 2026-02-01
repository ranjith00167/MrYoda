package com.mryoda.diagnostics.api.config;

import org.aeonbits.owner.ConfigFactory;

/**
 * Configuration Loader to load and provide configuration
 */
public class ConfigLoader {

    private static ConfigManager config;

    private ConfigLoader() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the configuration instance
     * 
     * @return ConfigManager instance
     */
    public static ConfigManager getConfig() {
        if (config == null) {
            config = ConfigFactory.create(ConfigManager.class);
        }
        return config;
    }

    /**
     * Reload configuration
     */
    public static void reloadConfig() {
        config = ConfigFactory.create(ConfigManager.class);
    }
}
