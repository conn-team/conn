package com.github.connteam.conn.server.database.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresModelFactory {
    public static EphemeralKey ephemeralKeyFromResultSet(ResultSet rs) throws SQLException {
        EphemeralKey ephemeralKey = new EphemeralKey();
        ephemeralKey.setIdKey(rs.getInt("id_key"));
        ephemeralKey.setIdUser(rs.getInt("id_user"));
        ephemeralKey.setKey(rs.getBytes("key"));
        ephemeralKey.setSignature(rs.getBytes("signature"));
        return ephemeralKey;
    }

    public static Message messageFromResultSet(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setIdMessage(rs.getInt("id_message"));
        message.setIdFrom(rs.getInt("id_from"));
        message.setIdTo(rs.getInt("id_to"));
        message.setMessage(rs.getBytes("message"));
        message.setKey(rs.getBytes("key"));
        message.setSignature(rs.getBytes("signature"));
        message.setTime(rs.getTimestamp("time"));
        return message;
    }

    public static Observed observedFromResultSet(ResultSet rs) throws SQLException {
        Observed observed = new Observed();
        observed.setIdObserved(rs.getInt("id_observed"));
        observed.setIdObserver(rs.getInt("id_observer"));
        return observed;
    }

    public static User userFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setIdUser(rs.getInt("id_user"));
        user.setUsername(rs.getString("username"));
        user.setPublicKey(rs.getBytes("public_key"));
        user.setSignupTime(rs.getTimestamp("signup_time"));
        return user;
    }
}