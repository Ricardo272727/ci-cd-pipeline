package com.example.aviation;

public record FuelCalculationResult(
        FlightDeparture departure,
        double distanceKm,
        double cruiseFuelLiters,
        double reserveFuelLiters,
        double totalFuelLiters
) {
}
