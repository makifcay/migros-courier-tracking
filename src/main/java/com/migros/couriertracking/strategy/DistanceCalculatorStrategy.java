package com.migros.couriertracking.strategy;

public interface DistanceCalculatorStrategy {
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);
}