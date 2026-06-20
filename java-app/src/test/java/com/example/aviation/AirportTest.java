package com.example.aviation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirportTest {

    @Test
    void testJFKAirport() {
        Airport airport = Airport.JFK;
        assertEquals("John F. Kennedy International", airport.getName());
        assertEquals("JFK", airport.getCode());
        assertEquals(40.6413, airport.getLatitude());
        assertEquals(-73.7781, airport.getLongitude());
    }

    @Test
    void testLAXAirport() {
        Airport airport = Airport.LAX;
        assertEquals("Los Angeles International", airport.getName());
        assertEquals("LAX", airport.getCode());
        assertEquals(33.9425, airport.getLatitude());
        assertEquals(-118.4081, airport.getLongitude());
    }

    @Test
    void testORDAirport() {
        Airport airport = Airport.ORD;
        assertEquals("O'Hare International", airport.getName());
        assertEquals("ORD", airport.getCode());
        assertEquals(41.9742, airport.getLatitude());
        assertEquals(-87.9073, airport.getLongitude());
    }

    @Test
    void testLHRAirport() {
        Airport airport = Airport.LHR;
        assertEquals("London Heathrow", airport.getName());
        assertEquals("LHR", airport.getCode());
        assertEquals(51.4700, airport.getLatitude());
        assertEquals(-0.4543, airport.getLongitude());
    }

    @Test
    void testCDGAirport() {
        Airport airport = Airport.CDG;
        assertEquals("Paris Charles de Gaulle", airport.getName());
        assertEquals("CDG", airport.getCode());
        assertEquals(49.0097, airport.getLatitude());
        assertEquals(2.5479, airport.getLongitude());
    }

    @Test
    void testMADAirport() {
        Airport airport = Airport.MAD;
        assertEquals("Madrid-Barajas", airport.getName());
        assertEquals("MAD", airport.getCode());
        assertEquals(40.4983, airport.getLatitude());
        assertEquals(-3.5676, airport.getLongitude());
    }

    @Test
    void testDXBAirport() {
        Airport airport = Airport.DXB;
        assertEquals("Dubai International", airport.getName());
        assertEquals("DXB", airport.getCode());
        assertEquals(25.2532, airport.getLatitude());
        assertEquals(55.3657, airport.getLongitude());
    }

    @Test
    void testNRTAirport() {
        Airport airport = Airport.NRT;
        assertEquals("Narita International", airport.getName());
        assertEquals("NRT", airport.getCode());
        assertEquals(35.7720, airport.getLatitude());
        assertEquals(140.3929, airport.getLongitude());
    }

    @Test
    void testAirportValues() {
        Airport[] airports = Airport.values();
        assertEquals(8, airports.length);
        assertArrayEquals(
                new Airport[]{Airport.JFK, Airport.LAX, Airport.ORD, Airport.LHR,
                            Airport.CDG, Airport.MAD, Airport.DXB, Airport.NRT},
                airports
        );
    }

    @Test
    void testAirportValueOf() {
        assertEquals(Airport.JFK, Airport.valueOf("JFK"));
        assertEquals(Airport.LAX, Airport.valueOf("LAX"));
        assertEquals(Airport.ORD, Airport.valueOf("ORD"));
        assertEquals(Airport.LHR, Airport.valueOf("LHR"));
        assertEquals(Airport.CDG, Airport.valueOf("CDG"));
        assertEquals(Airport.MAD, Airport.valueOf("MAD"));
        assertEquals(Airport.DXB, Airport.valueOf("DXB"));
        assertEquals(Airport.NRT, Airport.valueOf("NRT"));
    }

    @Test
    void testAirportOrdinals() {
        assertEquals(0, Airport.JFK.ordinal());
        assertEquals(1, Airport.LAX.ordinal());
        assertEquals(2, Airport.ORD.ordinal());
        assertEquals(3, Airport.LHR.ordinal());
        assertEquals(4, Airport.CDG.ordinal());
        assertEquals(5, Airport.MAD.ordinal());
        assertEquals(6, Airport.DXB.ordinal());
        assertEquals(7, Airport.NRT.ordinal());
    }
}
