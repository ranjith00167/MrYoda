package com.mryoda.diagnostics.api.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logger Utility for framework-wide logging
 */
public class LoggerUtil {

    private static final Logger logger = LogManager.getLogger(LoggerUtil.class);

    private LoggerUtil() {
        // Private constructor
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void debug(String message) {
        logger.debug(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public static void fatal(String message) {
        logger.fatal(message);
    }

    public static void logTestStart(String testName) {
        logger.info("========================================");
        logger.info("Starting Test: " + testName);
        logger.info("========================================");
    }

    public static void logTestEnd(String testName) {
        logger.info("========================================");
        logger.info("Completed Test: " + testName);
        logger.info("========================================");
    }

    public static void logStep(String step) {
        logger.info("STEP: " + step);
    }
}
