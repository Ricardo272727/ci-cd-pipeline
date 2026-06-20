package com.example.aviation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightDepartureTest {

    @Test
    void testValidConstruction() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );

        assertEquals(AirplaneModel.BOEING_737_800, departure.airplaneModel());
        assertEquals(150, departure.capacity());
        assertEquals(Airport.JFK, departure.origin());
        assertEquals(Airport.LAX, departure.destination());
    }

    @Test
    void testConstructionWithMaxCapacity() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                189,
                Airport.JFK,
                Airport.LAX
        );

        assertEquals(189, departure.capacity());
    }

    @Test
    void testConstructionWithMinimumCapacity() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                1,
                Airport.ORD,
                Airport.CDG
        );

        assertEquals(1, departure.capacity());
    }

    @Test
    void testAirplaneModelNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(null, 100, Airport.JFK, Airport.LAX)
        );

        assertTrue(ex.getMessage().contains("airplaneModel is required"));
    }

    @Test
    void testOriginNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        100,
                        null,
                        Airport.LAX
                )
        );

        assertTrue(ex.getMessage().contains("origin and destination are required"));
    }

    @Test
    void testDestinationNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        100,
                        Airport.JFK,
                        null
                )
        );

        assertTrue(ex.getMessage().contains("origin and destination are required"));
    }

    @Test
    void testOriginEqualDestination() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        100,
                        Airport.JFK,
                        Airport.JFK
                )
        );

        assertTrue(ex.getMessage().contains("origin and destination must be different"));
    }

    @Test
    void testCapacityZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        0,
                        Airport.JFK,
                        Airport.LAX
                )
        );

        assertTrue(ex.getMessage().contains("capacity must be greater than zero"));
    }

    @Test
    void testCapacityNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        -50,
                        Airport.JFK,
                        Airport.LAX
                )
        );

        assertTrue(ex.getMessage().contains("capacity must be greater than zero"));
    }

    @Test
    void testCapacityExceedsMax() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        190,
                        Airport.JFK,
                        Airport.LAX
                )
        );

        assertTrue(ex.getMessage().contains("capacity 190 exceeds maximum for Boeing 737-800 (189)"));
    }

    @Test
    void testMultipleValidAirplaneModels() {
        FlightDeparture departure1 = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                300,
                Airport.LHR,
                Airport.DXB
        );

        FlightDeparture departure2 = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.CDG,
                Airport.NRT
        );

        assertEquals(AirplaneModel.AIRBUS_A350_900, departure1.airplaneModel());
        assertEquals(AirplaneModel.BOEING_787_9, departure2.airplaneModel());
    }

    @Test
    void testMultipleValidAirports() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                350,
                Airport.MAD,
                Airport.ORD
        );

        assertEquals(Airport.MAD, departure.origin());
        assertEquals(Airport.ORD, departure.destination());
    }
}
