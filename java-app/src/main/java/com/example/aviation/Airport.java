package com.example.aviation;

public enum Airport {
    JFK("John F. Kennedy International", "JFK", 40.6413, -73.7781),
    LAX("Los Angeles International", "LAX", 33.9425, -118.4081),
    ORD("O'Hare International", "ORD", 41.9742, -87.9073),
    LHR("London Heathrow", "LHR", 51.4700, -0.4543),
    CDG("Paris Charles de Gaulle", "CDG", 49.0097, 2.5479),
    MAD("Madrid-Barajas", "MAD", 40.4983, -3.5676),
    DXB("Dubai International", "DXB", 25.2532, 55.3657),
    NRT("Narita International", "NRT", 35.7720, 140.3929);

    private final String name;
    private final String code;
    private final double latitude;
    private final double longitude;

    Airport(String name, String code, double latitude, double longitude) {
        this.name = name;
        this.code = code;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
