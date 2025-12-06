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

    // System Messages
    public static final int KICKED_OFFLINE = 200;
    public static final int SERVER_ERROR = 500;

    private ProtocolType() {}
}
