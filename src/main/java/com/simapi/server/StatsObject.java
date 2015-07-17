package com.simapi.server;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class StatsObject {
    private String name;
    private int statsMinute;
    private int counter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatsMinute() {
        return statsMinute;
    }

    public void setStatsMinute(int statsMinute) {
        this.statsMinute = statsMinute;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void incrementCounter() {
        this.counter++;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        StatsObject statsObject = (StatsObject) obj;
        return new EqualsBuilder().append(name, statsObject.name).isEquals();
    }
}
