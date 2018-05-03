package com.github.connteam.conn.server.database.provider;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.Observed;

public interface ObservedProvider {
    List<Observed> getObserved(int idObserver) throws DatabaseException;

    List<Observed> getObservers(int idObserved) throws DatabaseException;

    boolean insertObserved(@NotNull Observed observed) throws DatabaseException;

    boolean deleteObserved(@NotNull Observed observed) throws DatabaseException;
}
