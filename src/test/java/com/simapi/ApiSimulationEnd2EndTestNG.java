package com.simapi;

import com.simapi.server.NettyServer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Test(groups = "functional")
public class ApiSimulationEnd2EndTestNG {
    
    private static final Log LOGGER = LogFactory.getLog(ApiSimulationEnd2EndTestNG.class);
    
    private ConfigurableWebApplicationContext ctx;
    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    
    private static final String PORT = "8088";
    
    @BeforeClass
    public void setUp() throws Exception {
        ctx = createWebContext();
        
        final NettyServer nettyServer = (NettyServer) ctx.getBean("nettyServer");
        executorService.execute(new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				nettyServer.runServer();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}  	   
    	});
    }
     
    @AfterClass
    public void tearDown() {
        ctx.close();
        executorService.shutdown();
    }
    
    @Test(groups = "functional")
    public void testSuccessPath() throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://localhost:" + PORT
                + "/enterprise/someapi?someparam=value");
        HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
        assertEquals(readHttpResponse2Str(response), "<root>\n"
                + "    <text>This is api1 xml</text>\n" + "</root>");

        client = new DefaultHttpClient();
        request = new HttpGet("http://localhost:" + PORT + "/enterprise/someapi20");
        response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
        assertEquals(readHttpResponse2Str(response), "<root>\n" + "    <text>\n"
                + "        This is api2.xml\n" + "    </text>\n" + "</root>");
        
        client = new DefaultHttpClient();
        request = new HttpGet("http://localhost:" + PORT + "//enterprise/someapi20/");
        request.addHeader("Content-Type", "application/json");
        response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
        assertEquals(readHttpResponse2Str(response), "[\n" + "    {\n"
                + "        \"name\": \"This is api2.json\"\n" + "    }\n" + "]");
        
    }
    
    private String readHttpResponse2Str(HttpResponse response) throws IOException {
        InputStream in = response.getEntity().getContent();
        StringWriter out = new StringWriter();
        IOUtils.copy(in, out);
        
        return out.toString();
    }
    
    @Test(groups = "functional")
    public void testNotFoundPath() throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://localhost:" + PORT
                + "/enterprise_doesnt_exist");
        HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 404);
    }
    
    @Test(groups = "functional")
    public void testTooManyRequests() throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://localhost:" + PORT + "/enterprise_throuput");
        HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 429);
    }
    
    private static ConfigurableWebApplicationContext createWebContext() {
        LOGGER.info("Starting application context");
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.register(TestAppConfig.class);
        ctx.refresh();
        ctx.registerShutdownHook();
        
        return ctx;
    }
}
