package com.github.connteam.conn.client.app;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.connteam.conn.client.app.controllers.LoginViewController;
import com.github.connteam.conn.client.app.controllers.MainViewController;
import com.github.connteam.conn.client.app.controllers.RegisterViewController;
import com.github.connteam.conn.client.app.model.IdentityManager;
import com.github.connteam.conn.client.app.model.Session;
import com.github.connteam.conn.client.app.model.SessionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class App extends Application {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static final String CONFIG_DIR = System.getProperty("user.home") + "/.conn";
    public static final String TITLE = "Conn";

    private ScheduledExecutorService executor;
    private IdentityManager identities;
    private SessionManager sessionMgr;

    private Stage stage;
    private Parent mainView, loginView, registerView;

    public void asyncTask(Runnable task) {
        executor.execute(task);
    }

    public void asyncTaskLater(Runnable task, long delay, TimeUnit unit) {
        executor.schedule(task, delay, unit);
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

    private void updateCurrentView() {
        if (getSession() == null) {
            if (identities.getIdentities().isEmpty()) {
                setView(registerView);
            } else {
                setView(loginView);
            }
        } else {
            setView(mainView);
        }
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
        LOG.error("Reported error", err);
        reportError(String.valueOf(err));
    }

    private void createConfigDir() throws IOException {
        File dir = new File(CONFIG_DIR);
        dir.mkdirs();
        if (!dir.exists()) {
            throw new IOException("Cannot create .conn directory");
        }
    }

    public static Parent loadView(String resourceName, Object controller) throws IOException {
        URL resourceUrl = App.class.getClassLoader().getResource(resourceName);
        if (resourceUrl == null) {
            throw new MissingResourceException("Missing view", "", resourceName);
        }

        FXMLLoader loader = new FXMLLoader(resourceUrl);
        loader.setController(controller);
        return loader.load();
    }

    private void initModel() throws IOException {
        executor = Executors.newSingleThreadScheduledExecutor();
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

        sessionMgr.sessionProperty().addListener(x -> updateCurrentView());
        stage.focusedProperty().addListener(x -> identities.update());
        stage.setOnCloseRequest(x -> getSessionManager().disconnect());

        this.stage = stage;
        stage.setScene(new Scene(mainView));
        stage.setTitle(TITLE);
        stage.show();
        updateCurrentView();
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
