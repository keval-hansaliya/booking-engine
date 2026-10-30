package com.flash.bookingengine.config;

import com.flash.bookingengine.model.Item;
import com.flash.bookingengine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        Item item;

        if (itemRepository.count() == 0) {
            Item flashItem = Item.builder()
                    .name("VIP Concert Ticket")
                    .totalStock(100)
                    .availableStock(100)
                    .build();
            item = itemRepository.save(flashItem);
            log.info("Created new item in DB: ID={}, Name='{}', Stock={}",
                    item.getId(), item.getName(), item.getAvailableStock());
        } else {
            item = itemRepository.findAll().get(0);
            log.info("Found existing item in DB: ID={}, Name='{}', Available Stock={}",
                    item.getId(), item.getName(), item.getAvailableStock());
        }

        // Cache warm-up: Push available stock to Redis under 'item:{id}:stock'
        String stockKey = "item:" + item.getId() + ":stock";
        redisTemplate.opsForValue().set(stockKey, String.valueOf(item.getAvailableStock()));

        log.info("==========================================");
        log.info("WARMED REDIS CACHE: Key='{}', Stock={}", stockKey, item.getAvailableStock());
        log.info("==========================================");
    }
}