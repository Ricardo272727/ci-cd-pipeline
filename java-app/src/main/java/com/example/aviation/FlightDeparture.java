package com.example.aviation;

public record FlightDeparture(
        AirplaneModel airplaneModel,
        int capacity,
        Airport origin,
        Airport destination
) {
    public FlightDeparture {
        if (airplaneModel == null) {
            throw new IllegalArgumentException("airplaneModel is required");
        }
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("origin and destination are required");
        }
        if (origin == destination) {
            throw new IllegalArgumentException("origin and destination must be different");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        if (capacity > airplaneModel.getMaxCapacity()) {
            throw new IllegalArgumentException(
                    "capacity " + capacity + " exceeds maximum for " + airplaneModel.getDisplayName()
                            + " (" + airplaneModel.getMaxCapacity() + ")"
            );
        }
    }
}
