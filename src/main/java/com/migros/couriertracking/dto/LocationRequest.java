package com.migros.couriertracking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {

    @NotNull(message = "Courier ID cannot be null")
    @JsonProperty("courierId")
    @JsonAlias({"courier", "courier_id"})
    @Schema(description = "Kurye ID'si", example = "1")

    private Long courierId;

    @NotNull(message = "Latitude cannot be null")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Schema(description = "Enlem", example = "40.9923307")
    private Double lat;

    @NotNull(message = "Longitude cannot be null")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Schema(description = "Boylam", example = "29.1244229")
    private Double lng;

    @NotNull(message = "Time cannot be null")
    @Schema(description = "Konum zamanı", example = "2026-08-16T10:15:30")
    private LocalDateTime time;
}