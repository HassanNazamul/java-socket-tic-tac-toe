package com.testsocket.naz.nazmul.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new ArrayList<>();
    private final Map<String, WebSocketSession> idKeyMap = new HashMap<>();
    private final Map<WebSocketSession, String> sessionMap = new HashMap<>();
    private final Map<String, String> connectedUsers = new HashMap<>(); // Track connected user pairs
    private int userCounter = 1; // Simple counter for user IDs

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        String userKey = "User-" + userCounter++;

        idKeyMap.put(userKey, session);
        sessionMap.put(session, userKey);

        System.out.println("User connected: " + session);

        // Send user ID to the newly connected user
        session.sendMessage(new TextMessage("Note your User ID: " + userKey));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);

        String userIdToRemove = getUserIdFromSession(session);
        if (userIdToRemove != null) {
            sessionMap.remove(userIdToRemove);
            System.out.println(userIdToRemove + " disconnected.");
        }
    }

    @Override
    public void handleMessage(WebSocketSession senderSession, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        String senderId = getUserIdFromSession(senderSession);

        if (payload.startsWith("connect:")) {
            String receiverId = payload.substring(8).trim();

            // Establish the connection between the two users
            WebSocketSession receiverSession = getSessionFromUserId(receiverId);

            if (receiverSession != null) {
                connectedUsers.put(senderId, receiverId);
                connectedUsers.put(receiverId, senderId);

                senderSession.sendMessage(new TextMessage("You are connected with " + receiverId));
                receiverSession.sendMessage(new TextMessage("You are connected with " + senderId));

                System.out.println(senderId + " connected to " + receiverId);
            } else {
                senderSession.sendMessage(new TextMessage("User " + receiverId + " not found!"));
            }
        } else {
            // Check if sender is connected with someone
            String connectedUserId = connectedUsers.get(senderId);
            if (connectedUserId != null) {
                WebSocketSession connectedSession = getSessionFromUserId(connectedUserId);
                if (connectedSession != null && connectedSession.isOpen()) {
                    // Send the message only to the connected user
                    connectedSession.sendMessage(new TextMessage(senderId + ": " + payload));
                    System.out.println("Message from " + senderId + " to " + connectedUserId + ": " + payload);
                }
            }
        }
    }

    // Custom method to retrieve user ID from session
    private String getUserIdFromSession(WebSocketSession session) {
        for (Map.Entry<String, WebSocketSession> entry : idKeyMap.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Custom method to retrieve user ID from session
    private WebSocketSession getSessionFromUserId(String userId) {
        for (Map.Entry<WebSocketSession, String> entry : sessionMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }

        // System.out.println("Femal not found");
        return null;
    }
}
