package com.example.aviation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FuelCalculationServiceTest {

    private FuelCalculationService service;

    @BeforeEach
    void setUp() {
        service = new FuelCalculationService();
    }

    @Test
    void testCalculateFuelSingleFlight() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = service.calculateFuel(departure);

        assertNotNull(result);
        assertEquals(departure, result.departure());
        assertTrue(result.distanceKm() > 0);
        assertTrue(result.cruiseFuelLiters() > 0);
        assertTrue(result.reserveFuelLiters() > 0);
        assertTrue(result.totalFuelLiters() > 0);
    }

    @Test
    void testCalculateFuelDistanceIsRounded() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                100,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = service.calculateFuel(departure);

        double distanceStr = String.valueOf(result.distanceKm()).split("\\.")[1].length();
        assertTrue(distanceStr <= 2, "Distance should be rounded to 2 decimals");
    }

    @Test
    void testCalculateFuelReserveFuelMatchesAirplaneModel() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                200,
                Airport.ORD,
                Airport.LHR
        );

        FuelCalculationResult result = service.calculateFuel(departure);

        assertEquals(
                AirplaneModel.BOEING_787_9.getGroundReserveLiters(),
                result.reserveFuelLiters(),
                0.01
        );
    }

    @Test
    void testCalculateFuelTotalFuelIncludesContingencyFactor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                250,
                Airport.CDG,
                Airport.MAD
        );

        FuelCalculationResult result = service.calculateFuel(departure);

        double expectedTotal = (result.cruiseFuelLiters() * 1.10) + result.reserveFuelLiters();
        assertEquals(expectedTotal, result.totalFuelLiters(), 0.02);
    }

    @Test
    void testCalculateFuelDifferentAirplaneModels() {
        Airport origin = Airport.LAX;
        Airport destination = Airport.ORD;
        int capacity = 150;

        FlightDeparture departure1 = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                capacity,
                origin,
                destination
        );

        FlightDeparture departure2 = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                capacity,
                origin,
                destination
        );

        FuelCalculationResult result1 = service.calculateFuel(departure1);
        FuelCalculationResult result2 = service.calculateFuel(departure2);

        assertEquals(result1.distanceKm(), result2.distanceKm(), 0.01);
        assertNotEquals(result1.cruiseFuelLiters(), result2.cruiseFuelLiters(), 0.01);
        assertNotEquals(result1.totalFuelLiters(), result2.totalFuelLiters(), 0.01);
    }

    @Test
    void testCalculateFuelDifferentCapacities() {
        Airport origin = Airport.LHR;
        Airport destination = Airport.DXB;

        FlightDeparture departure1 = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                200,
                origin,
                destination
        );

        FlightDeparture departure2 = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                300,
                origin,
                destination
        );

        FuelCalculationResult result1 = service.calculateFuel(departure1);
        FuelCalculationResult result2 = service.calculateFuel(departure2);

        assertEquals(result1.distanceKm(), result2.distanceKm(), 0.01);
        assertTrue(result2.cruiseFuelLiters() > result1.cruiseFuelLiters());
        assertTrue(result2.totalFuelLiters() > result1.totalFuelLiters());
    }

    @Test
    void testCalculateFuelForDepartures() {
        List<FlightDeparture> departures = Arrays.asList(
                new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        100,
                        Airport.JFK,
                        Airport.LAX
                ),
                new FlightDeparture(
                        AirplaneModel.AIRBUS_A320,
                        120,
                        Airport.ORD,
                        Airport.LHR
                ),
                new FlightDeparture(
                        AirplaneModel.BOEING_787_9,
                        200,
                        Airport.CDG,
                        Airport.DXB
                )
        );

        List<FuelCalculationResult> results = service.calculateFuelForDepartures(departures);

        assertEquals(3, results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals(departures.get(i), results.get(i).departure());
            assertTrue(results.get(i).totalFuelLiters() > 0);
        }
    }

    @Test
    void testCalculateFuelForDeparturesNullThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateFuelForDepartures(null)
        );
    }

    @Test
    void testCalculateFuelForDeparturesEmptyThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateFuelForDepartures(List.of())
        );
    }

    @Test
    void testTotalFuelLiters() {
        List<FlightDeparture> departures = Arrays.asList(
                new FlightDeparture(
                        AirplaneModel.AIRBUS_A320,
                        100,
                        Airport.JFK,
                        Airport.LAX
                ),
                new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        120,
                        Airport.ORD,
                        Airport.LHR
                )
        );

        List<FuelCalculationResult> results = service.calculateFuelForDepartures(departures);
        double totalFuel = service.totalFuelLiters(results);

        double expectedTotal = results.stream()
                .mapToDouble(FuelCalculationResult::totalFuelLiters)
                .sum();
        assertEquals(expectedTotal, totalFuel, 0.02);
        assertTrue(totalFuel > 0);
    }

    @Test
    void testTotalFuelLitersIsRounded() {
        List<FlightDeparture> departures = List.of(
                new FlightDeparture(
                        AirplaneModel.BOEING_787_9,
                        150,
                        Airport.MAD,
                        Airport.NRT
                )
        );

        List<FuelCalculationResult> results = service.calculateFuelForDepartures(departures);
        double totalFuel = service.totalFuelLiters(results);

        double distanceStr = String.valueOf(totalFuel).split("\\.")[1].length();
        assertTrue(distanceStr <= 2, "Total fuel should be rounded to 2 decimals");
    }

    @Test
    void testCalculateFuelConsistency() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result1 = service.calculateFuel(departure);
        FuelCalculationResult result2 = service.calculateFuel(departure);

        assertEquals(result1.distanceKm(), result2.distanceKm(), 0.01);
        assertEquals(result1.cruiseFuelLiters(), result2.cruiseFuelLiters(), 0.01);
        assertEquals(result1.totalFuelLiters(), result2.totalFuelLiters(), 0.01);
    }

    @Test
    void testCalculateFuelShortDistance() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                100,
                Airport.CDG,
                Airport.MAD
        );

        FuelCalculationResult result = service.calculateFuel(departure);

        assertTrue(result.distanceKm() > 0);
        assertTrue(result.cruiseFuelLiters() > 0);
        assertTrue(result.totalFuelLiters() > 0);
    }

    @Test
    void testCalculateFuelLongDistance() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.LAX,
                Airport.NRT
        );

        FuelCalculationResult result = service.calculateFuel(departure);

        assertTrue(result.distanceKm() > 5000);
        assertTrue(result.cruiseFuelLiters() > 0);
        assertTrue(result.totalFuelLiters() > result.cruiseFuelLiters());
    }
}
