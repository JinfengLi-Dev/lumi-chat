package com.lumichat.im.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.protocol.Packet;
import com.lumichat.im.protocol.ProtocolType;
import com.lumichat.im.service.MessageProcessor;
import com.lumichat.im.session.SessionManager;
import com.lumichat.im.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;
    private final ObjectMapper objectMapper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame textFrame) {
            String text = textFrame.text();
            log.debug("Received message: {}", text);

            try {
                Packet packet = objectMapper.readValue(text, Packet.class);
                handlePacket(ctx, packet);
            } catch (Exception e) {
                log.error("Failed to parse message: {}", text, e);
                sendError(ctx, "Invalid message format");
            }
        } else {
            log.warn("Unsupported frame type: {}", frame.getClass().getName());
        }
    }

    private void handlePacket(ChannelHandlerContext ctx, Packet packet) {
        sessionManager.updateLastActive(ctx.channel());

        switch (packet.getType()) {
            case ProtocolType.LOGIN -> messageProcessor.handleLogin(ctx, packet);
            case ProtocolType.LOGOUT -> messageProcessor.handleLogout(ctx, packet);
            case ProtocolType.HEARTBEAT -> messageProcessor.handleHeartbeat(ctx, packet);
            case ProtocolType.CHAT_MESSAGE -> messageProcessor.handleChatMessage(ctx, packet);
            case ProtocolType.TYPING -> messageProcessor.handleTyping(ctx, packet);
            case ProtocolType.READ_ACK -> messageProcessor.handleReadAck(ctx, packet);
            case ProtocolType.RECALL_MESSAGE -> messageProcessor.handleRecall(ctx, packet);
            case ProtocolType.SYNC_REQUEST -> messageProcessor.handleSyncRequest(ctx, packet);
            case ProtocolType.OFFLINE_SYNC_REQUEST -> messageProcessor.handleOfflineSyncRequest(ctx, packet);
            case ProtocolType.OFFLINE_SYNC_ACK -> messageProcessor.handleOfflineSyncAck(ctx, packet);
            case ProtocolType.ONLINE_STATUS_REQUEST -> messageProcessor.handleOnlineStatusRequest(ctx, packet);
            case ProtocolType.ONLINE_STATUS_SUBSCRIBE -> messageProcessor.handleOnlineStatusSubscribe(ctx, packet);
            default -> log.warn("Unknown packet type: {}", packet.getType());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("WebSocket connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        UserSession session = sessionManager.getSessionByChannel(ctx.channel());
        if (session != null) {
            log.info("WebSocket disconnected: userId={}, deviceId={}",
                    session.getUserId(), session.getDeviceId());
            messageProcessor.handleDisconnect(session);
        }
        sessionManager.removeSession(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket error: {}", cause.getMessage(), cause);
        ctx.close();
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        try {
            Packet errorPacket = Packet.of(ProtocolType.SERVER_ERROR,
                    java.util.Map.of("error", message));
            String json = objectMapper.writeValueAsString(errorPacket);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }
}
