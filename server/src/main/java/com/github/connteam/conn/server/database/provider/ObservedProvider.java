package com.github.connteam.conn.server.database.provider;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.ObservedEntry;

public interface ObservedProvider {
    List<ObservedEntry> getObserved(int idObserver) throws DatabaseException;

    List<ObservedEntry> getObservers(int idObserved) throws DatabaseException;

    boolean insertObserved(@NotNull ObservedEntry observed) throws DatabaseException;

    boolean deleteObserved(@NotNull ObservedEntry observed) throws DatabaseException;
}
