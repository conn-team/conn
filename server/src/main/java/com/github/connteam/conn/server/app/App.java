package com.github.connteam.conn.server.app;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.ConnServer;
import com.github.connteam.conn.server.database.model.EphemeralKey;
import com.github.connteam.conn.server.database.model.Message;
import com.github.connteam.conn.server.database.model.Observed;
import com.github.connteam.conn.server.database.model.User;
import com.github.connteam.conn.server.database.provider.DataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        final User user = new User();

        user.setId(123);
        user.setUsername("teapot");
        user.setPublicKey(Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE0+P3PrmpY+o605eYFdh1xD/TzL5QBf862D4CHejNidTPbeLcxGFq4BEjDxOFhtB6WtOYa57B24F2XU/wb9z6Q=="));
        user.setSignupTime(new Timestamp(System.currentTimeMillis()));

        DataProvider provider = new DataProvider() {
            @Override
            public Optional<EphemeralKey> getEphemeralKey(int keyId) throws DatabaseException {
                return null;
            }

            @Override
            public List<EphemeralKey> getEphemeralKeyByUserId(int userId) throws DatabaseException {
                return null;
            }

            @Override
            public int insertEphemeralKey(EphemeralKey key) throws DatabaseException {
                return 0;
            }

            @Override
            public boolean updateEphemeralKey(EphemeralKey key) throws DatabaseException {
                return false;
            }

            @Override
            public boolean deleteEphemeralKey(int keyId) throws DatabaseException {
                return false;
            }

            @Override
            public int deleteEphemeralKeyByUserId(int userId) throws DatabaseException {
                return 0;
            }

            @Override
            public Optional<Message> getMessage(int idMessage) throws DatabaseException {
                return null;
            }

            @Override
            public List<Message> getMessagesFrom(int idFrom) throws DatabaseException {
                return null;
            }

            @Override
            public List<Message> getMessagesTo(int idTo) throws DatabaseException {
                return null;
            }

            @Override
            public int insertMessage(Message message) throws DatabaseException {
                return 0;
            }

            @Override
            public boolean updateMessage(Message message) throws DatabaseException {
                return false;
            }

            @Override
            public boolean deleteMessage(int idMessage) throws DatabaseException {
                return false;
            }

            @Override
            public int deleteMessagesFrom(int idFrom) throws DatabaseException {
                return 0;
            }

            @Override
            public int deleteMessagesTo(int idTo) throws DatabaseException {
                return 0;
            }

            @Override
            public List<Observed> getObserved(int idObserver) throws DatabaseException {
                return null;
            }

            @Override
            public List<Observed> getObservers(int idObserved) throws DatabaseException {
                return null;
            }

            @Override
            public boolean insertObserved(Observed observed) throws DatabaseException {
                return false;
            }

            @Override
            public boolean deleteObserved(Observed observed) throws DatabaseException {
                return false;
            }

            @Override
            public Optional<User> getUser(int id) throws DatabaseException {
                return Optional.ofNullable(user.getId() == id ? user : null);
            }

            @Override
            public Optional<User> getUserByUsername(String username) throws DatabaseException {
                return Optional.ofNullable(user.getUsername().equals(username) ? user : null);
            }

            @Override
            public int insertUser(User user) throws DatabaseException {
                return 0;
            }

            @Override
            public boolean updateUser(User user) throws DatabaseException {
                return false;
            }

            @Override
            public boolean updateUserByUsername(User username) throws DatabaseException {
                return false;
            }

            @Override
            public boolean deleteUser(int id) throws DatabaseException {
                return false;
            }

            @Override
            public boolean deleteUserByUsername(String username) throws DatabaseException {
                return false;
            }

            @Override
            public void close() throws Exception {
            }
        };

        try (ConnServer server = ConnServer.builder().setPort(9090).setTransport(Transport.SSL)
                .setDataProvider(provider).build()) {
            LOG.info("Listening");
            server.listen();
        }
    }
}
