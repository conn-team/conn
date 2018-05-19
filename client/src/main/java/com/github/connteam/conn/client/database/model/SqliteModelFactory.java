package com.github.connteam.conn.client.database.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteModelFactory {
    public static EphemeralKey ephemeralKeyFromResultSet(ResultSet rs) throws SQLException {
        EphemeralKey key = new EphemeralKey();
        key.setId(rs.getInt("id_key"));
        key.setPublicKey(rs.getBytes("public_key"));
        key.setPrivateKey(rs.getBytes("private_key"));
        return key;
    }

    public static Message messageFromResultSet(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setIdMessage(rs.getInt("id_message"));
        message.setIdUser(rs.getInt("id_user"));
        message.setMessage(rs.getString("message"));
        message.setOutgoing(rs.getBoolean("is_outgoing"));
        message.setTime(rs.getTimestamp("time"));
        return message;
    }

    public static Settings settingsFromResultSet(ResultSet rs) throws SQLException {
        Settings settings = new Settings();
        settings.setUsername(rs.getString("username"));
        settings.setPrivateKey(rs.getBytes("private_key"));
        settings.setPublicKey(rs.getBytes("public_key"));
        return settings;
    }

    public static User userFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id_user"));
        user.setUsername(rs.getString("username"));
        user.setPublicKey(rs.getBytes("public_key"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setOutSequence(rs.getInt("out_sequence"));
        user.setInSequence(rs.getInt("in_sequence"));
        user.isFriend(rs.getBoolean("is_friend"));
        return user;
    }
}