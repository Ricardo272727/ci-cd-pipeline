package com.example.aviation;

public enum AirplaneModel {
    BOEING_737_800("Boeing 737-800", 189, 2.85, 4200),
    AIRBUS_A320("Airbus A320", 180, 2.65, 3900),
    BOEING_787_9("Boeing 787-9", 290, 3.10, 5500),
    AIRBUS_A350_900("Airbus A350-900", 325, 2.95, 5800),
    BOEING_777_300ER("Boeing 777-300ER", 396, 3.45, 7200);

    private final String displayName;
    private final int maxCapacity;
    private final double fuelLitersPerKmPerPassenger;
    private final double groundReserveLiters;

    AirplaneModel(
            String displayName,
            int maxCapacity,
            double fuelLitersPerKmPerPassenger,
            double groundReserveLiters
    ) {
        this.displayName = displayName;
        this.maxCapacity = maxCapacity;
        this.fuelLitersPerKmPerPassenger = fuelLitersPerKmPerPassenger;
        this.groundReserveLiters = groundReserveLiters;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public double getFuelLitersPerKmPerPassenger() {
        return fuelLitersPerKmPerPassenger;
    }

    public double getGroundReserveLiters() {
        return groundReserveLiters;
    }
}
