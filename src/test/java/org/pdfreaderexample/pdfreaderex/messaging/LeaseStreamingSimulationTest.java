package org.pdfreaderexample.pdfreaderex.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.RepeatedTest;
import org.pdfreaderexample.pdfreaderex.model.LeaseData;import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import; // 🚀 REQUIRED IMPORT
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Create Spring Boot application context for testing, inject custom config
// (to serialize the LeaseData bean), cleanup the context,
// and spin up lightweight Kafka broker running in KRaft mode in memory
// and expose a single topic (cre-lease-events)

@SpringBootTest(properties = { "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}" })
@Import(LeaseStreamingSimulationTest.TestKafkaConfig.class)
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"cre-lease-events"}
)
class LeaseStreamingSimulationTest {

    private final Logger log = LoggerFactory.getLogger(LeaseStreamingSimulationTest.class);

    private static final String TOPIC = "cre-lease-events";
    private static final BlockingQueue<String> assertionQueue = new LinkedBlockingQueue<>();  // main thread waits until data arrives
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    //@Test
    @RepeatedTest(10)   // run multiple times, to make sure Kafka is able to create the necessary server and consumer picks it up properly
    void shouldStreamExtractedLeaseDataThroughMessageQueue() throws Exception {
        // Arrange
        LeaseData outboundEvent = new LeaseData();
        outboundEvent.setLandlord("Apex Commercial Holdings LLC");
        outboundEvent.setTenant("Nexus Software Solutions Inc.");
        outboundEvent.setExpirationDate("September 30, 2031");

        String jsonPayload = objectMapper.writeValueAsString(outboundEvent);

        // Act
        kafkaTemplate.send(TOPIC, jsonPayload);

        // KRaft broker takes a split second to perform partition balancing on startup,
        // the first message might fire before the consumer is fully listening
        // to mitigate this issue, we add a retry loop
        // Here poll() method blocks the main thread and waits for up to 2 seconds
        // if it returns null, re-fires the message
        // Once the consumer establishes its partition handshake, it caches the message and
        // the loop breaks immediately
        String inboundJson = null;
        for (int i = 0; i < 5; i++) {
            inboundJson = assertionQueue.poll(2, TimeUnit.SECONDS);
            if (inboundJson != null) break;
            kafkaTemplate.send(TOPIC, jsonPayload); // Re-fire fallback safeguard
        }

        assertNotNull(inboundJson, "The message payload failed to clear the embedded broker queue within the allocation window.");


        // Deserialize the JSON back to LeaseData and assert to ensure no data was changed or lost during transit
        LeaseData inboundEvent = objectMapper.readValue(inboundJson, LeaseData.class);
        assertEquals("Apex Commercial Holdings LLC", inboundEvent.getLandlord());
        assertEquals("Nexus Software Solutions Inc.", inboundEvent.getTenant());
        assertEquals("September 30, 2031", inboundEvent.getExpirationDate());

        if (inboundEvent != null) {
            log.info("Deserialized LeaseData:" + inboundEvent.getTenant() + ", LandLord: " + inboundEvent.getLandlord() + ", Expiration Date: " + inboundEvent.getExpirationDate());
        } else {
            log.info("Deserialized LeaseData null");
        }

    }

    // Used to simulate microservice consumer
    // When broker brodcasts a message on the topic, Spring triggers this method to add the raw string
    // into the assertionQueue. Then wake up main thread to do the assertions
    @KafkaListener(topics = TOPIC, groupId = "cre-test-group")
    public void listen(String rawJsonMessage) {
        assertionQueue.add(rawJsonMessage);
    }

    @Configuration
    @EnableKafka  // Forces isolated tests to scan for and activate background @KafkaListener
    static class TestKafkaConfig {

        @org.springframework.beans.factory.annotation.Value("${spring.embedded.kafka.brokers}")
        private String bootstrapServers;

        @Bean
        public ProducerFactory<String, String> producerFactory() {
            Map<String, Object> config = new HashMap<>();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);  // serialize both key/value as string
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        public KafkaTemplate<String, String> kafkaTemplate() {
            return new KafkaTemplate<>(producerFactory());
        }

        @Bean
        public ConsumerFactory<String, String> consumerFactory() {
            Map<String, Object> config = new HashMap<>();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ConsumerConfig.GROUP_ID_CONFIG, "cre-test-group");
            config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory());
            return factory;
        }
    }
}
