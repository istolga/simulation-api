package com.simapi.server;

import com.simapi.config.ApiConfig;
import com.simapi.config.Mapping;
import org.apache.commons.lang.mutable.MutableInt;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.*;


@Test(groups = "unit")
public class ServerStatsUnitTestNG {
    
    @Test(groups = "unit")
    public void testCalculateRequests() {
        Mapping mapping1 = new Mapping();
        mapping1.setUri("/path/uri1");
        Mapping mapping2 = new Mapping();
        mapping2.setUri("/path/uri2");

        List<Mapping> mappings = new ArrayList<>();
        mappings.add(mapping1);

        ApiConfig config = new ApiConfig();
        config.setMappings(mappings);

        final MutableInt currentMinute = new MutableInt(10);
        ServerStats serverStats = new ServerStats(config) {
            
            @Override
            protected int getCurrentMinute() {
                return currentMinute.intValue();
            }
        };

        
        serverStats.recordRequest(mapping1);
        assertEquals(serverStats.getNumRequestPerMin(mapping1), 1);
        try {
            serverStats.getNumRequestPerMin(mapping2);
            fail("Should be IllegalArgumentException");
        }
        catch (IllegalStateException e) {
        }
        
        serverStats.recordRequest(mapping1);
        assertEquals(serverStats.getNumRequestPerMin(mapping1), 2);
        
        serverStats.recordRequest(mapping2);
        assertEquals(serverStats.getNumRequestPerMin(mapping2), 1);
        
        currentMinute.setValue(15);
        assertEquals(serverStats.getNumRequestPerMin(mapping1), 0);
        assertEquals(serverStats.getNumRequestPerMin(mapping2), 0);
        
        serverStats.recordRequest(mapping2);
        assertEquals(serverStats.getNumRequestPerMin(mapping2), 1);
    }
    
    @Test(groups = "unit")
    public void testCalculateConcurrentRequests() throws InterruptedException {
        final Mapping mapping1 = new Mapping();
        mapping1.setUri("/path/uri1");
        final Mapping mapping2 = new Mapping();
        mapping2.setUri("/path/uri2");
        final Mapping mapping3 = new Mapping();
        mapping3.setUri("/path/uri3");

        List<Mapping> mappings = new ArrayList<>();
        mappings.add(mapping1);
        mappings.add(mapping2);

        ApiConfig config = new ApiConfig();
        config.setMappings(mappings);

        final MutableInt currentMinute = new MutableInt(10);
        final ServerStats serverStats = new ServerStats(config) {
            
            @Override
            protected int getCurrentMinute() {
                return currentMinute.intValue();
            }
        };

        
        Callable<Integer> t1 = new Callable<Integer>() {
            
            @Override
            public Integer call() throws Exception {
                serverStats.recordRequest(mapping1);
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                }
                serverStats.recordRequest(mapping1);
                
                return 0;
            }
        };
        
        Callable<Integer> t2 = new Callable<Integer>() {
            
            @Override
            public Integer call() throws Exception {
                serverStats.recordRequest(mapping2);
                serverStats.recordRequest(mapping2);
                serverStats.recordRequest(mapping2);
                serverStats.recordRequest(mapping2);
                
                return 0;
            }
        };
        Callable<Integer> t3 = new Callable<Integer>() {
            
            @Override
            public Integer call() throws Exception {
                serverStats.recordRequest(mapping3);
                serverStats.recordRequest(mapping3);
                serverStats.recordRequest(mapping3);
                serverStats.recordRequest(mapping3);
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                }
                serverStats.recordRequest(mapping3);
                serverStats.recordRequest(mapping3);
                serverStats.recordRequest(mapping3);
                serverStats.recordRequest(mapping3);
                
                return 0;
            }
            
        };
        
        ExecutorService executorPool = Executors.newFixedThreadPool(5);
        List<Callable<Integer>> callableList = new ArrayList<>();
        callableList.add(t1);
        callableList.add(t2);
        callableList.add(t3);
        executorPool.invokeAll(callableList);
        
        assertEquals(serverStats.getNumRequestPerMin(mapping1), 2);
        assertEquals(serverStats.getNumRequestPerMin(mapping2), 4);
        assertEquals(serverStats.getNumRequestPerMin(mapping3), 8);
    }
}
