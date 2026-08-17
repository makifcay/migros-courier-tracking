package com.migros.couriertracking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "couriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {

    @Id
    private Long id; // Kurye ID'si dışarıdan gelecek

    @Builder.Default
    private Double totalDistance = 0.0; // Metre cinsinden

    private Double lastLat;
    private Double lastLng;
    private LocalDateTime lastLocationTime;
}