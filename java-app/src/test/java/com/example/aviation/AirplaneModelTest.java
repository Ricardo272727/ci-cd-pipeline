package com.example.aviation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirplaneModelTest {

    @Test
    void testBOEING_737_800Properties() {
        AirplaneModel model = AirplaneModel.BOEING_737_800;
        assertEquals("Boeing 737-800", model.getDisplayName());
        assertEquals(189, model.getMaxCapacity());
        assertEquals(2.85, model.getFuelLitersPerKmPerPassenger(), 0.01);
        assertEquals(4200, model.getGroundReserveLiters(), 0.01);
    }

    @Test
    void testAIRBUS_A320Properties() {
        AirplaneModel model = AirplaneModel.AIRBUS_A320;
        assertEquals("Airbus A320", model.getDisplayName());
        assertEquals(180, model.getMaxCapacity());
        assertEquals(2.65, model.getFuelLitersPerKmPerPassenger(), 0.01);
        assertEquals(3900, model.getGroundReserveLiters(), 0.01);
    }

    @Test
    void testBOEING_787_9Properties() {
        AirplaneModel model = AirplaneModel.BOEING_787_9;
        assertEquals("Boeing 787-9", model.getDisplayName());
        assertEquals(290, model.getMaxCapacity());
        assertEquals(3.10, model.getFuelLitersPerKmPerPassenger(), 0.01);
        assertEquals(5500, model.getGroundReserveLiters(), 0.01);
    }

    @Test
    void testAIRBUS_A350_900Properties() {
        AirplaneModel model = AirplaneModel.AIRBUS_A350_900;
        assertEquals("Airbus A350-900", model.getDisplayName());
        assertEquals(325, model.getMaxCapacity());
        assertEquals(2.95, model.getFuelLitersPerKmPerPassenger(), 0.01);
        assertEquals(5800, model.getGroundReserveLiters(), 0.01);
    }

    @Test
    void testBOEING_777_300ERProperties() {
        AirplaneModel model = AirplaneModel.BOEING_777_300ER;
        assertEquals("Boeing 777-300ER", model.getDisplayName());
        assertEquals(396, model.getMaxCapacity());
        assertEquals(3.45, model.getFuelLitersPerKmPerPassenger(), 0.01);
        assertEquals(7200, model.getGroundReserveLiters(), 0.01);
    }

    @Test
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
    void testAllModelsHaveValidProperties() {
        for (AirplaneModel model : AirplaneModel.values()) {
            assertNotNull(model.getDisplayName());
            assertTrue(model.getDisplayName().length() > 0);
            assertTrue(model.getMaxCapacity() > 0);
            assertTrue(model.getFuelLitersPerKmPerPassenger() > 0);
            assertTrue(model.getGroundReserveLiters() > 0);
        }
    }
}
