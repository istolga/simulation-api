package com.simapi.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

public class Mapping {
    public static enum REJECT_TYPE {
        REJECT, WAIT
    }

    private String uri;
    private int throuputInMin;
    private REJECT_TYPE rejectType = REJECT_TYPE.REJECT;
    private List<Latency> latencies = new ArrayList<>();

    private String fileName;

    @XmlAttribute(name = "uri")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @XmlAttribute(name = "throuput-in-min")
    public int getThrouputInMin() {
        return throuputInMin;
    }

    public void setThrouputInMin(int throuputInMin) {
        this.throuputInMin = throuputInMin;
    }

    @XmlAttribute(name = "reject-type")
    public REJECT_TYPE getRejectType() {
        return rejectType;
    }

    public void setRejectType(REJECT_TYPE rejectType) {
        this.rejectType = rejectType;
    }

    @XmlElementWrapper(name = "latencies")
    @XmlElement(name = "latency")
    public List<Latency> getLatencies() {
        return latencies;
    }

    public void setLatencies(List<Latency> latencies) {
        this.latencies = latencies;
    }

    @XmlAttribute(name = "filename")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "Mapping{" + "uri='" + uri + '\'' + ", rejectType=" + rejectType
          + ", latencies=" + latencies + '}';
    }
}
