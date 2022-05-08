package krypto;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class Controller {

    private static final Logger l = LoggerFactory.getLogger(User.class.getName());
    private FileReader fr = new FileReader();
    private FileChooser fileChooser;

    public void onActionButtonLoad(ActionEvent actionEvent) throws IOException {
        String saveName;
        fileChooser = new FileChooser();
        StackPane chooser = new StackPane();
        Scene chooserScene = new Scene(chooser, 500, 500);
        Stage chooserStage = new Stage();
        saveName = fileChooser.showSaveDialog(chooserStage).getName();
        try {
            fr.read(saveName);
        } catch (Exception unhandledException) {
            l.warn("Unknown_error");
            unhandledException.printStackTrace();
        }
    }
    public void onActionButtonSave(ActionEvent actionEvent) throws IOException {
        fr.save(fr.getBytes());
    }
}
