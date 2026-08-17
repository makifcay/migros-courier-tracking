package com.migros.couriertracking.repository;

import com.migros.couriertracking.entity.StoreEntranceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreEntranceLogRepository extends JpaRepository<StoreEntranceLog, Long> {

    // Kuryenin ilgili mağazaya yaptığı EN SON girişi bulur (1 dakika kuralını kontrol etmek için)
    Optional<StoreEntranceLog> findTopByCourierIdAndStoreNameOrderByEntranceTimeDesc(Long courierId, String storeName);
}