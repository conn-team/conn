package com.github.connteam.conn.server.database.provider;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.EphemeralKey;
import com.github.connteam.conn.server.database.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PostgresDataProviderTest {
    DataProvider db;
    List<User> users;

    @Before
    public void initDatabase() throws DatabaseException {
        db = new PostgresDataProvider.Builder().setName("conn_test").setUser("conn").setPassword("").build();
        db.dropTables();
        db.createTables();

        // insertUser

        users = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            User user = new User();
            user.setPublicKey(("public" + i).getBytes());
            user.setUsername("admin" + i);
            db.insertUser(user);
            users.add(user);
        }
    }
    
    @After
    public void closeDatabase() throws DatabaseException {
        db.close();
    }

    @Test
    public void testEphemeralKeys() throws DatabaseException {
        List<EphemeralKey> keys = new ArrayList<>();

        // insertEphemeralKey

        for (int i = 0; i < users.size(); i++) {
            for (int j = 0; j < i; j++) {
                EphemeralKey key = new EphemeralKey();
                key.setIdUser(i);
                key.setKey(("key" + j).getBytes());
                key.setSignature(("sign" + j).getBytes());
                db.insertEphemeralKey(key);
            }
        }
    }
}
