package com.simapi.server;

import com.simapi.config.ApiConfig;
import com.simapi.config.Mapping;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.testng.Assert.*;

@Test(groups = "unit")
public class ServletNettyHandlerUnitTestNG {
    
    private ApiConfig config;
    
    @BeforeClass
    public void setUp() throws Exception {
        JAXBContext context = JAXBContext.newInstance(ApiConfig.class);
        Unmarshaller unmarchaller = context.createUnmarshaller();
        config = (ApiConfig) unmarchaller.unmarshal(new ClassPathResource(
                "test-sim-api-config.xml").getInputStream());
    }
    
    @Test(groups = "unit")
    public void testFindUriMatchedMapping() {
        UriResolver uriResolver = new UriResolver();
        uriResolver.setApiConfig(config);
        try {
            uriResolver.findUriMatchedMapping(null);
            fail("Should be IllegalArgumentException for null uri");
        }
        catch (IllegalArgumentException e) {
        }
        
        Mapping mapping = uriResolver.findUriMatchedMapping("");
        assertNull(mapping);
        
        mapping = uriResolver.findUriMatchedMapping("not_existing");
        assertNull(mapping);
        
        mapping = uriResolver.findUriMatchedMapping("/enterprise/someapi");
        assertNotNull(mapping);
        assertEquals(mapping.getRejectType(), Mapping.REJECT_TYPE.REJECT);
        assertEquals(mapping.getThrouputInMin(), 300);
        assertEquals(mapping.getFileName(), "response/api1.xml");
        
        mapping = uriResolver.findUriMatchedMapping("/enterprise/someapi/");
        assertNotNull(mapping);
        assertEquals(mapping.getRejectType(), Mapping.REJECT_TYPE.REJECT);
        assertEquals(mapping.getThrouputInMin(), 300);
        assertEquals(mapping.getFileName(), "response/api1.xml");
    }
    
    @Test(groups = "unit")
    public void testGetServiceDelay() throws Exception {
        ServletNettyHandler servletNettyHandler = new ServletNettyHandler();
        
        UriResolver uriResolver = new UriResolver();
        uriResolver.setApiConfig(config);
        
        long delay = servletNettyHandler.getServiceDelay(uriResolver.findUriMatchedMapping("enterprise2"));
        assertEquals(delay, 30L);
        
        delay = servletNettyHandler.getServiceDelay(uriResolver.findUriMatchedMapping("enterprise3"));
        assertEquals(delay, 30L);
        
        delay = servletNettyHandler.getServiceDelay(uriResolver.findUriMatchedMapping("enterprise4"));
        assertEquals(delay, 0L);
    }
    
    @Test(groups = "unit")
    public void testGetContentMimeType() {
        ServletNettyHandler servletNettyHandler = new ServletNettyHandler();
        DefaultHttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.GET, "someuri/uri");
        
        assertEquals(servletNettyHandler.getContentMimeType(httpRequest, ""),
                "application/xml");
        assertEquals(servletNettyHandler.getContentMimeType(httpRequest, "api1"),
                "application/xml");
        assertEquals(servletNettyHandler.getContentMimeType(httpRequest, "api1.xml"),
                "application/xml");
        assertEquals(servletNettyHandler.getContentMimeType(httpRequest, "api1.json"),
                "application/json");
        
        httpRequest.headers().add(CONTENT_TYPE, "application/json");
        assertEquals(servletNettyHandler.getContentMimeType(httpRequest, "api1"),
                "application/json");
    }
}
