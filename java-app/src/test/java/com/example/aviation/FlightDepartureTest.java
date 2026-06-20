package com.example.aviation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FlightDeparture Tests")
class FlightDepartureTest {

    @Test
    @DisplayName("Should create valid FlightDeparture with all valid parameters")
    void testValidFlightDeparture() {
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
    @DisplayName("Should create valid FlightDeparture with different airplane models")
    void testValidFlightDepartureWithDifferentModels() {
        FlightDeparture departure1 = new FlightDeparture(
                AirplaneModel.AIRBUS_A320,
                180,
                Airport.CDG,
                Airport.LHR
        );
        assertEquals(AirplaneModel.AIRBUS_A320, departure1.airplaneModel());
        assertEquals(180, departure1.capacity());

        FlightDeparture departure2 = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                290,
                Airport.DXB,
                Airport.NRT
        );
        assertEquals(AirplaneModel.BOEING_787_9, departure2.airplaneModel());
        assertEquals(290, departure2.capacity());
    }

    @Test
    @DisplayName("Should create valid FlightDeparture at maximum capacity")
    void testValidFlightDepartureAtMaxCapacity() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.BOEING_777_300ER,
                396,
                Airport.ORD,
                Airport.MAD
        );
        
        assertEquals(396, departure.capacity());
        assertEquals(AirplaneModel.BOEING_777_300ER.getMaxCapacity(), departure.capacity());
    }

    @Test
    @DisplayName("Should create valid FlightDeparture with minimum capacity")
    void testValidFlightDepartureWithMinimumCapacity() {
        FlightDeparture departure = new FlightDeparture(
                AirplaneModel.AIRBUS_A350_900,
                1,
                Airport.LHR,
                Airport.CDG
        );
        
        assertEquals(1, departure.capacity());
    }

    @Test
    @DisplayName("Should throw exception when airplaneModel is null")
    void testNullAirplaneModel() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(null, 100, Airport.JFK, Airport.LAX)
        );
        assertTrue(exception.getMessage().contains("airplaneModel is required"));
    }

    @Test
    @DisplayName("Should throw exception when origin is null")
    void testNullOrigin() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(AirplaneModel.BOEING_737_800, 100, null, Airport.LAX)
        );
        assertTrue(exception.getMessage().contains("origin and destination are required"));
    }

    @Test
    @DisplayName("Should throw exception when destination is null")
    void testNullDestination() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(AirplaneModel.BOEING_737_800, 100, Airport.JFK, null)
        );
        assertTrue(exception.getMessage().contains("origin and destination are required"));
    }

    @Test
    @DisplayName("Should throw exception when origin and destination are the same")
    void testOriginEqualsDestination() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        100,
                        Airport.JFK,
                        Airport.JFK
                )
        );
        assertTrue(exception.getMessage().contains("origin and destination must be different"));
    }

    @Test
    @DisplayName("Should throw exception when capacity is zero")
    void testCapacityZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        0,
                        Airport.JFK,
                        Airport.LAX
                )
        );
        assertTrue(exception.getMessage().contains("capacity must be greater than zero"));
    }

    @Test
    @DisplayName("Should throw exception when capacity is negative")
    void testCapacityNegative() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        -50,
                        Airport.JFK,
                        Airport.LAX
                )
        );
        assertTrue(exception.getMessage().contains("capacity must be greater than zero"));
    }

    @Test
    @DisplayName("Should throw exception when capacity exceeds maximum for Boeing 737-800")
    void testCapacityExceedsMaximumBoeing737() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_737_800,
                        200,
                        Airport.JFK,
                        Airport.LAX
                )
        );
        String message = exception.getMessage();
        assertTrue(message.contains("capacity 200 exceeds maximum"));
        assertTrue(message.contains("Boeing 737-800"));
        assertTrue(message.contains("189"));
    }

    @Test
    @DisplayName("Should throw exception when capacity exceeds maximum for Airbus A320")
    void testCapacityExceedsMaximumAirbusA320() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.AIRBUS_A320,
                        200,
                        Airport.CDG,
                        Airport.LHR
                )
        );
        String message = exception.getMessage();
        assertTrue(message.contains("capacity 200 exceeds maximum"));
        assertTrue(message.contains("Airbus A320"));
        assertTrue(message.contains("180"));
    }

    @Test
    @DisplayName("Should throw exception when capacity exceeds maximum for Boeing 787-9")
    void testCapacityExceedsMaximumBoeing787() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_787_9,
                        300,
                        Airport.DXB,
                        Airport.NRT
                )
        );
        String message = exception.getMessage();
        assertTrue(message.contains("capacity 300 exceeds maximum"));
        assertTrue(message.contains("Boeing 787-9"));
        assertTrue(message.contains("290"));
    }

    @Test
    @DisplayName("Should throw exception when capacity exceeds maximum for Airbus A350-900")
    void testCapacityExceedsMaximumAirbusA350() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.AIRBUS_A350_900,
                        400,
                        Airport.LHR,
                        Airport.CDG
                )
        );
        String message = exception.getMessage();
        assertTrue(message.contains("capacity 400 exceeds maximum"));
        assertTrue(message.contains("Airbus A350-900"));
        assertTrue(message.contains("325"));
    }

    @Test
    @DisplayName("Should throw exception when capacity exceeds maximum for Boeing 777-300ER")
    void testCapacityExceedsMaximumBoeing777() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FlightDeparture(
                        AirplaneModel.BOEING_777_300ER,
                        500,
                        Airport.ORD,
                        Airport.MAD
                )
        );
        String message = exception.getMessage();
        assertTrue(message.contains("capacity 500 exceeds maximum"));
        assertTrue(message.contains("Boeing 777-300ER"));
        assertTrue(message.contains("396"));
    }
}
