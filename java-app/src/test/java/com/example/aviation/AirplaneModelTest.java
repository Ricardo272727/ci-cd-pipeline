package com.example.aviation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AirplaneModel Enum Tests")
class AirplaneModelTest {

    @Test
    @DisplayName("BOEING_737_800 has correct properties")
    void testBoeing737800Properties() {
        AirplaneModel model = AirplaneModel.BOEING_737_800;
        assertEquals("Boeing 737-800", model.getDisplayName());
        assertEquals(189, model.getMaxCapacity());
        assertEquals(2.85, model.getFuelLitersPerKmPerPassenger());
        assertEquals(4200.0, model.getGroundReserveLiters());
    }

    @Test
    @DisplayName("AIRBUS_A320 has correct properties")
    void testAirbusA320Properties() {
        AirplaneModel model = AirplaneModel.AIRBUS_A320;
        assertEquals("Airbus A320", model.getDisplayName());
        assertEquals(180, model.getMaxCapacity());
        assertEquals(2.65, model.getFuelLitersPerKmPerPassenger());
        assertEquals(3900.0, model.getGroundReserveLiters());
    }

    @Test
    @DisplayName("BOEING_787_9 has correct properties")
    void testBoeing7879Properties() {
        AirplaneModel model = AirplaneModel.BOEING_787_9;
        assertEquals("Boeing 787-9", model.getDisplayName());
        assertEquals(290, model.getMaxCapacity());
        assertEquals(3.10, model.getFuelLitersPerKmPerPassenger());
        assertEquals(5500.0, model.getGroundReserveLiters());
    }

    @Test
    @DisplayName("AIRBUS_A350_900 has correct properties")
    void testAirbusA350900Properties() {
        AirplaneModel model = AirplaneModel.AIRBUS_A350_900;
        assertEquals("Airbus A350-900", model.getDisplayName());
        assertEquals(325, model.getMaxCapacity());
        assertEquals(2.95, model.getFuelLitersPerKmPerPassenger());
        assertEquals(5800.0, model.getGroundReserveLiters());
    }

    @Test
    @DisplayName("BOEING_777_300ER has correct properties")
    void testBoeing777300erProperties() {
        AirplaneModel model = AirplaneModel.BOEING_777_300ER;
        assertEquals("Boeing 777-300ER", model.getDisplayName());
        assertEquals(396, model.getMaxCapacity());
        assertEquals(3.45, model.getFuelLitersPerKmPerPassenger());
        assertEquals(7200.0, model.getGroundReserveLiters());
    }

    @Test
    @DisplayName("All enum constants are accessible")
    void testAllEnumConstantsExist() {
        AirplaneModel[] models = AirplaneModel.values();
        assertEquals(5, models.length);
        assertNotNull(AirplaneModel.BOEING_737_800);
        assertNotNull(AirplaneModel.AIRBUS_A320);
        assertNotNull(AirplaneModel.BOEING_787_9);
        assertNotNull(AirplaneModel.AIRBUS_A350_900);
        assertNotNull(AirplaneModel.BOEING_777_300ER);
    }

    @Test
    @DisplayName("valueOf returns correct enum constant")
    void testValueOf() {
        assertEquals(AirplaneModel.BOEING_737_800, AirplaneModel.valueOf("BOEING_737_800"));
        assertEquals(AirplaneModel.AIRBUS_A320, AirplaneModel.valueOf("AIRBUS_A320"));
        assertEquals(AirplaneModel.BOEING_787_9, AirplaneModel.valueOf("BOEING_787_9"));
    }

    @Test
    @DisplayName("Max capacity is positive for all models")
    void testAllModelsHavePositiveCapacity() {
        for (AirplaneModel model : AirplaneModel.values()) {
            assertTrue(model.getMaxCapacity() > 0, "Max capacity must be positive for " + model.getDisplayName());
        }
    }

    @Test
    @DisplayName("Fuel consumption is positive for all models")
    void testAllModelsHavePositiveFuelConsumption() {
        for (AirplaneModel model : AirplaneModel.values()) {
            assertTrue(model.getFuelLitersPerKmPerPassenger() > 0, 
                    "Fuel consumption must be positive for " + model.getDisplayName());
        }
    }

    @Test
    @DisplayName("Ground reserve fuel is positive for all models")
    void testAllModelsHavePositiveGroundReserve() {
        for (AirplaneModel model : AirplaneModel.values()) {
            assertTrue(model.getGroundReserveLiters() > 0, 
                    "Ground reserve must be positive for " + model.getDisplayName());
        }
    }
}
