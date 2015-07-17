package com.simapi.config;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * class to hold configuration for api urls, latency and other characteristics
 */
@XmlRootElement(name = "configuration")
public class ApiConfig {
    
    private List<Mapping> mappings;
    
    @XmlElementWrapper(name = "mappings")
    @XmlElement(name = "mapping")
    public List<Mapping> getMappings() {
        return mappings;
    }
    
    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }
    
    @Override
    public String toString() {
        return "ApiConfig{" + "mappings=" + mappings + '}';
    }
}
