package com.kevshake.gateway.components;

import org.jpos.iso.ISOPackager;
import org.jpos.q2.Q2;
import org.jpos.util.NameRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

@Component
public class IsoServer {
    private static final Logger log = LoggerFactory.getLogger(IsoServer.class);

    @Autowired
    @Qualifier("posPackager")
    private ISOPackager posPackager;
    
    @Autowired
    private BankCommunicationConfig config;
    
    @Autowired
    private MaskedLogger maskedLogger;
    
    @Autowired
    private IsoServerHandler serverHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    @PostConstruct
    public void startServer() {
        // Start server in a separate thread to avoid blocking Spring startup
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

            ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new LengthFieldBasedFrameDecoder(10240, 0, 2, 0, 2))
                            .addLast(new IsoMessageDecoder(posPackager)) // POS packager for incoming
                            .addLast(new IsoMessageEncoder(posPackager)) // POS packager for responses
                            .addLast(serverHandler);                     // Business logic handler
                    }
                });

            try {
                int port = config.getPos().getPort();
                serverFuture = b.bind(port);
                serverFuture.sync();
                
                log.info("ISO8583 POS Server started on port {} with {} channel", 
                    port, config.getPos().getChannelType());
                maskedLogger.logSystemEvent("SERVER_START", 
                    String.format("POS Server started on port %d", port));
                
                serverFuture.channel().closeFuture().sync();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Server startup interrupted", e);
            } catch (Exception e) {
                log.error("Error starting ISO8583 server", e);
                maskedLogger.logError("SERVER_START", "Failed to start server", e);
            } finally {
                shutdown();
            }
        }, "ISO8583-Server").start();
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ISO8583 Server");
        
        if (serverFuture != null) {
            serverFuture.channel().close();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        maskedLogger.logSystemEvent("SERVER_STOP", "POS Server stopped");
    }
}