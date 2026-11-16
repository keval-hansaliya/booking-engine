package com.flash.bookingengine.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${KAFKA_BOOTSTRAP_SERVERS}")
    private String bootstrapServers;

    @Value("${KAFKA_USERNAME}")
    private String username;

    @Value("${KAFKA_PASSWORD}")
    private String password;

    private Map<String, Object> baseSecurityProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
                        username, password));

        try {
            String caCert = new ClassPathResource("certs/ca.pem")
                    .getContentAsString(StandardCharsets.UTF_8);
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
            props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCert);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Aiven ca.pem from classpath:certs/ca.pem", e);
        }

        return props;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(baseSecurityProps());
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = baseSecurityProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = baseSecurityProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "booking-persistence-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.flash.bookingengine.*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}