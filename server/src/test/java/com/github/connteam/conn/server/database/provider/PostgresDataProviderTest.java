package com.github.connteam.conn.server.database.provider;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.server.database.model.MessageEntry;
import com.github.connteam.conn.server.database.model.ObservedEntry;
import com.github.connteam.conn.server.database.model.UserEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// TODO: expand ephemeralKey test(popEphemeralKey)
public class PostgresDataProviderTest {
    DataProvider db;
    List<UserEntry> users;

    @Before
    public void initDatabase() throws DatabaseException {
        db = new PostgresDataProvider.Builder().setName("conn_test").setUser("conn").setPassword("").build();
        db.dropTables();
        db.createTables();

        // insertUser

        users = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            UserEntry user = new UserEntry();
            user.setPublicKey(("public" + i).getBytes());
            user.setUsername("admin" + i);
            user.setSignupTime(new Timestamp(987 + i * 1024));
            user.setIdUser(db.insertUser(user));
            users.add(user);
        }
    }

    @After
    public void closeDatabase() throws DatabaseException {
        db.close();
    }

    @Test
    public void testEphemeralKeys() throws DatabaseException {
        List<EphemeralKeyEntry> keys = new ArrayList<>();

        // insertEphemeralKey

        for (int i = 0; i < users.size(); i++) {
            for (int j = 0; j <= i; j++) {
                EphemeralKeyEntry key = new EphemeralKeyEntry();
                key.setIdUser(i + 1);
                key.setKey(("key" + j).getBytes());
                key.setSignature(("sign" + j).getBytes());
                key.setIdKey(db.insertEphemeralKey(key));
                keys.add(key);
            }
        }

        // getEphemeralKey, getEphemeralKeysByUserId

        for (EphemeralKeyEntry key : keys) {
            assertEquals(key, db.getEphemeralKey(key.getIdKey()).get());
            assertTrue(db.getEphemeralKeysByUserId(key.getIdUser()).contains(key));
        }

        // countEphemeralKeysByUserId

        for (UserEntry user : users) {
            int n = db.countEphemeralKeysByUserId(user.getIdUser());
            assertEquals(keys.stream().filter(x -> x.getIdUser() == user.getIdUser()).count(), n);
        }

        // updateEphemeralKey

        for (int i = 0; i < keys.size(); i++) {
            EphemeralKeyEntry key = keys.get(i);

            key.setKey(("update key" + i).getBytes());
            key.setSignature(("update sign" + i).getBytes());

            assertNotEquals(key, db.getEphemeralKey(key.getIdKey()).get());
            assertTrue(db.updateEphemeralKey(key));
            assertEquals(key, db.getEphemeralKey(key.getIdKey()).get());
        }

        // deleteEphemeralKeysByUserId

        db.deleteEphemeralKeysByUserId(3);

        for (EphemeralKeyEntry key : keys) {
            if (key.getIdUser() == 3) {
                assertFalse(db.deleteEphemeralKey(key.getIdKey()));
                assertFalse(db.getEphemeralKey(key.getIdKey()).isPresent());
            }
        }

        // deleteEphemeralKey

        for (EphemeralKeyEntry key : keys) {
            if (key.getIdUser() == 3) {
                continue;
            }
            assertTrue(db.deleteEphemeralKey(key.getIdKey()));
            assertFalse(db.deleteEphemeralKey(key.getIdKey()));
            assertFalse(db.getEphemeralKey(key.getIdKey()).isPresent());
        }
    }

    @Test
    public void testMessages() throws DatabaseException {
        List<MessageEntry> messages = new ArrayList<>();

        // insertMessage

        for (int from = 0; from < users.size(); from++) {
            for (int to = 0; to < users.size(); to++) {
                for (int i = 0; i < to; i++) {
                    MessageEntry msg = new MessageEntry();
                    msg.setIdFrom(from + 1);
                    msg.setIdTo(to + 1);
                    msg.setMessage(("msg" + messages.size()).getBytes());
                    msg.setKey(("key" + messages.size()).getBytes());
                    msg.setSignature(("sign" + messages.size()).getBytes());
                    msg.setTime(new Timestamp(123 + i * 100));
                    msg.setIdMessage(db.insertMessage(msg));
                    messages.add(msg);
                }
            }
        }

        // getMessage, getMessagesFrom, getMessagesTo

        for (MessageEntry msg : messages) {
            assertEquals(msg, db.getMessage(msg.getIdMessage()).get());
            assertTrue(db.getMessagesFrom(msg.getIdFrom()).contains(msg));
            assertTrue(db.getMessagesTo(msg.getIdTo()).contains(msg));
        }

        // updateMessage

        for (int i = 0; i < messages.size(); i++) {
            MessageEntry msg = messages.get(i);

            msg.setMessage(("update msg" + messages.size()).getBytes());
            msg.setKey(("update key" + messages.size()).getBytes());
            msg.setSignature(("update sign" + messages.size()).getBytes());
            msg.setTime(new Timestamp(345 + i * 50));

            assertNotEquals(msg, db.getMessage(msg.getIdMessage()).get());
            assertTrue(db.updateMessage(msg));
            assertEquals(msg, db.getMessage(msg.getIdMessage()).get());
        }

        // deleteMessagesFrom

        db.deleteMessagesFrom(3);

        for (MessageEntry msg : messages) {
            if (msg.getIdFrom() == 3) {
                assertFalse(db.deleteEphemeralKey(msg.getIdMessage()));
                assertFalse(db.getEphemeralKey(msg.getIdMessage()).isPresent());
            }
        }

        // deleteMessagesTo

        db.deleteMessagesTo(2);

        for (MessageEntry msg : messages) {
            if (msg.getIdTo() == 2) {
                assertFalse(db.deleteEphemeralKey(msg.getIdMessage()));
                assertFalse(db.getEphemeralKey(msg.getIdMessage()).isPresent());
            }
        }

        // deleteMessage

        for (MessageEntry msg : messages) {
            if (msg.getIdFrom() == 3 || msg.getIdTo() == 2) {
                continue;
            }
            assertTrue(db.deleteMessage(msg.getIdMessage()));
            assertFalse(db.deleteMessage(msg.getIdMessage()));
            assertFalse(db.getEphemeralKey(msg.getIdMessage()).isPresent());
        }
    }

    @Test
    public void testObserved() throws DatabaseException {
        List<ObservedEntry> observed = new ArrayList<>();

        // insertObserved

        for (int from = 0; from < users.size(); from++) {
            for (int to = 0; to < users.size(); to++) {
                if (((from + to) % 2) == 0) {
                    ObservedEntry obs = new ObservedEntry();
                    obs.setIdObserver(from + 1);
                    obs.setIdObserved(to + 1);
                    db.insertObserved(obs);
                    observed.add(obs);
                }
            }
        }

        // Duplicate observed

        try {
            db.insertObserved(observed.get(0));
            fail();
        } catch (DatabaseException e) {
        }

        // getObserved, getObservers

        for (int i = 0; i < users.size(); i++) {
            List<ObservedEntry> other = db.getObserved(i);
            for (ObservedEntry x : observed) {
                if (x.getIdObserver() == i) {
                    assertTrue(other.contains(x));
                }
            }

            other = db.getObservers(i);
            for (ObservedEntry x : observed) {
                if (x.getIdObserved() == i) {
                    assertTrue(other.contains(x));
                }
            }
        }

        // deleteObserved

        for (ObservedEntry obs : observed) {
            assertTrue(db.deleteObserved(obs));
            assertFalse(db.deleteObserved(obs));
        }
    }

    @Test
    public void testUsers() throws DatabaseException {
        // Duplicate users

        try {
            db.insertUser(users.get(0));
            fail();
        } catch (DatabaseException e) {
        }

        // getUser, getUserByUsername

        for (UserEntry user : users) {
            assertEquals(user, db.getUser(user.getIdUser()).get());
            assertEquals(user, db.getUserByUsername(user.getUsername()).get());
        }

        // updateUser

        for (int i = 0; i < users.size(); i++) {
            UserEntry user = users.get(i);

            user.setPublicKey(("update public" + i).getBytes());
            user.setUsername("update admin" + i);
            user.setSignupTime(new Timestamp(600 + i * 49));

            assertNotEquals(user, db.getUser(user.getIdUser()).get());
            assertTrue(db.updateUser(user));
            assertEquals(user, db.getUser(user.getIdUser()).get());
        }

        // updateUserByUsername <- is this thing even necessary?

        for (int i = 0; i < users.size(); i++) {
            UserEntry user = users.get(i);

            user.setPublicKey(("update2 public" + i).getBytes());
            user.setSignupTime(new Timestamp(789 + i * 90));

            assertNotEquals(user, db.getUser(user.getIdUser()).get());
            assertTrue(db.updateUserByUsername(user));
            assertEquals(user, db.getUser(user.getIdUser()).get());
        }

        // deleteUser, deleteUserByUsername

        for (int i = 0; i < users.size(); i++) {
            UserEntry user = users.get(i);

            if ((i % 2) == 0) {
                assertTrue(db.getUserByUsername(user.getUsername()).isPresent());
                assertTrue(db.deleteUser(user.getIdUser()));
                assertFalse(db.deleteUser(user.getIdUser()));
                assertFalse(db.getUserByUsername(user.getUsername()).isPresent());
            } else {
                assertTrue(db.getUser(user.getIdUser()).isPresent());
                assertTrue(db.deleteUserByUsername(user.getUsername()));
                assertFalse(db.deleteUserByUsername(user.getUsername()));
                assertFalse(db.getUser(user.getIdUser()).isPresent());
            }
        }
    }
}
