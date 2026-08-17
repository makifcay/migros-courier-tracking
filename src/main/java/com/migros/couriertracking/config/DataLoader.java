package com.migros.couriertracking.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.entity.Store;
import com.migros.couriertracking.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final StoreRepository storeRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (storeRepository.count() == 0) {
            log.info("Loading initial store locations from stores.json...");
            Resource resource = resourceLoader.getResource("classpath:stores.json");

            try (InputStream inputStream = resource.getInputStream()) {
                List<Store> stores = objectMapper.readValue(inputStream, new TypeReference<List<Store>>() {});
                storeRepository.saveAll(stores);
                log.info("Successfully loaded {} stores into the database.", stores.size());
            } catch (Exception e) {
                log.error("Failed to load stores from stores.json: {}", e.getMessage(), e);
            }
        }
    }
}