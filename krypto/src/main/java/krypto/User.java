package krypto;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;
import javafx.fxml.FXMLLoader;

public abstract class User extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent mainroot =
                FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/menu.fxml")));
        Scene mainscene = new Scene(mainroot, 720, 480);
        primaryStage.setTitle("Sudoku");
        primaryStage.setScene(mainscene);
        primaryStage.sizeToScene();
        primaryStage.show();
    }

    public static void main(String[] args) {
        System.setProperty("javafx.App", User.class.getName());
        launch(args);
    }
}
