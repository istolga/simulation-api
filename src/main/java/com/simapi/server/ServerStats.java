package com.simapi.server;

import com.simapi.config.ApiConfig;
import com.simapi.config.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * singleton class to keep track of request statistics per uri User:
 */
public class ServerStats {
    
    private Map<String, StatsObject> statsMap = new ConcurrentHashMap<>();
    
    public ServerStats(@Qualifier("apiConfig") ApiConfig apiconfig) {
        initStatsMap(apiconfig);
    }
    
    private void initStatsMap(@Qualifier("apiConfig") ApiConfig apiconfig) {
        List<Mapping> mappingList = apiconfig.getMappings();
        for (Mapping mapping : mappingList) {
            StatsObject statsObject = createNewStatsObject(mapping.getUri());
            statsMap.put(statsObject.getName(), statsObject);
        }
    }
    
    private StatsObject createNewStatsObject(String uri) {
        StatsObject statsObject = new StatsObject();
        statsObject.setName(uri);
        statsObject.setStatsMinute(getCurrentMinute());
        
        return statsObject;
    }
    
    public void recordRequest(Mapping mapping) {
        if (null == mapping) {
            throw new IllegalArgumentException("mapping can't be null");
        }
        
        StatsObject statsObject = statsMap.get(mapping.getUri());
        if (null == statsObject) {
            statsObject = createNewStatsObject(mapping.getUri());
            statsMap.put(statsObject.getName(), statsObject);
        }
        
        synchronized (statsObject) {
            int currentMin = getCurrentMinute();
            if (statsObject.getStatsMinute() == currentMin) {
                statsObject.incrementCounter();
            }
            else {
                statsObject.setCounter(1);
                statsObject.setStatsMinute(currentMin);
            }
        }
    }
    
    public int getNumRequestPerMin(Mapping mapping) {
        if (null == mapping) {
            throw new IllegalArgumentException("mapping can't be null");
        }
        
        int numRequestPerMin = 0;
        StatsObject statsObject = statsMap.get(mapping.getUri());
        if (null == statsObject) {
            throw new IllegalStateException(
                    "recordRequest method should be called before");
        }
        synchronized (statsObject) {
            if (statsObject.getStatsMinute() == getCurrentMinute()) {
                numRequestPerMin = statsObject.getCounter();
            }
        }
        return numRequestPerMin;
    }
    
    protected int getCurrentMinute() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.MINUTE);
    }
}
