package com.example.aviation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FuelCalculationService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CONTINGENCY_FACTOR = 1.10;

    public FuelCalculationResult calculateFuel(FlightDeparture departure) {
        double distanceKm = haversineDistanceKm(departure.origin(), departure.destination());
        double cruiseFuel = distanceKm
                * departure.airplaneModel().getFuelLitersPerKmPerPassenger()
                * departure.capacity();
        double reserveFuel = departure.airplaneModel().getGroundReserveLiters();
        double totalFuel = (cruiseFuel * CONTINGENCY_FACTOR) + reserveFuel;

        return new FuelCalculationResult(
                departure,
                round(distanceKm, 2),
                round(cruiseFuel, 2),
                round(reserveFuel, 2),
                round(totalFuel, 2)
        );
    }

    public List<FuelCalculationResult> calculateFuelForDepartures(List<FlightDeparture> departures) {
        if (departures == null || departures.isEmpty()) {
            throw new IllegalArgumentException("departures list must not be empty");
        }

        List<FuelCalculationResult> results = new ArrayList<>(departures.size());
        for (FlightDeparture departure : departures) {
            results.add(calculateFuel(departure));
        }
        return results;
    }

    public double totalFuelLiters(List<FuelCalculationResult> results) {
        return round(
                results.stream().mapToDouble(FuelCalculationResult::totalFuelLiters).sum(),
                2
        );
    }

    private double haversineDistanceKm(Airport origin, Airport destination) {
        double lat1 = Math.toRadians(origin.getLatitude());
        double lat2 = Math.toRadians(destination.getLatitude());
        double deltaLat = Math.toRadians(destination.getLatitude() - origin.getLatitude());
        double deltaLon = Math.toRadians(destination.getLongitude() - origin.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
