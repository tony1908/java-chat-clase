package com.example;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;

public class ChatWebSocket extends WebSocketAdapter {
    private static KafkaProducerService producerService;

    public static void setProducerService(KafkaProducerService producer) {
        producerService = producer;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        System.out.println("WebSocket connected: " + session.getRemoteAddress());
    }

    @Override
    public void onWebSocketText(String message) {
        String userId = WebsocketManager.getUserId(this.getSession());

        try {
            if (userId != null) {
                String[] parts = message.trim().substring(5).trim().split(":");
                if (parts.length >= 1 && !parts[0].isEmpty()) {
                    String newUserId = parts[0];
                    String room = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : "general";

                    WebsocketManager.addSession(this.getSession(), newUserId, room);
                    this.getSession().getRemote().sendString("Welcome " + newUserId + " to " + room);
                    WebsocketManager.broadcastMessage(room, "User " + newUserId + " has joined the room");
                } else {
                    this.getSession().getRemote().sendString("Invalid message format");
                }
            } else {
                String userRoom = WebsocketManager.getUserRoom(this.getSession());
                String chatMessage = userId + ": " + message;

                producerService.sendMessage(userId, userRoom, chatMessage).thenRun(() -> {
                    System.out.println("Message sent: " + chatMessage);
                }).exceptionally(ex -> {
                    System.err.println("Failed to send message: " + ex.getMessage());
                    return null;
                });   
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        
        String userId = WebsocketManager.getUserId(this.getSession());
        String UserRoom = WebsocketManager.getUserRoom(this.getSession());
        
        WebsocketManager.removeSession(this.getSession());

        WebsocketManager.broadcastMessage(UserRoom, "User " + userId + " has left the room");
        
    }

    
}
