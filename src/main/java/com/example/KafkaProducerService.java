package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class KafkaProducerService {
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_NAME = "chat_messages";

    public KafkaProducerService(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        this.producer = new KafkaProducer<>(props);
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Void> sendMessage(String sessionId, String room, String message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            ChatMessage chatMessage = new ChatMessage(sessionId, room, message);
            String jsonMessage = objectMapper.writeValueAsString(chatMessage);

            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, room, jsonMessage);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    future.completeExceptionally(exception);
                } else {
                    future.complete(null);
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public static class ChatMessage {
        private String sessionId;
        private String room;
        private String message;
        private long timestamp;

        public ChatMessage(String sessionId, String room, String message) {
            this.sessionId = sessionId;
            this.room = room;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }


}
