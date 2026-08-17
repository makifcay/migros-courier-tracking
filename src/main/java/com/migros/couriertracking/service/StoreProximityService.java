package com.migros.couriertracking.service;

import com.migros.couriertracking.dto.LocationRequest;
import com.migros.couriertracking.entity.Store;
import com.migros.couriertracking.entity.StoreEntranceLog;
import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.repository.StoreEntranceLogRepository;
import com.migros.couriertracking.repository.StoreRepository;
import com.migros.couriertracking.strategy.DistanceCalculatorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreProximityService {

    private final StoreRepository storeRepository;
    private final StoreEntranceLogRepository entranceLogRepository;
    private final DistanceCalculatorStrategy distanceCalculator;

    @Value("${courier.tracking.store-radius-in-meters:100.0}")
    private double storeRadiusInMeters;

    @Value("${courier.tracking.reentry-cooldown-minutes:1}")
    private long reentryCooldownMinutes;

    @EventListener
    @Transactional
    public void onCourierLocationReceived(CourierLocationEvent event) {
        LocationRequest location = event.getLocationRequest();
        checkAndLogStoreEntrances(location);
    }

    private void checkAndLogStoreEntrances(LocationRequest location) {
        List<Store> stores = storeRepository.findAll();

        for (Store store : stores) {
            double distance = distanceCalculator.calculateDistance(
                    location.getLat(), location.getLng(),
                    store.getLat(), store.getLng()
            );

            // 100 metre yarıçapında mı?
            if (distance <= storeRadiusInMeters) {
                processStoreEntrance(location, store, distance);
            }
        }
    }

    private void processStoreEntrance(LocationRequest location, Store store, double distance) {
        Optional<StoreEntranceLog> lastEntranceOpt = entranceLogRepository
                .findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(location.getCourierId(), store.getName());

        boolean isEligibleForNewEntrance = true;

        // processStoreEntrance metodu içinde:
        if (lastEntranceOpt.isPresent()) {
            LocalDateTime lastEntranceTime = lastEntranceOpt.get().getEntranceTime();
            long secondsPassed = Duration.between(lastEntranceTime, location.getTime()).toSeconds();

            // 60 saniyeden (1 dakikadan) az süre geçmişse tekrar giriş sayma
            if (secondsPassed >= 0 && secondsPassed < (reentryCooldownMinutes * 60)) {
                isEligibleForNewEntrance = false;
                log.debug("Courier {} is within {}m of store {}, but ignored due to cooldown.",
                        location.getCourierId(), distance, store.getName());
            }
        }

        if (isEligibleForNewEntrance) {
            StoreEntranceLog logEntry = StoreEntranceLog.builder()
                    .courierId(location.getCourierId())
                    .storeName(store.getName())
                    .entranceTime(location.getTime())
                    .distanceToStore(distance)
                    .build();

            entranceLogRepository.save(logEntry);

            // Güncellenen temiz log satırı:
            log.info(">>> ENTRANCE LOGGED: Courier {} entered {} (Distance: {}m, Time: {})",
                    location.getCourierId(), store.getName(), String.format("%.2f", distance), location.getTime());
        }
    }
}