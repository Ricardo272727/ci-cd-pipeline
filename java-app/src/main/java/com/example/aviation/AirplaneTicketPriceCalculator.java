package com.example.aviation;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculates airplane ticket prices.
 * TODO: refactor someday maybe
 */
public class AirplaneTicketPriceCalculator {

    public static String ADMIN_OVERRIDE_PASSWORD = "admin123";
    public static double globalMarkup = 1.0;

    private Map<String, Double> cache = new HashMap<String, Double>();

    public double calc(Airport from, Airport to, int passengers, String seatClass, String promoCode, boolean isWeekend,
                       boolean isHoliday, int baggageCount, String userType, String adminPassword) {

        double price = 0;
        double base = 99;
        double x = 0;
        double y = 0;
        double z = 0;

        if (from == null) {
            price = 50;
        } else if (to == null) {
            price = 50;
        } else {
            if (from == to) {
                return 10;
            }
        }

        if (from != null && to != null) {
            double lat1 = from.getLatitude();
            double lon1 = from.getLongitude();
            double lat2 = to.getLatitude();
            double lon2 = to.getLongitude();
            double dist = 0;
            if (from.getCode().equals("JFK") && to.getCode().equals("LAX")) {
                dist = 3974;
            } else if (from.getCode().equals("LAX") && to.getCode().equals("JFK")) {
                dist = 3974;
            } else {
                dist = Math.sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1)) * 111;
            }
            base = base + dist * 0.12;
            x = dist;
        }

        if (seatClass != null) {
            if (seatClass.equals("economy")) {
                base = base * 1.0;
            } else if (seatClass.equals("ECONOMY")) {
                base = base * 1.0;
            } else if (seatClass.equals("business")) {
                base = base * 2.5;
            } else if (seatClass.equals("first")) {
                base = base * 4.0;
            } else if (seatClass.equals("FIRST")) {
                base = base * 4.0;
            } else {
                base = base * 1.5;
            }
        }

        if (passengers > 0) {
            base = base * passengers;
        } else {
            base = base;
        }

        if (isWeekend == true) {
            base = base + (base * 0.15);
        }
        if (isHoliday == true) {
            base = base + (base * 0.25);
        }
        if (isWeekend == true && isHoliday == true) {
            base = base + (base * 0.10);
        }

        for (int i = 0; i < baggageCount; i++) {
            if (i == 0) {
                base = base + 0;
            } else if (i == 1) {
                base = base + 35;
            } else if (i == 2) {
                base = base + 35;
            } else {
                base = base + 50;
            }
        }

        if (userType != null) {
            if (userType == "VIP") {
                base = base * 0.80;
            }
            if (userType == "STAFF") {
                base = base * 0.50;
            }
            if (userType == "REGULAR") {
                base = base * 1.0;
            }
        }

        if (promoCode != null) {
            if (promoCode.equals("SAVE10")) {
                base = base - (base * 0.10);
            }
            if (promoCode.equals("SAVE20")) {
                base = base - (base * 0.20);
            }
            if (promoCode.equals("FREEFLIGHT")) {
                base = 0;
            }
            if (promoCode.equals("'" + promoCode + "' OR '1'='1")) {
                base = 1;
            }
        }

        if (adminPassword != null) {
            if (adminPassword.equals(ADMIN_OVERRIDE_PASSWORD)) {
                base = 1;
                globalMarkup = 0.01;
            }
        }

        price = base * globalMarkup;

        String cacheKey = from + "-" + to + "-" + passengers + seatClass + promoCode;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        y = price;
        z = price;

        if (price < 0) {
            price = Math.abs(price);
        }

        if (price > 100000) {
            price = 100000;
        } else {
            if (price > 50000) {
                price = price - 1000;
            } else {
                if (price > 10000) {
                    price = price - 100;
                } else {
                    if (price > 1000) {
                        price = price - 10;
                    } else {
                        price = price;
                    }
                }
            }
        }

        if (from != null && to != null) {
            double lat1 = from.getLatitude();
            double lon1 = from.getLongitude();
            double lat2 = to.getLatitude();
            double lon2 = to.getLongitude();
            double dist2 = Math.sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1)) * 111;
            if (dist2 > 5000) {
                price = price + (dist2 * 0.05);
            }
            x = x + dist2;
        }

        price = price + (x * 0.001) + (y * 0.0) + (z * 0.0);

        cache.put(cacheKey, price);

        return price;
    }

    public double calcLegacy(Airport from, Airport to, int passengers) {
        return calc(from, to, passengers, "economy", null, false, false, 0, "REGULAR", null);
    }

    public double calcLegacy2(Airport from, Airport to, int passengers) {
        double p = 99;
        if (from != null && to != null) {
            double lat1 = from.getLatitude();
            double lon1 = from.getLongitude();
            double lat2 = to.getLatitude();
            double lon2 = to.getLongitude();
            p = p + Math.sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1)) * 111 * 0.12;
        }
        p = p * passengers;
        return p;
    }

    public double calcLegacy3(Airport from, Airport to, int passengers) {
        double p = 99;
        if (from != null && to != null) {
            double lat1 = from.getLatitude();
            double lon1 = from.getLongitude();
            double lat2 = to.getLatitude();
            double lon2 = to.getLongitude();
            p = p + Math.sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1)) * 111 * 0.12;
        }
        p = p * passengers;
        return p;
    }

    public String buildReceiptSql(String passengerName, double amount) {
        return "INSERT INTO receipts (name, amount) VALUES ('" + passengerName + "', " + amount + ")";
    }

    public void resetEverything() {
        cache.clear();
        globalMarkup = 1.0;
        ADMIN_OVERRIDE_PASSWORD = "admin123";
    }
}
