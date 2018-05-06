package com.github.connteam.conn.client.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent view = FXMLLoader.load(getClass().getClassLoader().getResource("views/LoginView.fxml"));
        primaryStage.setScene(new Scene(view));
        primaryStage.setTitle("Conn");
        primaryStage.show();
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("-cli")) {
            new CLI().start();
        } else {
            launch(App.class, args);
        }
    }
}
