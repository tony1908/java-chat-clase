package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Properties;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class KafkaConsumerService {
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final String TOPIC_NAME = "chat_messages";
    private static final String GROUP_ID = "java-chat-group";

    private Consumer<KafkaProducerService.ChatMessage> messageHandler;

    public KafkaConsumerService(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        this.consumer = new KafkaConsumer<>(props);
        this.objectMapper = new ObjectMapper();
    }

    public void setMessageHandler(Consumer<KafkaProducerService.ChatMessage> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));

            Thread consumerThread = new Thread(this::consume);
            consumerThread.setDaemon(true);
            consumerThread.setName("kafka-consumer");
            consumerThread.start();       
        }
    }

    private void consume() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        KafkaProducerService.ChatMessage chatMessage = objectMapper.readValue(
                            record.value(), 
                            KafkaProducerService.ChatMessage.class
                        );
                        
                        if (messageHandler != null) {
                            messageHandler.accept(chatMessage);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }

    public void stop() {
        running.set(false);
        if (consumer != null) {
            consumer.close();
        }
    }
}
