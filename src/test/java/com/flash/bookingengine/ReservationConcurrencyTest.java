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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ReservationConcurrencyTest {

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

        // Seed 1 item with exactly 10 units of stock
        Item item = Item.builder()
                .name("Flash Sale iPhone")
                .totalStock(10)
                .availableStock(10)
                .build();
        testItemId = itemRepository.save(item).getId();
    }

    @Test
    void testConcurrentReservations_causesOptimisticLockCollisions() throws InterruptedException {
        int numberOfThreads = 100; // 100 users competing for 10 items
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(1); // Gunshot barrier to fire simultaneously

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user_" + i;
            executorService.submit(() -> {
                try {
                    latch.await(); // Wait until all threads are prepped

                    ReservationRequestDto request = new ReservationRequestDto();
                    request.setItemId(testItemId);
                    request.setUserId(userId);

                    reservationService.reserveTicket(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Catches ObjectOptimisticLockingFailureException or Sold out
                    failureCount.incrementAndGet();
                }
            });
        }

        // Fire all threads at once
        latch.countDown();
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(50);
        }

        System.out.println("==========================================");
        System.out.println("SUCCESSFUL RESERVATIONS: " + successCount.get());
        System.out.println("FAILED (COLLISIONS / SOLD OUT): " + failureCount.get());
        System.out.println("==========================================");

        Item finalItem = itemRepository.findById(testItemId).orElseThrow();
        long actualReservationsInDb = reservationRepository.count();

        System.out.println("FINAL DB STOCK: " + finalItem.getAvailableStock());
        System.out.println("TOTAL RESERVATION ROWS CREATED: " + actualReservationsInDb);
        System.out.println("==========================================");

        // Verification: Did we oversell?
        // Stock + reservations must always equal the initial total stock (10)
        assertEquals(10, finalItem.getAvailableStock() + actualReservationsInDb,
                "Inventory was corrupted! Overselling occurred.");
    }
}