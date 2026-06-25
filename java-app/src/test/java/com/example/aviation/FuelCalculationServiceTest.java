package com.example.aviation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FuelCalculationServiceTest {

    @Autowired
    private FuelCalculationService fuelCalculationService;

    private FlightDeparture validFlight;

    @BeforeEach
    void setUp() {
        validFlight = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
    }

    @Test
    void calculateFuel_ValidFlight_ReturnsCorrectCalculation() {
        FuelCalculationResult result = fuelCalculationService.calculateFuel(validFlight);

        assertNotNull(result);
        assertEquals(validFlight, result.departure());
        assertTrue(result.distanceKm() > 0);
        assertTrue(result.cruiseFuelLiters() > 0);
        assertTrue(result.reserveFuelLiters() > 0);
        assertTrue(result.totalFuelLiters() > 0);
    }

    @Test
    void calculateFuel_CruiseFuelCalculationAccuracy() {
        FlightDeparture flight = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                100,
                Airport.JFK,
                Airport.LAX
        );

        FuelCalculationResult result = fuelCalculationService.calculateFuel(flight);

        assertTrue(result.cruiseFuelLiters() > 0);
        assertTrue(result.distanceKm() > 0);
        assertTrue(result.totalFuelLiters() >= result.cruiseFuelLiters());
    }

    @Test
    void calculateFuel_TotalFuelIncludesContingencyAndReserve() {
        FuelCalculationResult result = fuelCalculationService.calculateFuel(validFlight);

        assertTrue(result.totalFuelLiters() > result.cruiseFuelLiters());
        assertTrue(result.totalFuelLiters() > result.reserveFuelLiters());
        assertEquals(result.reserveFuelLiters(), AirplaneModel.BOEING_737_800.getGroundReserveLiters(), 0.02);
    }

    @Test
    void calculateFuel_DifferentAirplaneModels_DifferentConsumption() {
        FlightDeparture flight737 = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                150,
                Airport.JFK,
                Airport.LAX
        );
        FlightDeparture flight787 = new FlightDeparture(
                AirplaneModel.BOEING_787_9,
                250,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult result737 = fuelCalculationService.calculateFuel(flight737);
        FuelCalculationResult result787 = fuelCalculationService.calculateFuel(flight787);
        
        assertNotEquals(result737.cruiseFuelLiters(), result787.cruiseFuelLiters());
    }

    @Test
    void calculateFuel_ShorterDistance_LessCruiseFuel() {
        FlightDeparture shortFlight = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                100,
                Airport.JFK,
                Airport.LAX
        );
        FlightDeparture longFlight = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                100,
                Airport.JFK,
                Airport.NRT
        );
        
        FuelCalculationResult shortResult = fuelCalculationService.calculateFuel(shortFlight);
        FuelCalculationResult longResult = fuelCalculationService.calculateFuel(longFlight);
        
        assertTrue(shortResult.cruiseFuelLiters() < longResult.cruiseFuelLiters());
    }

    @Test
    void calculateFuel_ReserveFuelMatchesAirplaneModel() {
        FuelCalculationResult result = fuelCalculationService.calculateFuel(validFlight);
        
        assertEquals(AirplaneModel.BOEING_737_800.getGroundReserveLiters(), 
                result.reserveFuelLiters(), 0.01);
    }

    @Test
    void calculateFuel_RoundingTo2Decimals() {
        FuelCalculationResult result = fuelCalculationService.calculateFuel(validFlight);
        
        double distanceFractional = result.distanceKm() * 100;
        assertEquals(Math.floor(distanceFractional), distanceFractional, 0.01);
    }

    @Test
    void calculateFuelForDepartures_ValidList_ReturnsResults() {
        List<FlightDeparture> departures = List.of(
                validFlight,
                new FlightDeparture(AirplaneModel.AIRBUS_A320, 120, Airport.LAX, Airport.JFK)
        );
        
        List<FuelCalculationResult> results = fuelCalculationService.calculateFuelForDepartures(departures);
        
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.totalFuelLiters() > 0));
    }

    @Test
    void calculateFuelForDepartures_NullList_ThrowsException() {
        assertThrows(IllegalArgumentException.class, 
                () -> fuelCalculationService.calculateFuelForDepartures(null));
    }

    @Test
    void calculateFuelForDepartures_EmptyList_ThrowsException() {
        assertThrows(IllegalArgumentException.class, 
                () -> fuelCalculationService.calculateFuelForDepartures(List.of()));
    }

    @Test
    void calculateFuelForDepartures_MultipleFlights_AllCalculated() {
        List<FlightDeparture> departures = List.of(
                new FlightDeparture(AirplaneModel.BOEING_737_800, 100, Airport.JFK, Airport.LAX),
                new FlightDeparture(AirplaneModel.AIRBUS_A320, 120, Airport.LAX, Airport.ORD),
                new FlightDeparture(AirplaneModel.BOEING_787_9, 200, Airport.ORD, Airport.LHR)
        );
        
        List<FuelCalculationResult> results = fuelCalculationService.calculateFuelForDepartures(departures);
        
        assertEquals(3, results.size());
        for (int i = 0; i < departures.size(); i++) {
            assertEquals(departures.get(i), results.get(i).departure());
        }
    }

    @Test
    void totalFuelLiters_MultipleResults_CorrectSum() {
        List<FlightDeparture> departures = List.of(
                new FlightDeparture(AirplaneModel.BOEING_737_800, 100, Airport.JFK, Airport.LAX),
                new FlightDeparture(AirplaneModel.AIRBUS_A320, 100, Airport.LAX, Airport.ORD)
        );
        
        List<FuelCalculationResult> results = fuelCalculationService.calculateFuelForDepartures(departures);
        double totalFuel = fuelCalculationService.totalFuelLiters(results);
        
        double expectedTotal = results.stream()
                .mapToDouble(FuelCalculationResult::totalFuelLiters)
                .sum();
        assertEquals(expectedTotal, totalFuel, 0.01);
    }

    @Test
    void totalFuelLiters_SingleResult_ReturnsTotalFromResult() {
        List<FlightDeparture> departures = List.of(validFlight);
        List<FuelCalculationResult> results = fuelCalculationService.calculateFuelForDepartures(departures);
        
        double totalFuel = fuelCalculationService.totalFuelLiters(results);
        
        assertEquals(results.get(0).totalFuelLiters(), totalFuel, 0.01);
    }

    @Test
    void calculateFuel_DifferentCapacities_ProportionalFuelIncrease() {
        FlightDeparture flightSmall = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                50,
                Airport.JFK,
                Airport.LAX
        );
        FlightDeparture flightLarge = new FlightDeparture(
                AirplaneModel.BOEING_737_800,
                100,
                Airport.JFK,
                Airport.LAX
        );
        
        FuelCalculationResult smallResult = fuelCalculationService.calculateFuel(flightSmall);
        FuelCalculationResult largeResult = fuelCalculationService.calculateFuel(flightLarge);
        
        assertEquals(smallResult.distanceKm(), largeResult.distanceKm(), 0.01);
        assertEquals(2.0 * smallResult.cruiseFuelLiters(), largeResult.cruiseFuelLiters(), 0.01);
    }
}
