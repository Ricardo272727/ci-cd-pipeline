package com.example.aviation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FuelCalculationResult")
class FuelCalculationResultTest {

    @Test
    @DisplayName("should create record with valid values")
    void shouldCreateRecordWithValidValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                3944.0,
                1500.0,
                4200.0,
                5850.0
        );
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("should access departure via accessor")
    void shouldAccessDepartureViaAccessor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                100,
                Airport.LHR,
                Airport.CDG
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                200.0,
                300.0,
                400.0,
                500.0
        );
        
        assertEquals(departure, result.departure());
        assertEquals(AirplaneModel.AIRBUS_A320, result.departure().airplaneModel());
        assertEquals(Airport.LHR, result.departure().origin());
        assertEquals(Airport.CDG, result.departure().destination());
    }

    @Test
    @DisplayName("should access distance kilometers via accessor")
    void shouldAccessDistanceKmViaAccessor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                200,
                Airport.ORD,
                Airport.MAD
        );
        
        double expectedDistance = 5900.50;
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                expectedDistance,
                1000.0,
                2000.0,
                3000.0
        );
        
        assertEquals(expectedDistance, result.distanceKm(), 0.02);
    }

    @Test
    @DisplayName("should access cruise fuel liters via accessor")
    void shouldAccessCruiseFuelLitersViaAccessor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                250,
                Airport.DXB,
                Airport.NRT
        );
        
        double expectedCruiseFuel = 2345.67;
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                4000.0,
                expectedCruiseFuel,
                1500.0,
                5000.0
        );
        
        assertEquals(expectedCruiseFuel, result.cruiseFuelLiters(), 0.02);
    }

    @Test
    @DisplayName("should access reserve fuel liters via accessor")
    void shouldAccessReserveFuelLitersViaAccessor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                300,
                Airport.JFK,
                Airport.LHR
        );
        
        double expectedReserveFuel = 6000.0;
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                5500.0,
                3000.0,
                expectedReserveFuel,
                9500.0
        );
        
        assertEquals(expectedReserveFuel, result.reserveFuelLiters(), 0.02);
    }

    @Test
    @DisplayName("should access total fuel liters via accessor")
    void shouldAccessTotalFuelLitersViaAccessor() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                180,
                Airport.LAX,
                Airport.ORD
        );
        
        double expectedTotalFuel = 7234.56;
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                2000.0,
                3000.0,
                4200.0,
                expectedTotalFuel
        );
        
        assertEquals(expectedTotalFuel, result.totalFuelLiters(), 0.02);
    }

    @Test
    @DisplayName("should establish equality between identical results")
    void shouldEstablishEqualityBetweenIdenticalResults() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                120,
                Airport.CDG,
                Airport.MAD
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure,
                450.0,
                800.0,
                3900.0,
                5700.0
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure,
                450.0,
                800.0,
                3900.0,
                5700.0
        );
        
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("should establish inequality between different results")
    void shouldEstablishInequalityBetweenDifferentResults() {
        FlightDeparture departure1 = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FlightDeparture departure2 = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                100,
                Airport.LHR,
                Airport.CDG
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure1,
                3944.0,
                1500.0,
                4200.0,
                5850.0
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure2,
                350.0,
                500.0,
                3900.0,
                4550.0
        );
        
        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("should generate consistent hash code for equal records")
    void shouldGenerateConsistentHashCodeForEqualRecords() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.ORD,
                Airport.MAD
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure,
                5900.0,
                4500.0,
                5500.0,
                10500.0
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure,
                5900.0,
                4500.0,
                5500.0,
                10500.0
        );
        
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("should provide meaningful string representation")
    void shouldProvideMeaningfulStringRepresentation() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                300,
                Airport.DXB,
                Airport.NRT
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                6700.0,
                5000.0,
                5800.0,
                11300.0
        );
        
        String stringRepresentation = result.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.contains("FuelCalculationResult"));
    }

    @Test
    @DisplayName("should handle zero distance for short flights")
    void shouldHandleSmallDistanceValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                100,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                0.0,
                0.0,
                4200.0,
                4200.0
        );
        
        assertEquals(0.0, result.distanceKm(), 0.02);
        assertEquals(0.0, result.cruiseFuelLiters(), 0.02);
        assertEquals(4200.0, result.reserveFuelLiters(), 0.02);
    }

    @Test
    @DisplayName("should handle large distance and fuel values")
    void shouldHandleLargeDistanceAndFuelValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                350,
                Airport.NRT,
                Airport.JFK
        );
        
        double largeDistance = 10800.0;
        double largeCruiseFuel = 150000.0;
        double largeReserveFuel = 7200.0;
        double largeTotalFuel = 170000.0;
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                largeDistance,
                largeCruiseFuel,
                largeReserveFuel,
                largeTotalFuel
        );
        
        assertEquals(largeDistance, result.distanceKm(), 0.02);
        assertEquals(largeCruiseFuel, result.cruiseFuelLiters(), 0.02);
        assertEquals(largeReserveFuel, result.reserveFuelLiters(), 0.02);
        assertEquals(largeTotalFuel, result.totalFuelLiters(), 0.02);
    }

    @Test
    @DisplayName("should maintain precision with decimal values")
    void shouldMaintainPrecisionWithDecimalValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                175,
                Airport.MAD,
                Airport.CDG
        );
        
        double decimalDistance = 456.78;
        double decimalCruiseFuel = 1234.56;
        double decimalReserveFuel = 3900.00;
        double decimalTotalFuel = 5456.72;
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                decimalDistance,
                decimalCruiseFuel,
                decimalReserveFuel,
                decimalTotalFuel
        );
        
        assertEquals(decimalDistance, result.distanceKm(), 0.02);
        assertEquals(decimalCruiseFuel, result.cruiseFuelLiters(), 0.02);
        assertEquals(decimalReserveFuel, result.reserveFuelLiters(), 0.02);
        assertEquals(decimalTotalFuel, result.totalFuelLiters(), 0.02);
    }
}
