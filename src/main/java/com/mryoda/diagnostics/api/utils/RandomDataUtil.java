package com.mryoda.diagnostics.api.utils;

import java.util.Random;
import java.util.UUID;

public class RandomDataUtil {
    private static final Random rand = new Random();

    public static String getRandomFirstName() {
        String[] names = { "John", "Alex", "Chris", "Sam", "David", "Arjun", "Rahul", "Michael" };
        return names[rand.nextInt(names.length)];
    }

    public static String getRandomLastName() {
        String[] names = { "Doe", "Smith", "Sharma", "Patel", "Singh", "Khan", "Brown", "Wilson" };
        return names[rand.nextInt(names.length)];
    }

    public static String getRandomMiddleName() {
        return String.valueOf((char) ('A' + rand.nextInt(26)));
    }

    public static String getRandomGender() {
        return rand.nextBoolean() ? "male" : "female";
    }

    public static String getRandomDOB() {
        return getRandomNumber(1980, 2005) + "-" +
                String.format("%02d", getRandomNumber(1, 12)) + "-" +
                String.format("%02d", getRandomNumber(1, 28));
    }

    private static int getRandomNumber(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    public static String getRandomMobile() {
        return (6 + rand.nextInt(4)) + String.format("%09d", rand.nextInt(1_000_000_000));
    }

    public static String getRandomEmail() {
        return "auto_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    public static String getRandomProfilePic() {
        return "https://cdn.test.com/img/" + UUID.randomUUID().toString().substring(0, 6) + ".png";
    }
}
