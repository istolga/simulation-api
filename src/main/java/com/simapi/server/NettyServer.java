package com.simapi.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * class to run netty server User: okhylkouskaya Date: 5/12/14
 */
@Component
@Qualifier("nettyServer")
public class NettyServer {
    
    private static final Log LOGGER = LogFactory.getLog(NettyServer.class);
    
    @Autowired()
    @Qualifier("bossGroup")
    private EventLoopGroup bossGroup;
    
    @Autowired()
    @Qualifier("workerGroup")
    private EventLoopGroup workerGroup;
    
    @Autowired()
    @Qualifier("dispatcherServletChannelInitializer")
    private DispatcherServletChannelInitializer dispatcherServletChannelInitializer;
    
    @Value("${server.port}")
    private int port;
    
    public void runServer() throws Exception {
        LOGGER.info("running.... server on port: " + port);
        
        ServerBootstrap server = new ServerBootstrap();
        try {
            server.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).localAddress(
                    port).childHandler(dispatcherServletChannelInitializer);
            
            // Start the server.
            ChannelFuture f = server.bind(port).sync();
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        }
        finally {
            LOGGER.info("shutting down server on port: " + port);
        }
    }
}
