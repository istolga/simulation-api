package com.simapi.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


/**
 * netty dispatcher channel initializer to define handlers for channel data back
 */
@Component
@Qualifier("dispatcherServletChannelInitializer")
public class DispatcherServletChannelInitializer extends
        ChannelInitializer<SocketChannel> {
    
    private static final Log LOGGER = LogFactory.getLog(DispatcherServletChannelInitializer.class);
    
    @Autowired()
    @Qualifier("servletNettyHandler")
    private ServletNettyHandler servletNettyHandler;
    
    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = channel.pipeline();
        
        if (LOGGER.isDebugEnabled()) {
            pipeline.addLast("log", new LoggingHandler(LogLevel.INFO));
        }
        // combines together HttpResponseEncoder and HttpRequestDecoder
        pipeline.addLast("codec", new HttpServerCodec());
        pipeline.addLast("handler", this.servletNettyHandler);
    }
}
