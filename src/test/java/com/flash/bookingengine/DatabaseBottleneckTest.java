package com.flash.bookingengine;

import com.flash.bookingengine.dto.ReservationRequestDto;
import com.flash.bookingengine.model.Item;
import com.flash.bookingengine.repository.ItemRepository;
import com.flash.bookingengine.repository.ReservationRepository;
import com.flash.bookingengine.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class DatabaseBottleneckTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long testItemId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        itemRepository.deleteAll();

        // 50 tickets available
        Item item = Item.builder()
                .name("Limited Edition Sneaker")
                .totalStock(50)
                .availableStock(50)
                .build();
        testItemId = itemRepository.save(item).getId();
    }

    @Test
    void runStressTest() throws InterruptedException {
        int totalRequests = 200;       // 200 concurrent buyers
        int simulatedConcurrentUsers = 50;

        ExecutorService executor = Executors.newFixedThreadPool(simulatedConcurrentUsers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockConflictCount = new AtomicInteger(0);
        AtomicInteger poolTimeoutCount = new AtomicInteger(0);
        AtomicInteger otherFailures = new AtomicInteger(0);

        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());

        long benchmarkStartTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final String userId = "user_" + i;
            executor.submit(() -> {
                long reqStart = 0;
                try {
                    startGate.await(); // Hold all threads until the starting gun
                    reqStart = System.currentTimeMillis();

                    ReservationRequestDto request = new ReservationRequestDto();
                    request.setItemId(testItemId);
                    request.setUserId(userId);

                    reservationService.reserveTicket(request);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    String exType = e.getClass().getSimpleName();

                    if (msg.contains("optimistic") || exType.contains("Optimistic")) {
                        lockConflictCount.incrementAndGet();
                    } else if (msg.contains("Connection is not available") || msg.contains("timeout")) {
                        poolTimeoutCount.incrementAndGet();
                    } else {
                        otherFailures.incrementAndGet();
                    }
                } finally {
                    if (reqStart > 0) {
                        latenciesMs.add(System.currentTimeMillis() - reqStart);
                    }
                    endGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();
        endGate.await(60, TimeUnit.SECONDS);
        long totalExecutionTimeMs = System.currentTimeMillis() - benchmarkStartTime;
        executor.shutdown();

        // Calculate statistics
        Collections.sort(latenciesMs);
        long p50 = latenciesMs.isEmpty() ? 0 : latenciesMs.get((int) (latenciesMs.size() * 0.50));
        long p95 = latenciesMs.isEmpty() ? 0 : latenciesMs.get((int) (latenciesMs.size() * 0.95));
        long p99 = latenciesMs.isEmpty() ? 0 : latenciesMs.get((int) (latenciesMs.size() * 0.99));
        double throughputRps = (double) totalRequests / (totalExecutionTimeMs / 1000.0);

        System.out.println("\n================ BENCHMARK RESULTS ================");
        System.out.println("Total Requests Simulated : " + totalRequests);
        System.out.println("Concurrency Level        : " + simulatedConcurrentUsers + " parallel threads");
        System.out.println("Total Benchmark Time     : " + totalExecutionTimeMs + " ms");
        System.out.printf( "Calculated Throughput    : %.2f req/sec\n", throughputRps);
        System.out.println("---------------------------------------------------");
        System.out.println("Successful Reservations  : " + successCount.get());
        System.out.println("Optimistic Lock Aborts   : " + lockConflictCount.get());
        System.out.println("DB Pool Starvations      : " + poolTimeoutCount.get());
        System.out.println("Other Failures (Sold Out): " + otherFailures.get());
        System.out.println("---------------------------------------------------");
        System.out.println("Latency P50 (Median)     : " + p50 + " ms");
        System.out.println("Latency P95              : " + p95 + " ms");
        System.out.println("Latency P99              : " + p99 + " ms");
        System.out.println("===================================================\n");
    }
}

/*
================ BENCHMARK RESULTS ================
Total Requests Simulated : 200
Concurrency Level        : 50 parallel threads
Total Benchmark Time     : 48963 ms
Calculated Throughput    : 4.08 req/sec
---------------------------------------------------
Successful Reservations  : 41
Optimistic Lock Aborts   : 153
DB Pool Starvations      : 0
Other Failures (Sold Out): 6
        ---------------------------------------------------
Latency P50 (Median)     : 11601 ms
Latency P95              : 23012 ms
Latency P99              : 30018 ms
===================================================
*/