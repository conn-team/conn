package com.github.connteam.conn.client.app;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.database.model.EphemeralKey;
import com.github.connteam.conn.client.database.model.Friend;
import com.github.connteam.conn.client.database.model.Message;
import com.github.connteam.conn.client.database.model.Settings;
import com.github.connteam.conn.client.database.model.User;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.net.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
		final Settings settings = new Settings();

		settings.setUsername("teapot");
		settings.setPublicKey(Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE0+P3PrmpY+o605eYFdh1xD/TzL5QBf862D4CHejNidTPbeLcxGFq4BEjDxOFhtB6WtOYa57B24F2XU/wb9z6Q=="));
		settings.setPrivateKey(Base64.getDecoder().decode("MEACAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJjAkAgEBBB9hQeQlV1JcELdUlk44FHY9zkPl92lG/PUdndyVlFpB"));

        DataProvider provider = new DataProvider() {
            @Override
            public Optional<Settings> getSettings() {
                return Optional.of(settings);
            }

			@Override
			public List<EphemeralKey> getEphemeralKeys() {
				return null;
			}

			@Override
			public Optional<EphemeralKey> getEphemeralKey(int id) {
				return null;
			}

			@Override
			public int insertEphemeralKey(EphemeralKey key) {
				return 0;
			}

			@Override
			public boolean deleteEphemeralKey(int id) {
				return false;
			}

			@Override
			public Optional<EphemeralKey> popEphemeralKey() {
				return null;
			}

			@Override
			public List<Friend> getFriends() {
				return null;
			}

			@Override
			public Optional<Friend> getFriendById(int id) {
				return null;
			}

			@Override
			public Optional<Integer> insertFriend(Friend friend) {
				return null;
			}

			@Override
			public boolean deleteFriend(int id) {
				return false;
			}

			@Override
			public List<Message> getMessageFrom(int idFrom) {
				return null;
			}

			@Override
			public List<Message> getMessageTo(int idFrom) {
				return null;
			}

			@Override
			public Optional<Message> getMessage(int idMessage) {
				return null;
			}

			@Override
			public int insertMessage(Message message) {
				return 0;
			}

			@Override
			public boolean updateMessage(Message message) {
				return false;
			}

			@Override
			public int deleteMessageFrom(int idFrom) {
				return 0;
			}

			@Override
			public int deleteMessagesTo(int idTo) {
				return 0;
			}

			@Override
			public boolean setSettings(Settings settings) {
				return false;
			}

			@Override
			public Optional<User> getUser(int id) {
				return null;
			}

			@Override
			public Optional<User> getUserByUsername(String username) {
				return null;
			}

			@Override
			public List<User> getUsers() {
				return null;
			}

			@Override
			public List<User> getVerifiedUsers() {
				return null;
			}

			@Override
			public int insertUser(User user) {
				return 0;
			}

			@Override
			public boolean deleteUser(int id) {
				return false;
			}

			@Override
			public boolean deleteUserByUsername(String username) {
				return false;
			}

			@Override
			public void close() throws Exception {	
			}
        };

        LOG.info("Connecting");
        ConnClient client = ConnClient.builder().setHost("localhost").setPort(9090).setTransport(Transport.SSL)
                .setDataProvider(provider).build();

        client.setHandler(new ConnClientListener() {
            @Override
            public void onLogin() {
                LOG.info("Logged in!");
            }

            @Override
            public void onDisconnect(IOException err) {
                LOG.info("Disconnected: {}", err);
            }
        });

        LOG.info("Authenticating");
        client.start();
    }
}
