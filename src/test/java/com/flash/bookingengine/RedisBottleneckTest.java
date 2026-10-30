package com.flash.bookingengine;

import com.flash.bookingengine.dto.ReservationRequestDto;
import com.flash.bookingengine.model.Item;
import com.flash.bookingengine.repository.ItemRepository;
import com.flash.bookingengine.repository.ReservationRepository;
import com.flash.bookingengine.service.RedisReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class RedisBottleneckTest {

    @Autowired
    private RedisReservationService redisReservationService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long testItemId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        itemRepository.deleteAll();

        // 50 available tickets
        Item item = Item.builder()
                .name("Limited Edition Sneaker")
                .totalStock(50)
                .availableStock(50)
                .build();
        testItemId = itemRepository.save(item).getId();

        // Seed inventory in Redis
        String stockKey = "item:" + testItemId + ":stock";
        redisTemplate.opsForValue().set(stockKey, "50");
    }

    @Test
    void runRedisStressTest() throws InterruptedException {
        int totalRequests = 200;
        int simulatedConcurrentUsers = 50;

        ExecutorService executor = Executors.newFixedThreadPool(simulatedConcurrentUsers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger soldOutCount = new AtomicInteger(0);
        AtomicInteger otherFailures = new AtomicInteger(0);

        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());

        long benchmarkStartTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final String userId = "redis_user_" + i;
            executor.submit(() -> {
                long reqStart = 0;
                try {
                    startGate.await();
                    reqStart = System.currentTimeMillis();

                    ReservationRequestDto request = new ReservationRequestDto();
                    request.setItemId(testItemId);
                    request.setUserId(userId);

                    redisReservationService.reserveWithRedis(request);
                    successCount.incrementAndGet();

                } catch (IllegalStateException e) {
                    soldOutCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("THREAD ERROR: " + e.getClass().getName() + " - " + e.getMessage());
                    otherFailures.incrementAndGet();
                } finally {
                    if (reqStart > 0) {
                        latenciesMs.add(System.currentTimeMillis() - reqStart);
                    }
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await(60, TimeUnit.SECONDS);
        long totalExecutionTimeMs = System.currentTimeMillis() - benchmarkStartTime;
        executor.shutdown();

        Collections.sort(latenciesMs);
        long p50 = latenciesMs.isEmpty() ? 0 : latenciesMs.get((int) (latenciesMs.size() * 0.50));
        long p95 = latenciesMs.isEmpty() ? 0 : latenciesMs.get((int) (latenciesMs.size() * 0.95));
        long p99 = latenciesMs.isEmpty() ? 0 : latenciesMs.get((int) (latenciesMs.size() * 0.99));
        double throughputRps = (double) totalRequests / (totalExecutionTimeMs / 1000.0);

        System.out.println("\n============= REDIS BENCHMARK RESULTS =============");
        System.out.println("Total Requests Simulated : " + totalRequests);
        System.out.println("Concurrency Level        : " + simulatedConcurrentUsers + " parallel threads");
        System.out.println("Total Benchmark Time     : " + totalExecutionTimeMs + " ms");
        System.out.printf( "Calculated Throughput    : %.2f req/sec\n", throughputRps);
        System.out.println("---------------------------------------------------");
        System.out.println("Successful Allocations   : " + successCount.get());
        System.out.println("Rejected at Gate (RAM)   : " + soldOutCount.get());
        System.out.println("Other Errors             : " + otherFailures.get());
        System.out.println("---------------------------------------------------");
        System.out.println("Latency P50 (Median)     : " + p50 + " ms");
        System.out.println("Latency P95              : " + p95 + " ms");
        System.out.println("Latency P99              : " + p99 + " ms");
        System.out.println("===================================================\n");
    }
}

/*
============= REDIS BENCHMARK RESULTS =============
Total Requests Simulated : 200
Concurrency Level        : 50 parallel threads
Total Benchmark Time     : 9064 ms
Calculated Throughput    : 22.07 req/sec
---------------------------------------------------
Successful Allocations   : 50
Rejected at Gate (RAM)   : 150
Other Errors             : 0
---------------------------------------------------
Latency P50 (Median)     : 77 ms
Latency P95              : 8090 ms
Latency P99              : 9049 ms
===================================================
 */