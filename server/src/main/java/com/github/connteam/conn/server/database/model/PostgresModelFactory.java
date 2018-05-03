package com.github.connteam.conn.server.database.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresModelFactory {
    public static Observed observedFromResultSet(ResultSet rs) throws SQLException {
        Observed observed = new Observed();
        observed.setIdObserved(rs.getInt("id_observed"));
        observed.setIdObserver(rs.getInt("id_observer"));
        return observed;
    }
}