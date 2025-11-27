package com.lumichat.im.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.im.handler.WebSocketFrameHandler;
import com.lumichat.im.service.MessageProcessor;
import com.lumichat.im.session.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketServerConfig {

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;
    private final ObjectMapper objectMapper;

    @Value("${im.websocket.port:7901}")
    private int wsPort;

    @Value("${im.websocket.path:/ws}")
    private String wsPath;

    @Value("${im.session.heartbeat-interval:30000}")
    private long heartbeatInterval;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP codec for WebSocket handshake
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            // WebSocket protocol handler
                            pipeline.addLast(new WebSocketServerProtocolHandler(wsPath, null, true));

                            // Idle state handler for heartbeat timeout
                            long readTimeout = heartbeatInterval * 3 / 1000; // 3x heartbeat interval
                            pipeline.addLast(new IdleStateHandler(readTimeout, 0, 0, TimeUnit.SECONDS));

                            // Custom WebSocket frame handler
                            pipeline.addLast(new WebSocketFrameHandler(sessionManager, messageProcessor, objectMapper));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = b.bind(wsPort).sync().channel();
            log.info("WebSocket server started on port {}", wsPort);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to start WebSocket server", e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down WebSocket server...");

        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("WebSocket server stopped");
    }
}
