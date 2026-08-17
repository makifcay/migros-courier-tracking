package com.migros.couriertracking.service;

import com.migros.couriertracking.dto.LocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;
import com.migros.couriertracking.entity.Courier;
import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.repository.CourierRepository;
import com.migros.couriertracking.strategy.DistanceCalculatorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierService {

    private final CourierRepository courierRepository;
    private final DistanceCalculatorStrategy distanceCalculator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processLocation(LocationRequest request) {
        Courier courier = courierRepository.findById(request.getCourierId())
                .orElseGet(() -> Courier.builder()
                        .id(request.getCourierId())
                        .totalDistance(0.0)
                        .build());

        // Eğer kuryenin önceki bir konumu varsa aradaki mesafeyi hesapla ve ekle
        if (courier.getLastLat() != null && courier.getLastLng() != null) {
            double distanceTraveled = distanceCalculator.calculateDistance(
                    courier.getLastLat(), courier.getLastLng(),
                    request.getLat(), request.getLng()
            );
            courier.setTotalDistance(courier.getTotalDistance() + distanceTraveled);
            log.debug("Courier {} traveled {:.2f} meters.", courier.getId(), distanceTraveled);
        }

        // Son konumu ve zamanı güncelle
        courier.setLastLat(request.getLat());
        courier.setLastLng(request.getLng());
        courier.setLastLocationTime(request.getTime());

        courierRepository.save(courier);

        // Observer Pattern: Lokasyon alındı event'ini fırlat (Mağaza yakınlık servisi dinleyecek)
        eventPublisher.publishEvent(new CourierLocationEvent(this, request));
    }

    // Case metninde istenen zorunlu method imzası
    @Transactional(readOnly = true)
    public Double getTotalTravelDistance(Long courierId) {
        return courierRepository.findById(courierId)
                .map(Courier::getTotalDistance)
                .orElse(0.0);
    }

    @Transactional(readOnly = true)
    public TotalDistanceResponse getTotalDistanceResponse(Long courierId) {
        Double distanceInMeters = getTotalTravelDistance(courierId);
        return TotalDistanceResponse.builder()
                .courierId(courierId)
                .totalDistanceInMeters(distanceInMeters)
                .totalDistanceInKm(distanceInMeters / 1000.0)
                .build();
    }
}