package com.github.connteam.conn.client.app;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.connteam.conn.client.app.controllers.LoginViewController;
import com.github.connteam.conn.client.app.controllers.RegisterViewController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class App extends Application {
    public static final String CONFIG_DIR = System.getProperty("user.home") + "/.conn";
    public static final String TITLE = "Conn";

    private ExecutorService executor;
    private IdentityManager identities;
    private Session session;

    private Stage stage;
    private Parent loginView, registerView;

    public void asyncTask(Runnable task) {
        executor.execute(task);
    }

    public IdentityManager getIdentityManager() {
        return identities;
    }

    public Session getSession() {
        return session;
    }

    public Stage getStage() {
        return stage;
    }

    public void setView(Parent view) {
        // Switching scenes is causing flickering, so we switch scene's root instead
        stage.getScene().setRoot(view);
    }

    public Parent getLoginView() {
        return loginView;
    }

    public Parent getRegisterView() {
        return registerView;
    }

    private Parent loadView(String resourceName, Object controller) throws IOException {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        if (resourceUrl == null) {
            throw new MissingResourceException("Missing view", "", resourceName);
        }

        FXMLLoader loader = new FXMLLoader(resourceUrl);
        loader.setController(controller);
        return loader.load();
    }

    private void createConfigDir() throws IOException {
        File dir = new File(CONFIG_DIR);
        dir.mkdirs();
        if (!dir.exists()) {
            throw new IOException("Cannot create .conn directory");
        }
    }

    private void initModel() throws IOException {
        executor = Executors.newSingleThreadExecutor();
        createConfigDir();

        identities = new IdentityManager(this);
        session = new Session(this);
    }

    private void initViews() throws IOException {
        loginView = loadView("views/LoginView.fxml", new LoginViewController(this));
        registerView = loadView("views/RegisterView.fxml", new RegisterViewController(this));
    }

    @Override
    public void start(Stage stage) throws Exception {
        initModel();
        initViews();

        stage.focusedProperty().addListener(x -> {
            identities.update();
        });

        this.stage = stage;
        stage.setScene(new Scene(identities.getIdentities().isEmpty() ? registerView : loginView));
        stage.setTitle(TITLE);
        stage.show();
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
