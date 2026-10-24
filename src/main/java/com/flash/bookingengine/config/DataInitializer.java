package com.flash.bookingengine.config;

import com.flash.bookingengine.model.Item;
import com.flash.bookingengine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;

    @Override
    public void run(String... args) {
        if (itemRepository.count() == 0) {
            Item flashItem = Item.builder()
                    .name("VIP Concert Ticket")
                    .totalStock(100)
                    .availableStock(100)
                    .build();

            Item savedItem = itemRepository.save(flashItem);
            log.info("Initialized test inventory: ID={}, Name='{}', Stock={}",
                    savedItem.getId(), savedItem.getName(), savedItem.getAvailableStock());
        }
    }
}