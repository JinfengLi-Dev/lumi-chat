package com.lumichat.im.protocol;

public final class ProtocolType {
    // Client to Server
    public static final int LOGIN = 1;
    public static final int LOGOUT = 2;
    public static final int HEARTBEAT = 3;
    public static final int CHAT_MESSAGE = 10;
    public static final int TYPING = 11;
    public static final int READ_ACK = 12;
    public static final int RECALL_MESSAGE = 13;
    public static final int SYNC_REQUEST = 20;
    public static final int OFFLINE_SYNC_REQUEST = 21;  // Request offline messages on connect
    public static final int OFFLINE_SYNC_ACK = 22;      // Acknowledge offline message delivery
    public static final int ONLINE_STATUS_REQUEST = 23; // Request online status for user IDs
    public static final int ONLINE_STATUS_SUBSCRIBE = 24; // Subscribe to online status changes

    // Server to Client
    public static final int LOGIN_RESPONSE = 101;
    public static final int LOGOUT_RESPONSE = 102;
    public static final int HEARTBEAT_RESPONSE = 103;
    public static final int CHAT_MESSAGE_ACK = 110;
    public static final int RECEIVE_MESSAGE = 111;
    public static final int TYPING_NOTIFY = 112;
    public static final int RECALL_ACK = 113;
    public static final int RECALL_NOTIFY = 114;
    public static final int SYNC_RESPONSE = 120;
    public static final int OFFLINE_SYNC_RESPONSE = 121;  // Offline messages delivery
    public static final int OFFLINE_SYNC_COMPLETE = 122;  // All offline messages delivered
    public static final int ONLINE_STATUS_RESPONSE = 123; // Response with user online statuses
    public static final int ONLINE_STATUS_CHANGE = 124;   // Broadcast when a user's status changes
    public static final int READ_RECEIPT_NOTIFY = 125;    // Notify sender that messages were read
    public static final int REACTION_NOTIFY = 126;        // Notify when a reaction is added/removed

    // System Messages
    public static final int KICKED_OFFLINE = 200;
    public static final int SERVER_ERROR = 500;

    private ProtocolType() {}
}
