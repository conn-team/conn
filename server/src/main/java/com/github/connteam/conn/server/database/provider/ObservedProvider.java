package com.github.connteam.conn.server.database.provider;

import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.exception.DatabaseException;
import com.github.connteam.conn.server.database.model.Observed;

public interface ObservedProvider {
    Stream<Observed> selectByIdObserver(int idObserver) throws DatabaseException;

    Stream<Observed> selectByIdObserved(int idObserved) throws DatabaseException;

    boolean insert(@NotNull Observed observed) throws DatabaseException;

    boolean update(@NotNull Observed observed) throws DatabaseException;

    boolean delete(@NotNull Observed observed) throws DatabaseException;
}
