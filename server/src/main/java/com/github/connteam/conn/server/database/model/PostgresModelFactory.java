package com.github.connteam.conn.server.database.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresModelFactory {
    public static EphemeralKeyEntry ephemeralKeyFromResultSet(ResultSet rs) throws SQLException {
        EphemeralKeyEntry ephemeralKey = new EphemeralKeyEntry();
        ephemeralKey.setIdKey(rs.getInt("id_key"));
        ephemeralKey.setIdUser(rs.getInt("id_user"));
        ephemeralKey.setKey(rs.getBytes("key"));
        ephemeralKey.setSignature(rs.getBytes("signature"));
        return ephemeralKey;
    }

    public static MessageEntry messageFromResultSet(ResultSet rs) throws SQLException {
        MessageEntry message = new MessageEntry();
        message.setIdMessage(rs.getInt("id_message"));
        message.setIdFrom(rs.getInt("id_from"));
        message.setIdTo(rs.getInt("id_to"));
        message.setMessage(rs.getBytes("message"));
        message.setPartialKey1(rs.getBytes("partial_key1"));
        message.setPartialKey2(rs.getBytes("partial_key2"));
        message.setSignature(rs.getBytes("signature"));
        message.setTime(rs.getTimestamp("time"));
        return message;
    }

    public static UserEntry userFromResultSet(ResultSet rs) throws SQLException {
        UserEntry user = new UserEntry();
        user.setIdUser(rs.getInt("id_user"));
        user.setUsername(rs.getString("username"));
        user.setPublicKey(rs.getBytes("public_key"));
        user.setSignupTime(rs.getTimestamp("signup_time"));
        return user;
    }
}