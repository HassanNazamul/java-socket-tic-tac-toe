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

    // List of all connected sessions
    private final List<WebSocketSession> sessions = new ArrayList<>();
    // Map to store user ID with session
    private final Map<String, WebSocketSession> idKeyMap = new HashMap<>();
    private final Map<WebSocketSession, String> sessionMap = new HashMap<>();
    // Map for tracking connected users (pairs)
    private final Map<String, String> connectedUsers = new HashMap<>();
    // Store the color state of each user
    private final Map<String, Map<String, Boolean>> userColors = new HashMap<>();
    private int userCounter = 1;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        
        // Generate unique user ID
        String userKey = "User-" + userCounter++;
        idKeyMap.put(userKey, session);
        sessionMap.put(session, userKey);

        // Initialize user color state
        userColors.put(userKey, new HashMap<>());
        userColors.get(userKey).put("red", false);
        userColors.get(userKey).put("green", false);
        userColors.get(userKey).put("blue", false);

        System.out.println("User connected: " + session.getId());

        // Send the User ID to the newly connected user
        session.sendMessage(new TextMessage("Note your User ID: " + userKey));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);

        // Find and remove the user from the session map
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
        System.out.println("Received message from " + senderId + ": " + payload);

        if (payload.startsWith("connect:")) {
            // Handle connection requests
            String receiverId = payload.substring(8).trim();
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
        } else if (payload.startsWith("color:")) {
            // Handle color state updates
            String colorState = payload.substring(6).trim();
            String[] colorParts = colorState.split(":");
            if (colorParts.length == 2) {
                String color = colorParts[0];
                boolean state = Boolean.parseBoolean(colorParts[1]);

                // Update the color state of the sender user
                if (userColors.containsKey(senderId)) {
                    userColors.get(senderId).put(color, state);
                    System.out.println(senderId + " updated color " + color + " to " + state);

                    // Broadcast color state update to connected users
                    String connectedUserId = connectedUsers.get(senderId);
                    if (connectedUserId != null) {
                        WebSocketSession connectedSession = getSessionFromUserId(connectedUserId);
                        if (connectedSession != null && connectedSession.isOpen()) {
                            connectedSession.sendMessage(new TextMessage(senderId + " changed color " + color + " to " + state));
                            System.out.println("Broadcasted color change from " + senderId + " to " + connectedUserId + ": " + color + " " + state);
                        }
                    }
                }
            }
        } else {
            // Handle regular messages
            String connectedUserId = connectedUsers.get(senderId);
            if (connectedUserId != null) {
                WebSocketSession connectedSession = getSessionFromUserId(connectedUserId);
                if (connectedSession != null && connectedSession.isOpen()) {
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

    // Custom method to retrieve session from user ID
    private WebSocketSession getSessionFromUserId(String userId) {
        for (Map.Entry<WebSocketSession, String> entry : sessionMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
