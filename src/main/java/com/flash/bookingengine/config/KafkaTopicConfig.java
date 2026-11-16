package com.flash.bookingengine.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic reservationEventsTopic() {
        return TopicBuilder.name("reservation-events")
                .partitions(2)
                .replicas(1)
                .build();
    }
}