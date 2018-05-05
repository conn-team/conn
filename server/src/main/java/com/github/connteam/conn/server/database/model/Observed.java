package com.github.connteam.conn.server.database.model;

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
}