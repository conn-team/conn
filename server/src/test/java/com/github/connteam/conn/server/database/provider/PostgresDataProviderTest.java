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

        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setPublicKey(("public" + i).getBytes());
            user.setUsername("admin" + i);
            user.setId(db.insertUser(user));
            users.add(user);
        }
    }
    
    @After
    public void closeDatabase() throws DatabaseException {
        db.close();
    }

    private void checkEphemeralKeys(EphemeralKey key, EphemeralKey other) {
        assertEquals(key.getIdKey(), other.getIdKey());
        assertArrayEquals(key.getRawKey(), other.getRawKey());
        assertArrayEquals(key.getSignature(), other.getSignature());
        assertEquals(key.getIdUser(), other.getIdUser());
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
                key.setIdKey(db.insertEphemeralKey(key));
                keys.add(key);
            }
        }

        // getEphemeralKey, getEphemeralKeyByUserId

        for (EphemeralKey key : keys) {
            checkEphemeralKeys(key, db.getEphemeralKey(key.getIdKey()).get());
            db.getEphemeralKeyByUserId(key.getIdUser()).stream().filter(x -> x.getIdKey() == key.getIdKey())
                    .forEach(x -> checkEphemeralKeys(key, x));
        }

        // updateEphemeralKey

        for (int i = 0; i < keys.size(); i++) {
            EphemeralKey key = keys.get(i);
            
            key.setKey(("update key" + i).getBytes());
            key.setSignature(("update sign" + i).getBytes());

            assertTrue(db.updateEphemeralKey(key));
            checkEphemeralKeys(key, db.getEphemeralKey(key.getIdKey()).get());
        }

        // deleteEphemeralKeyByUserId

        db.deleteEphemeralKeyByUserId(3);

        for (EphemeralKey key : keys) {
            if (key.getIdUser() == 3) {
                assertFalse(db.deleteEphemeralKey(key.getIdKey()));
                assertFalse(db.getEphemeralKey(key.getIdKey()).isPresent());
            }
        }

        // deleteEphemeralKey

        for (EphemeralKey key : keys) {
            if (key.getIdUser() == 3) {
                continue;
            }
            assertTrue(db.deleteEphemeralKey(key.getIdKey()));
            assertFalse(db.deleteEphemeralKey(key.getIdKey()));
            assertFalse(db.getEphemeralKey(key.getIdKey()).isPresent());
        }
    }
}
