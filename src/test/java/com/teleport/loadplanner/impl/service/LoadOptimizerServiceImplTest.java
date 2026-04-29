package com.teleport.loadplanner.impl.service;

import com.teleport.loadplanner.businessobjects.OrderDTO;
import com.teleport.loadplanner.businessobjects.TruckDTO;
import com.teleport.loadplanner.businessobjects.loadoptimizer.RequestDTO;
import com.teleport.loadplanner.businessobjects.loadoptimizer.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadOptimizerServiceImplTest {

    private LoadOptimizerServiceImpl loadOptimizerService;

    @BeforeEach
    void setUp() {
        loadOptimizerService = new LoadOptimizerServiceImpl();
    }

    @Test
    void testOptimizeLoad_WithMultipleCompatibleOrders_ShouldSelectOptimalCombination() throws Exception {
        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 3000);
        
        OrderDTO order1 = createOrder("ord-001", 250000, 18000, 1200,
            "Los Angeles, CA", "Dallas, TX", "2025-12-05", "2025-12-09", false);
        OrderDTO order2 = createOrder("ord-002", 180000, 12000, 900,
            "Los Angeles, CA", "Dallas, TX", "2025-12-04", "2025-12-10", false);
        OrderDTO order3 = createOrder("ord-003", 320000, 30000, 1800,
            "Los Angeles, CA", "Dallas, TX", "2025-12-06", "2025-12-08", true);
        
        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(Arrays.asList(order1, order2, order3));

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        assertEquals(430000L, response.getTotalPayoutCents());
        assertEquals(30000L, response.getTotalWeightLbs());
        assertEquals(2100L, response.getTotalVolumeCuft());
        assertEquals(68.18, response.getUtilizationWeightPercent(), 0.01);
        assertEquals(70.0, response.getUtilizationVolumePercent(), 0.01);
        
        // Should select ord-001 and ord-002 (non-hazmat, compatible, max payout)
        assertEquals(2, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-001"));
        assertTrue(response.getSelectedOrderIds().contains("ord-002"));
        assertFalse(response.getSelectedOrderIds().contains("ord-003"));
    }

    @Test
    void testOptimizeLoad_With25Orders_ShouldSelectOptimalCombination() throws Exception {

        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 3000);

        List<OrderDTO> orders = new ArrayList<>();

        // ---- Optimal combination (ONLY valid best set) ----
        orders.add(createOrder("ord-001", 200000, 10000, 700,
                "LA", "TX", "2025-12-01", "2025-12-05", false));

        orders.add(createOrder("ord-002", 180000, 9000, 600,
                "LA", "TX", "2025-12-02", "2025-12-06", false));

        orders.add(createOrder("ord-003", 150000, 8000, 500,
                "LA", "TX", "2025-12-03", "2025-12-07", false));

        orders.add(createOrder("ord-004", 120000, 7000, 400,
                "LA", "TX", "2025-12-02", "2025-12-08", false));

        // TOTAL:
        // payout = 650000
        // weight = 34000
        // volume = 2200

        // Remaining capacity:
        // weight left = 10000
        // volume left = 800

        // ---- Distractors: cannot fit due to capacity ----
        orders.add(createOrder("ord-005", 3000, 25000, 1000,
                "LA", "TX", "2025-12-01", "2025-12-05", false)); // overweight

        orders.add(createOrder("ord-006", 2800, 28000, 900,
                "LA", "TX", "2025-12-01", "2025-12-05", false)); // overweight

        // ---- Hazmat orders (should not mix) ----
        orders.add(createOrder("ord-007", 500000, 20000, 1000,
                "LA", "TX", "2025-12-01", "2025-12-05", true));

        orders.add(createOrder("ord-008", 100000, 5000, 300,
                "LA", "TX", "2025-12-01", "2025-12-05", true));

        // ---- Different route (invalid) ----
        orders.add(createOrder("ord-009", 300000, 10000, 700,
                "NY", "TX", "2025-12-01", "2025-12-05", false));

        orders.add(createOrder("ord-010", 300000, 10000, 700,
                "LA", "FL", "2025-12-01", "2025-12-05", false));

        // ---- Remaining orders (VERY IMPORTANT: cannot fit in leftover capacity) ----
        // Each one exceeds remaining capacity (either weight or volume)
        for (int i = 11; i <= 25; i++) {
            orders.add(createOrder(
                    "ord-" + String.format("%03d", i),
                    50000,     // decent payout but irrelevant
                    11000,     // > remaining weight (10000)
                    900,       // > remaining volume (800)
                    "LA", "TX",
                    "2025-12-01",
                    "2025-12-10",
                    false
            ));
        }

        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(orders);

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());

        // ✅ Exact deterministic optimal result
        assertEquals(650000L, response.getTotalPayoutCents());
        assertEquals(34000L, response.getTotalWeightLbs());
        assertEquals(2200L, response.getTotalVolumeCuft());

        // Utilization
        assertEquals(77.27, response.getUtilizationWeightPercent(), 0.01);
        assertEquals(73.33, response.getUtilizationVolumePercent(), 0.01);

        // Selected orders
        assertEquals(4, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-001"));
        assertTrue(response.getSelectedOrderIds().contains("ord-002"));
        assertTrue(response.getSelectedOrderIds().contains("ord-003"));
        assertTrue(response.getSelectedOrderIds().contains("ord-004"));

        // Ensure exclusions
        assertFalse(response.getSelectedOrderIds().contains("ord-005")); // overweight
        assertFalse(response.getSelectedOrderIds().contains("ord-007")); // hazmat
        assertFalse(response.getSelectedOrderIds().contains("ord-009")); // wrong route
    }

    @Test
    void testOptimizeLoad_With25Orders_ShouldSelectAllCombination() throws Exception {

        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 3000);

        List<OrderDTO> orders = new ArrayList<>();


        for (int i = 0; i <= 22; i++) {
            orders.add(createOrder(
                    "ord-" + String.format("%03d", i),
                    50000,     // decent payout but irrelevant
                    1100,     // > remaining weight (10000)
                    90,       // > remaining volume (800)
                    "LA", "TX",
                    "2025-12-01",
                    "2025-12-10",
                    false
            ));
        }

        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(orders);

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());

        assertEquals(23, response.getSelectedOrderIds().size());

    }

    @Test
    void testOptimizeLoad_WithHazmatOrderOnly_ShouldSelectHazmatOrder() throws Exception {
        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 3000);
        
        OrderDTO hazmatOrder = createOrder("ord-003", 320000, 30000, 1800,
            "Los Angeles, CA", "Dallas, TX", "2025-12-06", "2025-12-08", true);
        
        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(Arrays.asList(hazmatOrder));

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        assertEquals(320000L, response.getTotalPayoutCents());
        assertEquals(30000L, response.getTotalWeightLbs());
        assertEquals(1800L, response.getTotalVolumeCuft());
        assertEquals(68.18, response.getUtilizationWeightPercent(), 0.01);
        assertEquals(60.0, response.getUtilizationVolumePercent(), 0.01);
        
        assertEquals(1, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-003"));
    }

    @Test
    void testOptimizeLoad_WithExceedingWeightCapacity_ShouldSelectFittingOrders() throws Exception {
        // Given
        TruckDTO truck = createTruck("truck-123", 25000, 3000); // Lower weight capacity
        
        OrderDTO order1 = createOrder("ord-001", 250000, 18000, 1200,
            "Los Angeles, CA", "Dallas, TX", "2025-12-05", "2025-12-09", false);
        OrderDTO order2 = createOrder("ord-002", 180000, 12000, 900,
            "Los Angeles, CA", "Dallas, TX", "2025-12-04", "2025-12-10", false);
        
        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(Arrays.asList(order1, order2));

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        
        // Should select only one order due to weight constraint
        assertEquals(1, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-001")); // Higher payout
        assertEquals(250000, response.getTotalPayoutCents());
        assertEquals(18000, response.getTotalWeightLbs());
    }

    @Test
    void testOptimizeLoad_WithExceedingVolumeCapacity_ShouldSelectFittingOrders() throws Exception {
        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 1500); // Lower volume capacity
        
        OrderDTO order1 = createOrder("ord-001", 250000, 18000, 1200,
            "Los Angeles, CA", "Dallas, TX", "2025-12-05", "2025-12-09", false);
        OrderDTO order2 = createOrder("ord-002", 180000, 12000, 900,
            "Los Angeles, CA", "Dallas, TX", "2025-12-04", "2025-12-10", false);
        
        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(Arrays.asList(order1, order2));

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        
        // Should select only one order due to volume constraint
        assertEquals(1, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-001")); // Higher payout
        assertEquals(250000L, response.getTotalPayoutCents());
        assertEquals(1200L, response.getTotalVolumeCuft());
    }



    @Test
    void testOptimizeLoad_WithDifferentRoutes_ShouldRejectIncompatibleOrders() throws Exception {
        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 3000);
        
        OrderDTO order1 = createOrder("ord-001", 250000, 18000, 1200,
            "Los Angeles, CA", "Dallas, TX", "2025-12-05", "2025-12-09", false);
        OrderDTO order2 = createOrder("ord-002", 180000, 12000, 900,
            "New York, NY", "Boston, MA", "2025-12-04", "2025-12-10", false); // Different route
        
        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(Arrays.asList(order1, order2));

        // When
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);

        // Then
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        
        // Should select only the first order (algorithm picks the first compatible route)
        assertEquals(1, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-001"));
    }

    @Test
    void testOptimizeLoad_WithTooManyOrders_ShouldThrowMaxUploadSizeExceededException() {
        // Given
        TruckDTO truck = createTruck("truck-123", 44000, 3000);
        
        // Create 61 orders (exceeds the 60 limit)
        List<OrderDTO> orders = new ArrayList<>();
        for (int i = 0; i < 61; i++) {
            orders.add(createOrder("ord-" + String.format("%03d", i), 100000, 5000, 300,
                "Los Angeles, CA", "Dallas, TX", "2025-12-05", "2025-12-09", false));
        }
        
        RequestDTO request = new RequestDTO();
        request.setTruck(truck);
        request.setOrders(orders);

        // When & Then
        assertThrows(MaxUploadSizeExceededException.class, () -> {
            loadOptimizerService.optimizeLoad(request);
        });
    }

    @Test
    void testOptimizeLoad_WithNullTruck_ShouldThrowIllegalArgumentException() {
        // Given
        RequestDTO request = new RequestDTO();
        request.setTruck(null);
        request.setOrders(Arrays.asList(createOrder("ord-001", 100000, 5000, 300,
            "Los Angeles, CA", "Dallas, TX", "2025-12-05", "2025-12-09", false)));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            loadOptimizerService.optimizeLoad(request);
        });
    }

    @Test
    void testOptimizeLoad_WithNullOrders_ShouldThrowIllegalArgumentException() {
        // Given
        RequestDTO request = new RequestDTO();
        request.setTruck(createTruck("truck-123", 44000, 3000));
        request.setOrders(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            loadOptimizerService.optimizeLoad(request);
        });
    }

    private TruckDTO createTruck(String id, Integer maxWeightLbs, Integer maxVolumeCuft) {
        TruckDTO truck = new TruckDTO();
        truck.setId(id);
        truck.setMaxWeightLbs(maxWeightLbs);
        truck.setMaxVolumeCuft(maxVolumeCuft);
        return truck;
    }

    private OrderDTO createOrder(String id, Integer payoutCents, Integer weightLbs, Integer volumeCuft,
                                String origin, String destination, String pickupDate, String deliveryDate, Boolean isHazmat) {
        OrderDTO order = new OrderDTO();
        order.setId(id);
        order.setPayoutCents(payoutCents);
        order.setWeightLbs(weightLbs);
        order.setVolumeCuft(volumeCuft);
        order.setOrigin(origin);
        order.setDestination(destination);
        order.setPickupDate(pickupDate);
        order.setDeliveryDate(deliveryDate);
        order.setIsHazmat(isHazmat);
        return order;
    }
}
