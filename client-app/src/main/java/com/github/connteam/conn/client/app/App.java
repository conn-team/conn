package com.github.connteam.conn.client.app;

import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;

import com.github.connteam.conn.client.app.controllers.LoginViewController;
import com.github.connteam.conn.client.app.controllers.RegisterViewController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class App extends Application {
    private Stage stage;
    private Parent loginView, registerView;

    private Parent loadScene(String resourceName, Object controller) throws IOException {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        if (resourceUrl == null) {
            throw new MissingResourceException("Missing view", "", resourceName);
        }

        FXMLLoader loader = new FXMLLoader(resourceUrl);
        loader.setController(controller);
        return loader.load();
    }

    @Override
    public void start(Stage stage) throws Exception {
        loginView = loadScene("views/LoginView.fxml", new LoginViewController(this));
        registerView = loadScene("views/RegisterView.fxml", new RegisterViewController(this));

        this.stage = stage;
        stage.setScene(new Scene(loginView));
        stage.setTitle("Conn");
        stage.show();
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
}
