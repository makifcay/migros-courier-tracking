# Migros Online - Courier Tracking Service

A production-ready, highly concurrent RESTful courier geolocation tracking and store proximity logging microservice built with **Java 17** and **Spring Boot 3.3.2**.

---

## 📌 Features & Business Rules
1. **Real-time Geolocation Ingestion:** Ingests streaming coordinates (`lat`, `lng`, `time`, `courierId`) with strict bean validation (`@DecimalMin`, `@DecimalMax`, `@NotNull`).
2. **Geofencing & Store Entrance Detection:** Detects when any courier enters within a **100-meter radius** of 5 predefined Migros stores loaded from `stores.json`.
3. **Re-entry Cooldown (1-Minute Rule):** Prevents duplicate entrance logs if a courier stays inside or re-enters the 100m circumference within 1 minute. Configurable via `courier.tracking.reentry-cooldown-minutes`.
4. **Total Travel Distance Accumulation:** Accurately computes cumulative travel distance using spherical geodesic trigonometry between consecutive coordinates.
5. **Multi-Courier State Isolation:** Keeps independent distance trackers and entrance histories per courier concurrently.
6. **Robust Error Handling:** Centralized `@RestControllerAdvice` returning standardized RFC-compliant error payloads.
7. **Interactive API Documentation:** Integrated Swagger / OpenAPI 3.0 UI.

---

## 🏛️ Architecture & Design Patterns
* **Strategy Pattern (`DistanceCalculatorStrategy`):**
   * Decouples the distance calculation algorithm from the core domain. Currently implemented via **Haversine formula** (`HaversineDistanceCalculator`) for spherical accuracy. Enables seamless plug-and-play for road-network or third-party matrix APIs without modifying business logic (Open-Closed Principle).
* **Observer / Event-Driven Pattern (`ApplicationEvent`):**
   * Decouples the ingestion pipeline from side-effects. Upon location ingestion, a `CourierLocationEvent` is published. The `StoreProximityService` handles geofence evaluation independently, adhering to the Single Responsibility Principle.

---

## 🚀 How to Run

### Option A: Using Maven Wrapper (Local)
```bash
./mvnw clean spring-boot:run
```

### Option B: Using Docker
```bash
docker build -t courier-tracking:latest .
docker run -p 8080:8080 courier-tracking:latest
```

---

## 📚 API Endpoints & Swagger UI

Once running, access Swagger UI at:  
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

* `POST /api/v1/couriers/locations` - Ingest courier geolocation update
* `GET /api/v1/couriers/{courierId}/total-distance` - Query cumulative distance traveled

---

## 🧪 Automated Testing Suite

The project includes **9 comprehensive unit and integration tests** covering the test pyramid:

1. **Unit Tests (Mockito & AssertJ):**
   * Geodesic mathematical accuracy of the Haversine formula (zero-distance, known store distances).
   * 100-meter proximity threshold trigger.
   * 1-minute re-entry cooldown debounce logic.
2. **Integration Tests (MockMvc & H2):**
   * **End-to-End Flow:** Sequential location streaming, store entrance logging, and cumulative distance verification.
   * **Multi-Courier Isolation:** Verifies distinct couriers operating across different stores simultaneously do not cross-contaminate state.
   * **Concurrency & High-Throughput:** Multi-threaded stress test (30 concurrent workers via `ExecutorService` & `CountDownLatch`) ensuring thread safety, zero deadlocks, and consistency.
   * **Negative / Validation Test:** Validates that out-of-bound coordinates (`lat > 90`) or missing fields trigger `400 Bad Request`.

To execute all tests:
```bash
./mvnw clean test
```