package krypto;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent mainroot =
                FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/menu.fxml")));
        Scene mainscene = new Scene(mainroot, 720, 480);
        primaryStage.setTitle("Krypto");
        primaryStage.setScene(mainscene);
        primaryStage.sizeToScene();
        primaryStage.show();
    }

    public static void main(String[] args) {
        System.setProperty("javafx.App", App.class.getName());
        launch(args);
    }
}
