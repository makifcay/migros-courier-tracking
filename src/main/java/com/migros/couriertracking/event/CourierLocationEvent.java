package com.migros.couriertracking.event;

import com.migros.couriertracking.dto.LocationRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CourierLocationEvent extends ApplicationEvent {

    private final LocationRequest locationRequest;

    public CourierLocationEvent(Object source, LocationRequest locationRequest) {
        super(source);
        this.locationRequest = locationRequest;
    }
}