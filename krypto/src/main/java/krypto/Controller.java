package krypto;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    private static final Logger l = LoggerFactory.getLogger(App.class.getName());
    private FileReader fr = new FileReader();
    private FileReader frS = new FileReader();
    private FileChooser fileChooser;
    private String saveName = "pusty.txt";
    private Algorithm ar = new Algorithm();
    private Signature sg = new Signature();
    private Path path = Path.of("C:/pusty.txt");
    private BigInteger[] podpis;

    public Button Exit;
    public TextField l1 = new TextField();
    public TextField l2 = new TextField();
    public TextField l3 = new TextField();
    public TextField l4 = new TextField();

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
        frS = fr;
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

    public void onActionButtonGenerate(ActionEvent actionEvent) throws IOException {
        sg.generateKey();
        l1.setText(sg.getY().toString(16));
        l2.setText(sg.getX().toString(16));
        l3.setText(sg.getQ().toString(16)+" "+sg.getG().toString(16));
        l4.setText(sg.getP().toString(16));
    }

    public void onActionButtonSign(ActionEvent actionEvent) throws IOException {
        podpis = sg.podpisuj(frS.getBytes());
        String s = podpis.toString();
        byte[] g = s.getBytes();
        fr.setBytes(g);
    }

    public void onActionButtonCheck(ActionEvent actionEvent) throws IOException {
        if(sg.weryfikujBigInt(frS.getBytes(), podpis)) {
            Label l1 = new Label("Podpis zgodny");
            StackPane root = new StackPane();
            root.getChildren().add(l1);
            Scene sc2 = new Scene(root, 250, 250);
            Stage st2 = new Stage();
            st2.setScene(sc2);
            st2.show();
        } else {
            Label l1 = new Label("Podpis niezgodny");
            StackPane root = new StackPane();
            root.getChildren().add(l1);
            Scene sc2 = new Scene(root, 250, 250);
            Stage st2 = new Stage();
            st2.setScene(sc2);
            st2.show();
        }
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
