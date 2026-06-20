package com.example.aviation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuelCalculationResultTest {

    @Test
    void testRecordConstructorAndAccessors() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        assertEquals(departure, result.departure());
        assertEquals(4000.5, result.distanceKm());
        assertEquals(1500.75, result.cruiseFuelLiters());
        assertEquals(100.25, result.reserveFuelLiters());
        assertEquals(2750.50, result.totalFuelLiters());
    }

    @Test
    void testEqualsWithIdenticalValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testNotEqualWithDifferentDistance() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure,
                5000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        assertNotEquals(result1, result2);
    }

    @Test
    void testNotEqualWithDifferentFuelValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure,
                4000.5,
                1600.75,
                100.25,
                2750.50
        );
        
        assertNotEquals(result1, result2);
    }

    @Test
    void testNotEqualWithDifferentDeparture() {
        FlightDeparture departure1 = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        
        FlightDeparture departure2 = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                160,
                Airport.ORD,
                Airport.LHR
        );
        
        FuelCalculationResult result1 = new FuelCalculationResult(
                departure1,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        FuelCalculationResult result2 = new FuelCalculationResult(
                departure2,
                4000.5,
                1500.75,
                100.25,
                2750.50
        );
        
        assertNotEquals(result1, result2);
    }

    @Test
    void testToString() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.CDG,
                Airport.DXB
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                5500.0,
                2200.0,
                150.0,
                3550.0
        );
        
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("FuelCalculationResult"));
        assertTrue(str.length() > 0);
    }

    @Test
    void testWithZeroFuel() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                100,
                Airport.LAX,
                Airport.ORD
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                0.0,
                0.0,
                0.0,
                0.0
        );
        
        assertEquals(0.0, result.distanceKm());
        assertEquals(0.0, result.cruiseFuelLiters());
        assertEquals(0.0, result.reserveFuelLiters());
        assertEquals(0.0, result.totalFuelLiters());
    }

    @Test
    void testWithLargeFuelValues() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                350,
                Airport.NRT,
                Airport.MAD
        );
        
        FuelCalculationResult result = new FuelCalculationResult(
                departure,
                15000.75,
                8500.50,
                7200.0,
                16350.55
        );
        
        assertEquals(15000.75, result.distanceKm());
        assertEquals(8500.50, result.cruiseFuelLiters());
        assertEquals(7200.0, result.reserveFuelLiters());
        assertEquals(16350.55, result.totalFuelLiters());
    }
}
