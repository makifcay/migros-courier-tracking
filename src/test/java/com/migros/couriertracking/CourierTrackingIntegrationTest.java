package com.migros.couriertracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.dto.LocationRequest;
import com.migros.couriertracking.entity.StoreEntranceLog;
import com.migros.couriertracking.repository.CourierRepository;
import com.migros.couriertracking.repository.StoreEntranceLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CourierTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreEntranceLogRepository entranceLogRepository;

    @Autowired
    private CourierRepository courierRepository;

    @Test
    @DisplayName("1. Uçtan uca kurye lokasyon akışı, mağaza girişi ve mesafe sorgulama testi")
    void fullCourierFlow_ShouldLogEntranceAndCalculateDistance() throws Exception {
        Long courierId = 99L;
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 16, 12, 0, 0);

        // 1. Konum: Ataşehir MMM Migros tam koordinatı (Giriş loglanmalı)
        LocationRequest loc1 = LocationRequest.builder()
                .courierId(courierId)
                .lat(40.9923307)
                .lng(29.1244229)
                .time(startTime)
                .build();

        mockMvc.perform(post("/api/v1/couriers/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc1)))
                .andExpect(status().isCreated());

        assertTrue(entranceLogRepository.findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(courierId, "Ataşehir MMM Migros").isPresent());

        // 2. Konum: 10 dakika sonra Novada MMM Migros'a git (Mesafe artmalı)
        LocationRequest loc2 = LocationRequest.builder()
                .courierId(courierId)
                .lat(40.986106)
                .lng(29.1161293)
                .time(startTime.plusMinutes(10))
                .build();

        mockMvc.perform(post("/api/v1/couriers/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc2)))
                .andExpect(status().isCreated());

        // 3. Toplam mesafeyi sorgula
        mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.totalDistanceInMeters").value(greaterThan(900.0)));
    }

    @Test
    @DisplayName("2. Çoklu Kurye İzolasyonu: Farklı kuryelerin mesafeleri ve logları birbirine karışmamalıdır")
    void multipleCouriers_ShouldMaintainIndependentStates() throws Exception {
        Long courierA = 101L;
        Long courierB = 102L;
        LocalDateTime now = LocalDateTime.now();

        // Kurye A: Ataşehir'de
        LocationRequest locA = LocationRequest.builder()
                .courierId(courierA).lat(40.9923307).lng(29.1244229).time(now).build();

        // Kurye B: Beylikdüzü'nde
        LocationRequest locB = LocationRequest.builder()
                .courierId(courierB).lat(41.0066851).lng(28.6552262).time(now).build();

        mockMvc.perform(post("/api/v1/couriers/locations")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(locA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/couriers/locations")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(locB)))
                .andExpect(status().isCreated());

        // Kurye A'nın Ataşehir'e, Kurye B'nin Beylikdüzü'ne girdiğini doğrula
        assertTrue(entranceLogRepository.findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(courierA, "Ataşehir MMM Migros").isPresent());
        assertTrue(entranceLogRepository.findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(courierB, "Beylikdüzü 5M Migros").isPresent());

        // Kurye A'nın Beylikdüzü'nde logunun OLMADIĞINI doğrula
        assertTrue(entranceLogRepository.findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(courierA, "Beylikdüzü 5M Migros").isEmpty());
    }

    @Test
    @DisplayName("3. Eşzamanlılık & Yüksek Trafik Testi: 30 kuryeden aynı anda veri aktığında sistem hatasız işlemelidir")
    void concurrentStreaming_ShouldProcessAllLocationsWithoutDeadlock() throws Exception {
        int numberOfThreads = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 1; i <= numberOfThreads; i++) {
            final long courierId = 200L + i;
            executorService.submit(() -> {
                try {
                    LocationRequest request = LocationRequest.builder()
                            .courierId(courierId)
                            .lat(40.9923307)
                            .lng(29.1244229)
                            .time(baseTime)
                            .build();

                    mockMvc.perform(post("/api/v1/couriers/locations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Tüm thread'lerin 10 saniye içinde tamamlanmasını bekle
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertTrue(completed, "Eşzamanlı istekler zaman aşımına uğradı!");
        assertEquals(numberOfThreads, successCount.get(), "Tüm eşzamanlı lokasyon istekleri 201 Created ile tamamlanmalıdır");
    }

    @Test
    @DisplayName("4. Validasyon / Negatif Test: Geçersiz koordinatlar veya eksik veride 400 Bad Request dönmelidir")
    void invalidLocationRequest_ShouldReturnBadRequest() throws Exception {
        // Enlem > 90 ve kurye ID eksik hatalı model
        LocationRequest invalidRequest = LocationRequest.builder()
                .courierId(null) // Zorunlu alan yok
                .lat(120.0)      // Geçersiz enlem (> 90)
                .lng(29.0)
                .time(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/couriers/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }
}