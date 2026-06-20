package com.example.aviation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FuelCalculationServiceTest {

    @Autowired
    private FuelCalculationService fuelCalculationService;

    @Test
    void testCalculateFuelBasicFlight() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(departure);

        assertNotNull(result);
        assertEquals(departure, result.departure());
        assertTrue(result.distanceKm() > 0);
        assertTrue(result.cruiseFuelLiters() > 0);
        assertEquals(4200, result.reserveFuelLiters());
        assertTrue(result.totalFuelLiters() > result.cruiseFuelLiters());
    }

    @Test
    void testCalculateFuelContingencyFactor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                100,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(departure);

        double expectedTotal = (result.cruiseFuelLiters() * 1.10) + result.reserveFuelLiters();
        assertEquals(expectedTotal, result.totalFuelLiters(), 0.01);
    }

    @Test
    void testCalculateFuelMultipleAirplaneModels() {
        FlightDeparture boeingFlight = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        FlightDeparture airbusFlight = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                150,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult boeingResult = fuelCalculationService.calculateFuel(boeingFlight);
        FuelCalculationResult airbusResult = fuelCalculationService.calculateFuel(airbusFlight);

        assertNotEquals(boeingResult.cruiseFuelLiters(), airbusResult.cruiseFuelLiters());
        assertEquals(boeingResult.distanceKm(), airbusResult.distanceKm());
    }

    @Test
    void testCalculateFuelVariousCapacities() {
        FlightDeparture lowCapacity = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                100,
                Airport.LHR,
                Airport.CDG
        );
        FlightDeparture highCapacity = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.LHR,
                Airport.CDG
        );

        FuelCalculationResult lowResult = fuelCalculationService.calculateFuel(lowCapacity);
        FuelCalculationResult highResult = fuelCalculationService.calculateFuel(highCapacity);

        assertTrue(highResult.cruiseFuelLiters() > lowResult.cruiseFuelLiters());
        assertEquals(lowResult.distanceKm(), highResult.distanceKm());
    }

    @Test
    void testCalculateFuelForDepartureSingleFlight() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                200,
                Airport.CDG,
                Airport.DXB
        );

        List<FuelCalculationResult> results = fuelCalculationService.calculateFuelForDepartures(
                List.of(departure)
        );

        assertEquals(1, results.size());
        assertEquals(departure, results.get(0).departure());
    }

    @Test
    void testCalculateFuelForDeparturesMultipleFlights() {
        FlightDeparture flight1 = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        FlightDeparture flight2 = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                120,
                Airport.ORD,
                Airport.LHR
        );
        FlightDeparture flight3 = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                300,
                Airport.MAD,
                Airport.NRT
        );

        List<FuelCalculationResult> results = fuelCalculationService.calculateFuelForDepartures(
                List.of(flight1, flight2, flight3)
        );

        assertEquals(3, results.size());
        assertEquals(flight1, results.get(0).departure());
        assertEquals(flight2, results.get(1).departure());
        assertEquals(flight3, results.get(2).departure());
    }

    @Test
    void testCalculateFuelForDeparturesNullList() {
        assertThrows(IllegalArgumentException.class, () ->
                fuelCalculationService.calculateFuelForDepartures(null)
        );
    }

    @Test
    void testCalculateFuelForDeparturesEmptyList() {
        assertThrows(IllegalArgumentException.class, () ->
                fuelCalculationService.calculateFuelForDepartures(List.of())
        );
    }

    @Test
    void testTotalFuelLitersSingleResult() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(departure);
        double totalFuel = fuelCalculationService.totalFuelLiters(List.of(result));

        assertEquals(result.totalFuelLiters(), totalFuel);
    }

    @Test
    void testTotalFuelLitersMultipleResults() {
        FlightDeparture flight1 = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        FlightDeparture flight2 = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                120,
                Airport.ORD,
                Airport.LHR
        );

        FuelCalculationResult result1 = fuelCalculationService.calculateFuel(flight1);
        FuelCalculationResult result2 = fuelCalculationService.calculateFuel(flight2);

        double totalFuel = fuelCalculationService.totalFuelLiters(List.of(result1, result2));
        double expectedTotal = result1.totalFuelLiters() + result2.totalFuelLiters();

        assertEquals(expectedTotal, totalFuel, 0.01);
    }

    @Test
    void testCalculateFuelLongDistanceFlight() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.JFK,
                Airport.NRT
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(departure);

        assertTrue(result.distanceKm() > 10000);
        assertTrue(result.cruiseFuelLiters() > 0);
        assertTrue(result.totalFuelLiters() > 0);
    }

    @Test
    void testCalculateFuelShortDistanceFlight() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                100,
                Airport.LHR,
                Airport.CDG
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(departure);

        assertTrue(result.distanceKm() > 0 && result.distanceKm() < 1000);
        assertTrue(result.cruiseFuelLiters() > 0);
    }

    @Test
    void testCalculateFuelRoundingPrecision() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                189,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(departure);

        String distanceStr = String.format("%.2f", result.distanceKm());
        String cruiseFuelStr = String.format("%.2f", result.cruiseFuelLiters());
        String totalFuelStr = String.format("%.2f", result.totalFuelLiters());

        assertEquals(result.distanceKm(), Double.parseDouble(distanceStr), 0.001);
        assertEquals(result.cruiseFuelLiters(), Double.parseDouble(cruiseFuelStr), 0.001);
        assertEquals(result.totalFuelLiters(), Double.parseDouble(totalFuelStr), 0.001);
    }
}
