package com.simapi.config;

import javax.xml.bind.annotation.XmlAttribute;

public class Latency {
    private int percent;
    private int timems;

    @XmlAttribute(name = "percent")
    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    @XmlAttribute(name = "timems")
    public int getTimems() {
        return timems;
    }

    public void setTimems(int timems) {
        this.timems = timems;
    }

    @Override
    public String toString() {
        return "Latency{" + "percent=" + percent + ", timems=" + timems + '}';
    }
}
