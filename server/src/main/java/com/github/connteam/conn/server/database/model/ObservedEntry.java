package com.github.connteam.conn.server.database.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ObservedEntry {
    private int idObserver;
    private int idObserved;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObservedEntry) {
            ObservedEntry x = (ObservedEntry) obj;
            return new EqualsBuilder().append(idObserver, x.idObserver).append(idObserved, x.idObserved).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(idObserver).append(idObserved).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

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
}