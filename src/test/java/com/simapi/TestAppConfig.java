package com.simapi;

import com.simapi.config.ApiConfig;
import com.simapi.config.Mapping;
import com.simapi.io.FileContentReader;
import com.simapi.server.ServerStats;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Log4jConfigurer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan(basePackages = {"com.simapi"},
        excludeFilters = @ComponentScan.Filter(value = org.springframework.context.annotation.Configuration.class, type = FilterType.ANNOTATION)
)
@PropertySource("classpath:test-sim_api.properties")
public class TestAppConfig {
    protected static final Log LOGGER;
    static {
        try {
            Log4jConfigurer.initLogging("classpath:sim-api-log4j.properties");
        }
        catch (FileNotFoundException e) {
        }

        LOGGER = LogFactory.getLog(TestAppConfig.class);
    }

    @Value("${boss.thread.count}")
    private int bossCount;

    @Value("${worker.thread.count}")
    private int workerCount;

    @Value("${scheduler.thread.count}")
    private int schedulerThreadCount;

    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup bossGroup() {
        return new NioEventLoopGroup(bossCount);
    }

    @Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup workerGroup() {
        return new NioEventLoopGroup(workerCount);
    }

    @Bean(name="scheduledExecutorService", destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(schedulerThreadCount);
    }

    @Bean(name = "apiConfig")
    public ApiConfig apiConfig() {
        ApiConfig config = null;
        try {
            JAXBContext context = JAXBContext.newInstance(ApiConfig.class);
            Unmarshaller unmarchaller = context.createUnmarshaller();
            config = (ApiConfig) unmarchaller.unmarshal(new ClassPathResource(
                    "test-sim-api-config.xml").getInputStream());
        }
        catch (Exception e) {
            LOGGER.error("Exception while loading sim-api-config.xml from classpath", e);
        }
        return config;
    }

    @Bean(name="servletStats")
    public ServerStats serverStats() {
        return new ServerStats(apiConfig());
    }

    @Bean(name = "fileContentReader")
    public FileContentReader fileContentReader() {
        ApiConfig apiConfig = apiConfig();
        List<String> classpathFileNames = new ArrayList<>();
        for (Mapping mapping : apiConfig.getMappings()) {
            classpathFileNames.add(mapping.getFileName());
        }
        FileContentReader contentReader = new FileContentReader();
        contentReader.init(classpathFileNames);
        return contentReader;
    }


    /**
     * Necessary to make the Value annotations work.
     *
     * @return
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
