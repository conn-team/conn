package com.github.connteam.conn.client.database.provider;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.github.connteam.conn.client.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.client.database.provider.SqliteDataProvider;
import com.github.connteam.conn.core.database.DatabaseException;

import org.junit.Before;
import org.junit.Test;

// TODO: expand user test (friends)
// TODO: add UsedEphemeralKey test
// TODO: better ephemeralKey test
public class SqliteDataProviderTest {
    private DataProvider dp = null;
    private UserEntry[] users = new UserEntry[3];

    @FunctionalInterface
    private static interface DatabaseExecutor {
        void execute() throws DatabaseException;
    }

    private static void wrapper(DatabaseExecutor executor) {
        try {
            executor.execute();
        } catch (DatabaseException e) {
            fail(e.getMessage());
        }
    }

    @Before
    public void initDatabase() {
        wrapper(() -> {
            dp = new SqliteDataProvider("/tmp/conn_test.db");
            dp.dropTables();
            dp.createTables();
        });

        for (int i = 0; i < 3; ++i) {
            UserEntry u = new UserEntry();
            u.setUsername("u" + i);
            u.setInSequence(i);
            u.setOutSequence(i);
            u.setPublicKey(new byte[] { (byte) i });
            u.setVerified(i % 2 == 0);
            if (i != 2) {
                u.isFriend(true);
            }

            wrapper(() -> {
                u.setId(dp.insertUser(u));
            });

            users[i] = u;
        }
    }

    @Test
    public void ephemeralKeyTest() {
        // insertEphemeralKey
        EphemeralKeyEntry ek = new EphemeralKeyEntry();
        ek.setPublicKey(new byte[] { 0 });
        ek.setPrivateKey(new byte[] { 1 });

        EphemeralKeyEntry ek2 = new EphemeralKeyEntry();
        ek2.setPublicKey(new byte[] { 2 });
        ek2.setPrivateKey(new byte[] { 3 });

        EphemeralKeyEntry ek3 = new EphemeralKeyEntry();
        ek3.setPublicKey(new byte[] { 4 });
        ek3.setPrivateKey(new byte[] { 5 });

        wrapper(() -> {
            ek.setId(dp.insertEphemeralKey(ek));
            ek2.setId(dp.insertEphemeralKey(ek2));
            ek3.setId(dp.insertEphemeralKey(ek3));
        });

        /// getEphemeralKeys
        wrapper(() -> {
            Object[] dpEkeys = dp.getEphemeralKeys().stream().map(ekey -> ekey.getId()).sorted().toArray();
            Object[] localEkeys = Arrays.stream(new EphemeralKeyEntry[] { ek, ek2, ek3 }).map(ekey -> ekey.getId())
                    .sorted().toArray();
            assertTrue(Arrays.equals(dpEkeys, localEkeys));
        });

        // getEphemeralKey
        wrapper(() -> {
            EphemeralKeyEntry ekCpy = dp.getEphemeralKey(ek.getId()).get();
            assertTrue(ekCpy.getId() == ek.getId());
            assertTrue(Arrays.equals(ekCpy.getRawPublicKey(), ek.getRawPublicKey()));
            assertTrue(Arrays.equals(ekCpy.getRawPrivateKey(), ek.getRawPrivateKey()));
        });

        // deleteEphemeralKey
        wrapper(() -> {
            dp.deleteEphemeralKey(ek.getId());
            Object[] dpEkeys2 = dp.getEphemeralKeys().stream().map(ekey -> ekey.getId()).sorted().toArray();
            Object[] localEkeys2 = Arrays.stream(new EphemeralKeyEntry[] { ek2, ek3 }).map(ekey -> ekey.getId())
                    .sorted().toArray();
            assertTrue(Arrays.equals(dpEkeys2, localEkeys2));
        });
    }

    @Test
    public void messageTest() {
        MessageEntry msgOut = new MessageEntry();
        ArrayList<Integer> msgOutIds = new ArrayList<>();

        msgOut.setIdUser(users[0].getId());
        msgOut.setMessage("krzys jest madry inaczej");
        msgOut.setOutgoing(true);
        msgOut.setTime(new Timestamp(new Date().getTime()));

        MessageEntry msgIn = new MessageEntry();
        ArrayList<Integer> msgInIds = new ArrayList<>();

        msgIn.setIdUser(users[1].getId());
        msgIn.setMessage("krzys jest madry inaczej");
        msgIn.setOutgoing(false);
        msgIn.setTime(new Timestamp(new Date().getTime()));

        // insertMessage
        wrapper(() -> {
            for (int i = 0; i < 10; ++i) {
                msgOutIds.add(dp.insertMessage(msgOut));
                msgInIds.add(dp.insertMessage(msgIn));
            }
        });

        // getMessage
        wrapper(() -> {
            MessageEntry msgOut2 = dp.getMessage(msgOutIds.get(0)).get();
            assertTrue(msgOut2.getIdMessage() == msgOutIds.get(0));
            assertEquals(msgOut2.getIdUser(), users[0].getId());
            assertEquals(msgOut2.getMessage(), msgOut.getMessage());
            assertEquals(msgOut2.isOutgoing(), msgOut.isOutgoing());
        });

        // getMessageFrom
        wrapper(() -> {
            List<MessageEntry> msgsFrom = dp.getMessageFrom(users[0].getId());
            assertEquals(msgsFrom.size(), 0);

            List<MessageEntry> msgsFrom2 = dp.getMessageFrom(users[1].getId());
            assertEquals(msgsFrom2.size(), 10);
        });

        // getMessageTo
        wrapper(() -> {
            List<MessageEntry> msgsFrom = dp.getMessageTo(users[0].getId());
            assertEquals(msgsFrom.size(), 10);

            List<MessageEntry> msgsFrom2 = dp.getMessageTo(users[1].getId());
            assertEquals(msgsFrom2.size(), 0);
        });

        // deleteMessagesFrom
        wrapper(() -> {
            dp.deleteMessagesFrom(users[0].getId());
            List<MessageEntry> msgsFrom = dp.getMessageTo(users[0].getId());
            assertEquals(msgsFrom.size(), 10);
        });

        wrapper(() -> {
            dp.deleteMessagesFrom(users[1].getId());
            List<MessageEntry> msgsFrom2 = dp.getMessageFrom(users[1].getId());
            assertEquals(msgsFrom2.size(), 0);
        });

        wrapper(() -> {
            for (int i = 0; i < 10; ++i) {
                msgInIds.add(dp.insertMessage(msgIn));
            }
        });

        // deleteMessagesTo
        wrapper(() -> {
            dp.deleteMessagesTo(users[1].getId());
            List<MessageEntry> msgsFrom = dp.getMessageFrom(users[1].getId());
            assertEquals(msgsFrom.size(), 10);

            dp.deleteMessagesTo(users[0].getId());
            List<MessageEntry> msgsFrom2 = dp.getMessageFrom(users[0].getId());
            assertEquals(msgsFrom2.size(), 0);
        });
    }

    @Test
    public void settingsTest() {
        // setSettings, getSettings
        SettingsEntry newSettings = new SettingsEntry();
        newSettings.setPrivateKey(new byte[] { 0 });
        newSettings.setPublicKey(new byte[] { 1 });
        newSettings.setUsername("krzys jest madry inaczej");

        wrapper(() -> {
            dp.setSettings(newSettings);
            SettingsEntry settings2 = dp.getSettings().get();
            assertEquals(newSettings.getUsername(), settings2.getUsername());
            assertTrue(Arrays.equals(newSettings.getRawPublicKey(), settings2.getRawPublicKey()));
            assertTrue(Arrays.equals(newSettings.getRawPrivateKey(), settings2.getRawPrivateKey()));
        });
    }

    @Test
    public void userTest() {
        // getUser
        UserEntry u = users[0];
        wrapper(() -> {
            UserEntry u2 = dp.getUser(u.getId()).get();
            assertTrue(u.getId() == u2.getId());
            assertTrue(u.getUsername().equals(u2.getUsername()));
            assertTrue(Arrays.equals(u.getRawPublicKey(), u2.getRawPublicKey()));
            assertTrue(u.isVerified() == u2.isVerified());
            assertTrue(u.getInSequence() == u2.getInSequence());
            assertTrue(u.getOutSequence() == u2.getOutSequence());
        });

        // getUserByUsername
        wrapper(() -> {
            UserEntry u3 = dp.getUserByUsername(u.getUsername()).get();
            assertTrue(u.getId() == u3.getId());
            assertTrue(u.getUsername().equals(u3.getUsername()));
            assertTrue(Arrays.equals(u.getRawPublicKey(), u3.getRawPublicKey()));
            assertTrue(u.isVerified() == u3.isVerified());
            assertTrue(u.getInSequence() == u3.getInSequence());
            assertTrue(u.getOutSequence() == u3.getOutSequence());
        });

        // getUsers
        wrapper(() -> {
            Object[] dpUsrs = dp.getUsers().stream().map(usr -> usr.getId()).sorted().toArray();
            Object[] insertedUsrs = Arrays.stream(users).map(usr -> usr.getId()).sorted().toArray();
            assertTrue(Arrays.equals(dpUsrs, insertedUsrs));
        });

        // getVerifiedUsers
        wrapper(() -> {
            Object[] dpVerifiedUsrs = dp.getVerifiedUsers().stream().map(usr -> usr.getId()).sorted().toArray();
            Object[] insertedVerfiedUsrs = Arrays.stream(users).filter(usr -> usr.isVerified()).map(usr -> usr.getId())
                    .sorted().toArray();
            assertTrue(Arrays.equals(dpVerifiedUsrs, insertedVerfiedUsrs));
        });

        // deleteUser
        UserEntry u4 = new UserEntry();
        wrapper(() -> {
            u4.setUsername("u4");
            u4.setInSequence(4);
            u4.setOutSequence(4);
            u4.setPublicKey(new byte[] { (byte) 4 });
            u4.setVerified(false);
            dp.deleteUser(dp.insertUser(u4));
        });

        wrapper(() -> {
            List<UserEntry> ul = dp.getUsers();
            assertTrue(ul.size() == 3);
            assertTrue(ul.stream().filter(usr -> usr.getUsername().equals("u4")).toArray().length == 0);
        });

        // deleteUserByUsername
        UserEntry u5 = new UserEntry();
        u5.setUsername("u5");
        u5.setInSequence(5);
        u5.setOutSequence(5);
        u5.setPublicKey(new byte[] { (byte) 5 });
        u5.setVerified(false);

        wrapper(() -> {
            dp.deleteUser(dp.insertUser(u5));
            List<UserEntry> ul2 = dp.getUsers();
            assertTrue(ul2.size() == 3);
            assertTrue(ul2.stream().filter(usr -> usr.getUsername().equals("u5")).toArray().length == 0);
        });
    }
}
