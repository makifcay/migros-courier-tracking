package com.migros.couriertracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalDistanceResponse {
    private Long courierId;
    private Double totalDistanceInMeters;
    private Double totalDistanceInKm;
}