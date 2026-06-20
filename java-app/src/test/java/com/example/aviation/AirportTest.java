package com.example.aviation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class AirportTest {

    @Test
    void testJFKAirport() {
        Airport jfk = Airport.JFK;
        assertEquals("John F. Kennedy International", jfk.getName());
        assertEquals("JFK", jfk.getCode());
        assertEquals(40.6413, jfk.getLatitude(), 0.0001);
        assertEquals(-73.7781, jfk.getLongitude(), 0.0001);
    }

    @Test
    void testLAXAirport() {
        Airport lax = Airport.LAX;
        assertEquals("Los Angeles International", lax.getName());
        assertEquals("LAX", lax.getCode());
        assertEquals(33.9425, lax.getLatitude(), 0.0001);
        assertEquals(-118.4081, lax.getLongitude(), 0.0001);
    }

    @Test
    void testORDAirport() {
        Airport ord = Airport.ORD;
        assertEquals("O'Hare International", ord.getName());
        assertEquals("ORD", ord.getCode());
        assertEquals(41.9742, ord.getLatitude(), 0.0001);
        assertEquals(-87.9073, ord.getLongitude(), 0.0001);
    }

    @Test
    void testLHRAirport() {
        Airport lhr = Airport.LHR;
        assertEquals("London Heathrow", lhr.getName());
        assertEquals("LHR", lhr.getCode());
        assertEquals(51.4700, lhr.getLatitude(), 0.0001);
        assertEquals(-0.4543, lhr.getLongitude(), 0.0001);
    }

    @Test
    void testCDGAirport() {
        Airport cdg = Airport.CDG;
        assertEquals("Paris Charles de Gaulle", cdg.getName());
        assertEquals("CDG", cdg.getCode());
        assertEquals(49.0097, cdg.getLatitude(), 0.0001);
        assertEquals(2.5479, cdg.getLongitude(), 0.0001);
    }

    @Test
    void testMADAirport() {
        Airport mad = Airport.MAD;
        assertEquals("Madrid-Barajas", mad.getName());
        assertEquals("MAD", mad.getCode());
        assertEquals(40.4983, mad.getLatitude(), 0.0001);
        assertEquals(-3.5676, mad.getLongitude(), 0.0001);
    }

    @Test
    void testDXBAirport() {
        Airport dxb = Airport.DXB;
        assertEquals("Dubai International", dxb.getName());
        assertEquals("DXB", dxb.getCode());
        assertEquals(25.2532, dxb.getLatitude(), 0.0001);
        assertEquals(55.3657, dxb.getLongitude(), 0.0001);
    }

    @Test
    void testNRTAirport() {
        Airport nrt = Airport.NRT;
        assertEquals("Narita International", nrt.getName());
        assertEquals("NRT", nrt.getCode());
        assertEquals(35.7720, nrt.getLatitude(), 0.0001);
        assertEquals(140.3929, nrt.getLongitude(), 0.0001);
    }

    @ParameterizedTest
    @EnumSource(Airport.class)
    void testAllAirportsHaveNonEmptyName(Airport airport) {
        assertNotNull(airport.getName());
        assertFalse(airport.getName().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Airport.class)
    void testAllAirportsHaveNonEmptyCode(Airport airport) {
        assertNotNull(airport.getCode());
        assertFalse(airport.getCode().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Airport.class)
    void testAllAirportsHaveValidCoordinates(Airport airport) {
        double latitude = airport.getLatitude();
        double longitude = airport.getLongitude();
        assertTrue(latitude >= -90 && latitude <= 90, "Latitude must be between -90 and 90");
        assertTrue(longitude >= -180 && longitude <= 180, "Longitude must be between -180 and 180");
    }
}
