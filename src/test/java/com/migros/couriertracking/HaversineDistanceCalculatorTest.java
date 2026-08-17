package com.migros.couriertracking.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineDistanceCalculatorTest {

    private final DistanceCalculatorStrategy calculator = new HaversineDistanceCalculator();

    @Test
    @DisplayName("Aynı iki koordinat arasındaki mesafe 0 metre olmalıdır")
    void calculateDistance_SameLocation_ShouldReturnZero() {
        double distance = calculator.calculateDistance(40.9923307, 29.1244229, 40.9923307, 29.1244229);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("Ataşehir Migros ile Novada Migros arasındaki bilinen mesafe doğru hesaplanmalıdır (~970m)")
    void calculateDistance_BetweenTwoStores_ShouldReturnAccurateDistance() {
        // Ataşehir MMM Migros: 40.9923307, 29.1244229
        // Novada MMM Migros: 40.986106, 29.1161293
        double distance = calculator.calculateDistance(40.9923307, 29.1244229, 40.986106, 29.1161293);

        // Yaklaşık 970 metre civarında olmalı
        assertTrue(distance > 900 && distance < 1100, "Distance should be around 970m, but was: " + distance);
    }
}