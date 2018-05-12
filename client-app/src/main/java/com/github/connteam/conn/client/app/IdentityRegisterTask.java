package com.github.connteam.conn.client.app;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.IdentityFactory;
import com.github.connteam.conn.client.app.model.IdentityManager;
import com.github.connteam.conn.client.app.model.Session;
import com.github.connteam.conn.client.database.provider.DataProvider;
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
        } catch (Exception e) {
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(db);
            onFinish(e);
        }
    }

    private void onFinish(Exception err) {
        if (err == null) {
            File src = new File(tempPath);
            File dst = new File(finalPath);

            if (!src.exists()) {
                err = new IOException("No identity file generated (shouldn't happen here)");
            } else if (dst.exists()) {
                err = new IOException("Identity file already exists");
            } else if (!src.renameTo(dst)) {
                err = new IOException("Error renaming identity file");
            }
        } else {
            new File(tempPath).delete();
        }

        callback.accept(err);
    }

    private class Handler implements ConnClientListener {
        @Override
        public void onLogin(boolean hasBeenRegistered) {
            success = true;
            IOUtils.closeQuietly(client);
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
