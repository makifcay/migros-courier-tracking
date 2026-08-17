package com.migros.couriertracking.service;

import com.migros.couriertracking.dto.LocationRequest;
import com.migros.couriertracking.entity.Store;
import com.migros.couriertracking.entity.StoreEntranceLog;
import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.repository.StoreEntranceLogRepository;
import com.migros.couriertracking.repository.StoreRepository;
import com.migros.couriertracking.strategy.DistanceCalculatorStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreProximityServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreEntranceLogRepository entranceLogRepository;

    @Mock
    private DistanceCalculatorStrategy distanceCalculator;

    @InjectMocks
    private StoreProximityService proximityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(proximityService, "storeRadiusInMeters", 100.0);
        ReflectionTestUtils.setField(proximityService, "reentryCooldownMinutes", 1L);
    }

    @Test
    @DisplayName("Kurye 100m içindeyse ve ilk girişi ise log kaydedilmelidir")
    void onLocationReceived_WithinRadiusFirstTime_ShouldSaveLog() {
        Store store = Store.builder().id(1L).name("Ataşehir MMM Migros").lat(40.9923).lng(29.1244).build();
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(50.0); // 50m < 100m
        when(entranceLogRepository.findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(1L, "Ataşehir MMM Migros"))
                .thenReturn(Optional.empty());

        LocationRequest request = LocationRequest.builder()
                .courierId(1L)
                .lat(40.9923)
                .lng(29.1244)
                .time(LocalDateTime.now())
                .build();

        proximityService.onCourierLocationReceived(new CourierLocationEvent(this, request));

        verify(entranceLogRepository, times(1)).save(any(StoreEntranceLog.class));
    }

    @Test
    @DisplayName("Kurye 1 dakika dolmadan tekrar girerse yeni log ATILMAMALIDIR")
    void onLocationReceived_WithinOneMinuteReentry_ShouldNotSaveLog() {
        LocalDateTime now = LocalDateTime.now();
        Store store = Store.builder().id(1L).name("Ataşehir MMM Migros").lat(40.9923).lng(29.1244).build();

        StoreEntranceLog previousLog = StoreEntranceLog.builder()
                .courierId(1L)
                .storeName("Ataşehir MMM Migros")
                .entranceTime(now.minusSeconds(30)) // 30 saniye önce girmişti
                .build();

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(30.0);
        when(entranceLogRepository.findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(1L, "Ataşehir MMM Migros"))
                .thenReturn(Optional.of(previousLog));

        LocationRequest request = LocationRequest.builder()
                .courierId(1L)
                .lat(40.9923)
                .lng(29.1244)
                .time(now)
                .build();

        proximityService.onCourierLocationReceived(new CourierLocationEvent(this, request));

        verify(entranceLogRepository, never()).save(any(StoreEntranceLog.class));
    }
}

