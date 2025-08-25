package com.example;

import org.eclipse.jetty.websocket.api.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class WebsocketManager {
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArraySet<Session>>> sessionsByRoom = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, String> usersBySession = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, String> roomsBySession = new ConcurrentHashMap<>();

    public static void addSession(Session session, String userId, String room) {
        usersBySession.put(session, userId);
        roomsBySession.put(session, room);

        sessionsByRoom.computeIfAbsent(room, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>())
            .add(session);
    }

    public static void removeSession(Session session) {
        String userId = usersBySession.remove(session);
        String room = roomsBySession.remove(session);

        if (userId != null && room != null) {
            ConcurrentHashMap<String, CopyOnWriteArraySet<Session>> roomUsers = sessionsByRoom.get(room);
            if (roomUsers != null) {
                CopyOnWriteArraySet<Session> userSessions = roomUsers.get(userId);
                if (userSessions != null) {
                    userSessions.remove(session);
                    if (userSessions.isEmpty()) {
                        roomUsers.remove(userId);
                        if (roomUsers.isEmpty()) {
                            sessionsByRoom.remove(room);
                        }
                    }
                }           
            }
        }
    }

    public static String getUserId(Session session) {
        return usersBySession.get(session);
    }

    public static String getUserRoom(Session session) {
        return roomsBySession.get(session);
    }

    public static void broadcastMessage(String room, String message) {
        ConcurrentHashMap<String, CopyOnWriteArraySet<Session>> roomUsers = sessionsByRoom.get(room);

        if (roomUsers != null) {
            for (CopyOnWriteArraySet<Session> userSessions : roomUsers.values()) {
                userSessions.removeIf(session -> {
                    try {
                        session.getRemote().sendString(message);
                        return false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return true;
                    }
                });
            }
        }
    }
}
