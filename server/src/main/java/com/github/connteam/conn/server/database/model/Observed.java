package com.github.connteam.conn.server.database.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Observed {
    private int idObserver;
    private int idObserved;

    public int getIdObserver() {
        return idObserver;
    }

    public int getIdObserved() {
        return idObserved;
    }

    public void setIdObserver(int idObserver) {
        this.idObserver = idObserver;
    }

    public void setIdObserved(int idObserved) {
        this.idObserved = idObserved;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}