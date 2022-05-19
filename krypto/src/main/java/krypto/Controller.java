package krypto;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    private static final Logger l = LoggerFactory.getLogger(App.class.getName());
    private FileReader fr = new FileReader();
    private FileChooser fileChooser;
    private String saveName = "pusty.txt";
    private Algorithm ar = new Algorithm();
    private Path path = Path.of("C:/pusty.txt");

    public Button Exit;

    public void onActionButtonLoad(ActionEvent actionEvent) throws IOException {
        fileChooser = new FileChooser();
        StackPane chooser = new StackPane();
        Scene chooserScene = new Scene(chooser, 500, 500);
        Stage chooserStage = new Stage();
        saveName = fileChooser.showOpenDialog(chooserStage).getPath();
        path = Paths.get(saveName);
        saveName = path.getFileName().toString();
        try {
            fr.read(path);
        } catch (Exception unhandledException) {
            l.warn("Unknown_error");
            unhandledException.printStackTrace();
        }
    }

    public void onActionButtonShow(ActionEvent actionEvent) throws IOException {
        Label l1 = new Label(saveName);
        StackPane root = new StackPane();
        root.getChildren().add(l1);
        Scene sc2 = new Scene(root, 250, 250);
        Stage st2 = new Stage();
        st2.setScene(sc2);
        st2.show();
    }

    public void onActionButtonShowFile(ActionEvent actionEvent) throws IOException {
        String y = fr.getBytes().toString();
        Label l1 = new Label(y);
        StackPane root = new StackPane();
        root.getChildren().add(l1);
        Scene sc2 = new Scene(root, 250, 250);
        Stage st2 = new Stage();
        st2.setScene(sc2);
        st2.show();
    }

    public void onActionButtonEncode(ActionEvent actionEvent) throws IOException {
        fr.setBytes(ar.encode3DES(fr.getBytes()));
    }

    public void onActionButtonDecode(ActionEvent actionEvent) throws IOException {
        fr.setBytes(ar.decode3DES(fr.getBytes()));
    }

    public void onActionButtonSave(ActionEvent actionEvent) throws IOException {
        fileChooser = new FileChooser();
        StackPane chooser = new StackPane();
        Scene chooserScene = new Scene(chooser, 500, 500);
        Stage chooserStage = new Stage();
        saveName = fileChooser.showSaveDialog(chooserStage).getPath();
        path = Paths.get(saveName);
        fr.save(fr.getBytes(), path);
    }

    public void onActionButtonExit(ActionEvent actionEvent) throws IOException {
        Stage stage = (Stage) Exit.getScene().getWindow();
        stage.close();
    }
}
