package com.simapi;

import com.simapi.config.AppConfig;
import com.simapi.server.NettyServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;


/**
 * main class to run netty server. This will block current thread until server
 * socket is closed
 */
public class Main {
    
    private static final Log LOGGER = LogFactory.getLog(Main.class);
    
    public static void main(String[] args) throws Exception {
        ConfigurableWebApplicationContext ctx = createWebContext();
        
        NettyServer nettyServer = (NettyServer) ctx.getBean("nettyServer");
        nettyServer.runServer();
    }
    
    private static ConfigurableWebApplicationContext createWebContext() {
        LOGGER.info("Starting application context");
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.register(AppConfig.class);
        ctx.refresh();
        ctx.registerShutdownHook();
        
        return ctx;
    }
}
