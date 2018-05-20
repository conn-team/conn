package com.github.connteam.conn.client.database.provider;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.ClientUtil;
import com.github.connteam.conn.client.database.model.EphemeralKey;
import com.github.connteam.conn.client.database.model.Message;
import com.github.connteam.conn.client.database.model.Settings;
import com.github.connteam.conn.client.database.model.UsedEphemeralKey;
import com.github.connteam.conn.client.database.model.User;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.client.database.provider.SqliteDataProvider;
import com.github.connteam.conn.core.database.DatabaseException;

import org.junit.Test;

public class SqliteDataProviderTest {
    private DataProvider dp;

    private List<User> users = new ArrayList<>();
    private int maxIdUser = -1;

    private List<EphemeralKey> keys = new ArrayList<>();
    private int maxIdKey = -1;

    private List<UsedEphemeralKey> usedKeys = new ArrayList<>();

    private HashMap<Integer, List<Message>> messagesFrom = new HashMap<>();
    private HashMap<Integer, List<Message>> messagesTo = new HashMap<>();
    private int maxIdMessage = -1;

    private void insertMessage(@NotNull Message message) {
        if (message == null) {
            throw new NullPointerException();
        }

        maxIdMessage = Math.max(maxIdMessage, message.getIdMessage());

        HashMap<Integer, List<Message>> messages = message.isOutgoing() ? messagesTo : messagesFrom;
        if (messages.get(message.getIdUser()) == null) {
            messages.put(message.getIdUser(), new ArrayList<>());
        }

        messages.get(message.getIdUser()).add(message);
    }

    private List<Message> getMessages(HashMap<Integer, List<Message>> map, int userId) {
        if (map.get(userId) == null) {
            map.put(userId, new ArrayList<>());
        }
        return map.get(userId);
    }

    private List<Message> getMessagesFrom(int userId) {
        return getMessages(messagesFrom, userId);
    }

    private List<Message> getMessagesTo(int userId) {
        return getMessages(messagesTo, userId);
    }

    private int deleteMessages(HashMap<Integer, List<Message>> map, int userId) {
        if (map.get(userId) == null) {
            return 0;
        }

        int len = map.get(userId).size();
        map.remove(userId);
        return len;
    }

    private int deleteMessagesFrom(int userId) {
        return deleteMessages(messagesFrom, userId);
    }

    private int deleteMessagesTo(int userId) {
        return deleteMessages(messagesTo, userId);
    }

    private boolean deleteMessage(int idMessage) {
        for (Integer key : messagesFrom.keySet()) {
            if (messagesFrom.get(key).removeIf(x -> x.getIdMessage() == idMessage)) {
                return true;
            }
        }

        for (Integer key : messagesTo.keySet()) {
            if (messagesTo.get(key).removeIf(x -> x.getIdMessage() == idMessage)) {
                return true;
            }
        }

        return false;
    }

    private User getUser(int no, boolean friend, boolean verified) {
        User u = new User();
        u.setUsername("u" + no);
        u.setPublicKey(new byte[] { (byte) no });
        u.setInSequence(no);
        u.setOutSequence(no);
        u.isFriend(friend);
        u.setVerified(verified);
        return u;
    }

    private int compareBytes(@NotNull byte[] lhs, @NotNull byte[] rhs) {
        if (lhs == null || rhs == null) {
            throw new NullPointerException();
        }

        for (int i = 0; i < lhs.length && i < rhs.length; ++i) {
            byte lhsByte = lhs[i];
            byte rhsByte = rhs[i];

            if (lhsByte != rhsByte) {
                return lhsByte - rhsByte;
            }
        }

        return lhs.length - rhs.length;
    }

    public SqliteDataProviderTest() throws Exception {
        dp = new SqliteDataProvider("/tmp/conn_test.db");
        dp.dropTables();
        dp.createTables();

        // generate some users

        for (int i = 0; i < 5; ++i) {
            User u = getUser(i, i < 2, i < 3);

            u.setId(dp.insertUser(u));
            maxIdUser = Math.max(maxIdUser, u.getId());
            users.add(u);
        }

        users.sort((x, y) -> x.getId() - y.getId());

        // generate ephemeral keys

        for (int i = 0; i < 100; ++i) {
            EphemeralKey key = ClientUtil.generateEphemeralKey();
            key.setId(dp.insertEphemeralKey(key));
            maxIdKey = Math.max(maxIdKey, key.getId());
            keys.add(key);
        }

        keys.sort((x, y) -> x.getId() - y.getId());

        // add half keys to used ephemeral keys

        for (int i = 0; i < 50; ++i) {
            UsedEphemeralKey usedKey = new UsedEphemeralKey();
            usedKey.setKey(keys.get(i).getRawPublicKey());
            dp.insertUsedEphemeralKey(usedKey);
            usedKeys.add(usedKey);
        }

        usedKeys.sort((x, y) -> compareBytes(x.getRawKey(), y.getRawKey()));

        // generate messages

        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 2; ++j) {
                // message from

                if (i < 2 || i == 4) {
                    Message messageFrom = new Message();
                    messageFrom.setOutgoing(false);
                    messageFrom.setIdUser(i);
                    messageFrom.setMessage("from" + i + j);
                    messageFrom.setTime(new Timestamp(new Date().getTime()));
                    messageFrom.setIdMessage(dp.insertMessage(messageFrom));
                    insertMessage(messageFrom);
                }

                // message to

                if (i >= 2) {
                    Message messageTo = new Message();
                    messageTo.setOutgoing(true);
                    messageTo.setIdUser(i);
                    messageTo.setMessage("to" + i + j);
                    messageTo.setTime(new Timestamp(new Date().getTime()));
                    messageTo.setIdMessage(dp.insertMessage(messageTo));
                    insertMessage(messageTo);
                }
            }
        }
    }

    @Test
    public void ephemeralKeyTest() throws Exception {
        Optional<EphemeralKey> dpKey;
        Comparator<EphemeralKey> comparator = (x, y) -> x.getId() - y.getId();

        // getEphemeralKeys()

        List<EphemeralKey> dpKeys = dp.getEphemeralKeys();
        dpKeys.sort(comparator);

        assertEquals(keys, dpKeys);

        // getEphemeralKey(int id)

        for (EphemeralKey key : keys) {
            dpKey = dp.getEphemeralKey(key.getId());
            assertTrue(dpKey.isPresent());
            assertEquals(key, dpKey.get());
        }

        dpKey = dp.getEphemeralKey(maxIdKey + 1);
        assertFalse(dpKey.isPresent());

        // getEphemeralKeyByPublicKey

        for (EphemeralKey key : keys) {
            dpKey = dp.getEphemeralKeyByPublicKey(key.getRawPublicKey());
            assertTrue(dpKey.isPresent());
            assertEquals(key, dpKey.get());
        }

        List<byte[]> publicKeys = users.stream().map(x -> x.getRawPublicKey()).collect(Collectors.toList());
        EphemeralKey key;

        do {
            key = ClientUtil.generateEphemeralKey();
        } while (publicKeys.contains(key.getRawPublicKey()));

        dpKey = dp.getEphemeralKeyByPublicKey(key.getRawPublicKey());
        assertFalse(dpKey.isPresent());

        try {
            dp.getEphemeralKeyByPublicKey(null);
            fail();
        } catch (NullPointerException e) {
        }

        // insertEphemeralKey
        try {
            dp.insertEphemeralKey(keys.get(0));
            fail();
        } catch (DatabaseException e) {
        }

        try {
            dp.insertEphemeralKey(null);
            fail();
        } catch (NullPointerException e) {
        }

        // deleteEphemeralKey

        key = keys.get(0);
        assertTrue(dp.deleteEphemeralKey(key.getId()));
        assertFalse(dp.deleteEphemeralKey(key.getId()));

        dpKeys = dp.getEphemeralKeys();
        for (EphemeralKey k : keys) {
            assertEquals(!k.equals(key), dpKeys.contains(k));
        }
    }

    @Test
    public void usedEphemeralKeyTest() throws Exception {

        // getUsedEphemeralKeys()
        List<UsedEphemeralKey> dpUsedKeys = dp.getUsedEphemeralKeys();
        dpUsedKeys.sort((x, y) -> compareBytes(x.getRawKey(), y.getRawKey()));
        assertEquals(usedKeys, dpUsedKeys);

        // isUsedEphemeralKey
        for (int i = 0; i < 100; ++i) {
            UsedEphemeralKey usedKey = new UsedEphemeralKey();
            usedKey.setKey(keys.get(i).getRawPublicKey());

            assertEquals(i < 50, dp.isUsedEphemeralKey(usedKey));
        }

        try {
            dp.isUsedEphemeralKey(null);
            fail();
        } catch (NullPointerException e) {
        }

        // insertUsedEphemeralKey

        // ktoś uważa ze to dobry pomysł aby pozwalać na kolizje...
        // for (int i = 0; i < 50; ++i) {
        // try {
        // UsedEphemeralKey usedKey = new UsedEphemeralKey();
        // usedKey.setKey(keys.get(i).getRawPublicKey());
        // dp.insertUsedEphemeralKey(usedKey);
        // fail();
        // } catch (DatabaseException e) {
        // }
        // }

        try {
            dp.insertUsedEphemeralKey(null);
            fail();
        } catch (NullPointerException e) {
        }

        // deleteUsedEphemeralKey
        for (int i = 25; i < 100; ++i) {
            UsedEphemeralKey usedKey = new UsedEphemeralKey();
            usedKey.setKey(keys.get(i).getRawPublicKey());
            assertEquals(i < 50, dp.deleteUsedEphemeralKey(usedKey));
        }

        try {
            dp.deleteUsedEphemeralKey(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void userTest() throws Exception {
        Optional<User> dpUser;
        Comparator<User> comparator = (x, y) -> x.getId() - y.getId();

        // getUsers

        List<User> dpUsers = dp.getUsers();
        dpUsers.sort(comparator);

        assertEquals(users, dpUsers);

        // getUser

        for (User u : users) {
            dpUser = dp.getUser(u.getId());
            assertTrue(dpUser.isPresent());
            assertEquals(u, dpUser.get());
        }

        dpUser = dp.getUser(maxIdUser + 1);
        assertFalse(dpUser.isPresent());

        // getUserByUsername

        for (User u : users) {
            dpUser = dp.getUserByUsername(u.getUsername().toUpperCase()); // should be case insensitive
            assertTrue(dpUser.isPresent());
            assertEquals(u, dpUser.get());
        }

        dpUser = dp.getUserByUsername("u5");
        assertFalse(dpUser.isPresent());

        try {
            dp.getUserByUsername(null);
            fail();
        } catch (NullPointerException e) {
        }

        // getVerifiedUsers

        List<User> verfiedUsers = users.stream().filter(x -> x.isVerified()).sorted((x, y) -> x.getId() - y.getId())
                .collect(Collectors.toList());
        List<User> dpVerifiedUsers = dp.getVerifiedUsers();
        dpVerifiedUsers.sort(comparator);
        assertEquals(verfiedUsers, dpVerifiedUsers);

        // getFriends
        List<User> friends = users.stream().filter(x -> x.isFriend()).sorted((x, y) -> x.getId() - y.getId())
                .collect(Collectors.toList());
        List<User> dpFriends = dp.getFriends();
        dpFriends.sort(comparator);
        assertEquals(friends, dpFriends);

        // insertUser

        User u = getUser(5, false, true);
        u.setId(dp.insertUser(u));

        Optional<User> dpU = dp.getUser(u.getId());
        assertTrue(dpU.isPresent());
        assertEquals(u, dpU.get());

        try {
            u.setUsername(u.getUsername().toUpperCase());
            dp.insertUser(u);
            fail("username should be case insensitive");
        } catch (DatabaseException e) {
        }

        try {
            dp.insertUser(null);
            fail();
        } catch (NullPointerException e) {
        }

        // deleteUser

        assertTrue(dp.deleteUser(u.getId()));
        dpUsers = dp.getUsers();
        dpUsers.sort(comparator);
        assertEquals(users, dpUsers);

        assertFalse(dp.deleteUser(u.getId()));
        dpUsers = dp.getUsers();
        dpUsers.sort(comparator);
        assertEquals(users, dpUsers);

        // deleteUserByUsername

        u.setId(dp.insertUser(u));

        assertTrue(dp.deleteUserByUsername(u.getUsername())); // should be case insensitive
        dpUsers = dp.getUsers();
        dpUsers.sort(comparator);
        assertEquals(users, dpUsers);

        assertFalse(dp.deleteUserByUsername(u.getUsername()));
        dpUsers = dp.getUsers();
        dpUsers.sort(comparator);
        assertEquals(users, dpUsers);

        try {
            dp.deleteUserByUsername(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void settingsTest() throws Exception {
        // setSettings, getSettings

        Settings settings = new Settings();
        settings.setPrivateKey(new byte[] { 0 });
        settings.setPublicKey(new byte[] { 1 });
        settings.setUsername("krzys jest madry inaczej");

        dp.setSettings(settings);
        Settings dpSettings = dp.getSettings().get();
        assertEquals(settings, dpSettings);

        try {
            dp.setSettings(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getMessagesFromTest() throws Exception {
        Comparator<Message> comparator = (x, y) -> x.getIdMessage() - y.getIdMessage();

        // getMessageFrom

        for (int i = 0; i < users.size(); ++i) {
            User u = users.get(i);
            List<Message> dpMessagesFrom = dp.getMessagesFrom(u.getId());
            List<Message> messagesFrom = getMessagesFrom(u.getId());

            dpMessagesFrom.sort(comparator);
            messagesFrom.sort(comparator);

            assertEquals(messagesFrom, dpMessagesFrom);
        }

    }

    @Test
    public void getMessagesToTest() throws Exception {
        Comparator<Message> comparator = (x, y) -> x.getIdMessage() - y.getIdMessage();

        // getMessageTo

        for (int i = 0; i < users.size(); ++i) {
            User u = users.get(i);
            List<Message> dpMessagesTo = dp.getMessagesTo(u.getId());
            List<Message> messagesTo = getMessagesTo(u.getId());

            dpMessagesTo.sort(comparator);
            messagesTo.sort(comparator);

            assertEquals(messagesTo, dpMessagesTo);
        }
    }

    @Test
    public void messageTest() throws Exception {
        // getMessage

        List<Message> messages = Stream.concat(messagesFrom.values().stream(), messagesTo.values().stream())
                .flatMap(List::stream).collect(Collectors.toList());

        for (Message message : messages) {
            Optional<Message> dpMessage = dp.getMessage(message.getIdMessage());
            assertTrue(dpMessage.isPresent());
            assertEquals(message, dpMessage.get());
        }

        assertFalse(dp.getMessage(maxIdMessage + 1).isPresent());

        // insertMessage
        try {
            dp.insertMessage(null);
            fail();
        } catch (NullPointerException e) {
        }

        // updateMessage

        Message toUpdate = messages.get(0);
        toUpdate.setMessage("updated message");
        toUpdate.setTime(new Timestamp(new Date().getTime()));
        assertTrue(dp.updateMessage(toUpdate));

        Optional<Message> dpUpdated = dp.getMessage(toUpdate.getIdMessage());
        assertTrue(dpUpdated.isPresent());
        assertEquals(toUpdate, dpUpdated.get());

        try {
            dp.updateMessage(null);
            fail();
        } catch (NullPointerException e) {
        }

        // deleteMessage
        assertTrue(dp.deleteMessage(toUpdate.getIdMessage()));
        assertFalse(dp.deleteMessage(toUpdate.getIdMessage()));
        assertFalse(dp.getMessage(toUpdate.getIdMessage()).isPresent());
        assertTrue(deleteMessage(toUpdate.getIdMessage()));
        assertFalse(deleteMessage(toUpdate.getIdMessage()));

        assertFalse(dp.deleteMessage(maxIdMessage + 1));

        // deleteMessagesFrom

        assertEquals(0, dp.deleteMessagesFrom(maxIdUser + 1));

        Set<Integer> messagesFromKeySet = new HashSet<>(messagesFrom.keySet());
        for (Integer key : messagesFromKeySet) {
            assertEquals(deleteMessagesFrom(key), dp.deleteMessagesFrom(key));
            getMessagesFromTest();
        }

        // deleteMessagesTo

        assertEquals(0, dp.deleteMessagesTo(maxIdUser + 1));

        Set<Integer> messagesToKeySet = new HashSet<>(messagesTo.keySet());
        for (Integer key : messagesToKeySet) {
            assertEquals(deleteMessagesTo(key), dp.deleteMessagesTo(key));
            getMessagesToTest();
        }
    }

    @Test
    public void allDeclaredPublicMethodsAreSynchronizedTest() throws Exception {
        String names = Arrays.stream(SqliteDataProvider.class.getDeclaredMethods())
                .filter(x -> Modifier.isPublic(x.getModifiers()) && !Modifier.isSynchronized(x.getModifiers()))
                .map(x -> x.getName()).collect(Collectors.joining(", "));
        if (names.length() != 0) {
            fail(names);
        }
    }
}
