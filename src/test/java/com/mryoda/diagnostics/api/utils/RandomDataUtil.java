package com.mryoda.diagnostics.api.utils;

import java.util.Random;

public class RandomDataUtil {

    private static final String[] FIRST_NAMES = {
            "John", "Jane", "Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Henry",
            "Ivy", "Jack", "Kathy", "Leo", "Mia", "Nina", "Oscar", "Paul", "Quinn", "Rose"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor",
            "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson"
    };

    public static String getRandomFirstName() {
        return FIRST_NAMES[new Random().nextInt(FIRST_NAMES.length)] + System.currentTimeMillis();
    }

    public static String getRandomLastName() {
        return LAST_NAMES[new Random().nextInt(LAST_NAMES.length)];
    }

    public static String getRandomMiddleName() {
        return ""; // Optional
    }

    public static String getRandomMobile() {
        // Generate a random 10-digit mobile number starting with 6-9
        long number = (long) (Math.random() * 1000000000L) + 6000000000L;
        return String.valueOf(number);
    }

    public static String getRandomGender() {
        return new Random().nextBoolean() ? "male" : "female";
    }

    public static String getRandomDOB() {
        int year = 1970 + new Random().nextInt(30); // 1970-2000
        int month = 1 + new Random().nextInt(12);
        int day = 1 + new Random().nextInt(28);
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    public static String getRandomEmail() {
        return "test" + System.nanoTime() + "@example.com";
    }

    public static String getRandomProfilePic() {
        return ""; // Empty string for now
    }
}
