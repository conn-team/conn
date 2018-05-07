package com.github.connteam.conn.client.app;

import java.io.File;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Consumer;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.IdentityFactory;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.io.IOUtils;

public class IdentityRegisterTask implements Runnable {
    private final String tempPath, finalPath;
    private final String username;
    private final Consumer<Exception> callback;

    private DataProvider db;
    private ConnClient client;
    private boolean success = false;

    public IdentityRegisterTask(String username, Consumer<Exception> callback) {
        this.tempPath = App.CONFIG_DIR + "/" + username + IdentityManager.EXTENSION_TEMP;
        this.finalPath = App.CONFIG_DIR + "/" + username + IdentityManager.EXTENSION;
        this.username = username;
        this.callback = callback;
    }

	@Override
	public void run() {
        try {
            db = IdentityFactory.create(tempPath, username);
            client = Session.createClient(db);
            client.setHandler(new Handler());
            client.start();
        } catch (DatabaseException | InvalidKeySpecException | IOException e) {
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(db);
            onFinish(e);
        }
    }

    private void onFinish(Exception err) {
        if (err == null) {
            new File(tempPath).renameTo(new File(finalPath));
        } else {
            new File(tempPath).delete();
        }
        callback.accept(err);
    }
    
    private class Handler implements ConnClientListener {
        @Override
        public void onLogin(boolean hasBeenRegistered) {
            success = true;
            client.close();
        }

        @Override
        public void onDisconnect(Exception err) {
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(db);

            if (success) {
                onFinish(null);
            } else {
                onFinish(err != null ? err : new Exception("Unknown error"));
            }
        }
    }
}
