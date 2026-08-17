package com.migros.couriertracking.controller;

import com.migros.couriertracking.dto.LocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;
import com.migros.couriertracking.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
@Tag(name = "Courier Tracking API", description = "Kurye konum takibi, mağaza giriş loglama ve mesafe sorgulama API'si")
public class CourierController {

    private final CourierService courierService;

    @PostMapping("/locations")
    @Operation(summary = "Kurye anlık konumunu kaydeder ve mağaza girişlerini denetler")
    public ResponseEntity<Void> receiveLocation(@Valid @RequestBody LocationRequest request) {
        courierService.processLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{courierId}/total-distance")
    @Operation(summary = "Kuryenin katettiği toplam mesafeyi döner")
    public ResponseEntity<TotalDistanceResponse> getTotalDistance(@PathVariable Long courierId) {
        TotalDistanceResponse response = courierService.getTotalDistanceResponse(courierId);
        return ResponseEntity.ok(response);
    }
}