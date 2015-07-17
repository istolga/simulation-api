package com.simapi;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;


public class TestProperties {
    
    private static final Log log = LogFactory.getLog(TestProperties.class);
    
    private static InputStream tProps = ClassLoader.getSystemClassLoader().getResourceAsStream(
            "test.properties");
    
    private static TestProperties INSTANCE = new TestProperties(tProps);
    private Properties testProps;
    
    private TestProperties (InputStream tProps) {
        loadProperties(tProps);
    }
    
    public static TestProperties getInstance() {
        return INSTANCE;
    }
    
    private void loadProperties(InputStream tProps) {
        try {
            testProps = new Properties();
            testProps.load(tProps);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not load Test properties from file:"
                    + tProps, e);
        }
    }
    
    private String getProperty(String propName) {
        Assert.assertNotNull(testProps, "Test setup is not initialized..");
        return testProps.getProperty(propName);
    }
    
    public String getApiSimulationHostUrl() {
        return getProperty("api.simulation.host.url");
    }
    
    public String getLargeAsyncRequestFileName() {
        return getProperty("large.async.request.filename");
    }
}
