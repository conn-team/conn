package com.github.connteam.conn.client.app;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.connteam.conn.client.app.controllers.LoginViewController;
import com.github.connteam.conn.client.app.controllers.MainViewController;
import com.github.connteam.conn.client.app.controllers.RegisterViewController;
import com.github.connteam.conn.client.app.model.IdentityManager;
import com.github.connteam.conn.client.app.model.Session;
import com.github.connteam.conn.client.app.model.SessionManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class App extends Application {
    public static final String CONFIG_DIR = System.getProperty("user.home") + "/.conn";
    public static final String TITLE = "Conn";

    private ExecutorService executor;
    private IdentityManager identities;
    private SessionManager sessionMgr;

    private Stage stage;
    private Parent mainView, loginView, registerView;

    public void asyncTask(Runnable task) {
        executor.execute(task);
    }

    public IdentityManager getIdentityManager() {
        return identities;
    }

    public SessionManager getSessionManager() {
        return sessionMgr;
    }

    public Session getSession() {
        return sessionMgr.getSession();
    }

    public Stage getStage() {
        return stage;
    }

    public void setView(Parent view) {
        // Switching scenes is causing flickering, so we switch scene's root instead
        stage.getScene().setRoot(view);
    }

    public Parent getMainView() {
        return mainView;
    }

    public Parent getLoginView() {
        return loginView;
    }

    public Parent getRegisterView() {
        return registerView;
    }

    public void reportError(String err) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Conn");
        alert.setHeaderText("Błąd");
        alert.setContentText(err);
        alert.initOwner(stage.getScene().getWindow());
        alert.showAndWait();
    }

    public void reportError(Exception err) {
        reportError(String.valueOf(err));
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
        sessionMgr = new SessionManager(this);
    }

    private void initViews() throws IOException {
        mainView = loadView("views/MainView.fxml", new MainViewController(this));
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

        stage.setOnCloseRequest(e -> {
            stage.close();
        });

        this.stage = stage;
        stage.setScene(new Scene(mainView));
        stage.setTitle(TITLE);
        stage.show();
        sessionMgr.setSession(null);
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
