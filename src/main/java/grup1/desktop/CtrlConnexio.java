package grup1.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class CtrlConnexio {


    //Atributos del entorno grafico del desktop 
    @FXML
    private ComboBox<String> ComboBoxUbicacion;

    @FXML
    private TextField TextServidor;

    @FXML
    private Button buttonConnecta;

    @FXML
    private void initialize() {
        ComboBoxUbicacion.getItems().addAll(
            "Taquilla","Puerta","Oficina"
        );
    }

    @FXML // Al pulsar el boton de conectar
    private void onConnecta() {
        Main.connectToServer();
    }

    public String getComboBoxValue() { //Para coger el valor de la combo box 
        return ComboBoxUbicacion.getValue();
    }

    public String getTextServidor() {
        return TextServidor.getText();
    }

    
}