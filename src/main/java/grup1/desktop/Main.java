package grup1.desktop;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.fxml.FXMLLoader;

public class Main extends Application {

    public static CtrlConnexio ctrlConnexio;
    public static void main(String[] args) {
        launch(args);
    }

    public static void connectToServer() { //Metodo del cliente para conectarse al servidor 
        String location = ctrlConnexio.getComboBoxValue();
        String server = ctrlConnexio.getTextServidor();
        //ieticloudpro.ieti.cat

        String serverUrl = "ws://" + server + ":3000"; 
        
        //*Se comprueba si location es null ya que  cogemos el valor de un comboBox
        // En el resto de comprobaciones, miramos que no este vacio ni con unicamente espacios */
        if (location == null || location.isBlank() || server.isBlank()) { 
            System.out.println("Rellena los campos");
        }

    }

    @Override
    public void start(Stage stage) throws Exception { //Este metodo consigue que al ejectuar el codigo aparezca la interfaz grafica
        final int windowWidth = 1000;
        final int windowHeight = 700;

        FXMLLoader fxmlLoader = new FXMLLoader( //Cargamos la interfaz grafica
            getClass().getResource("/assets/connecta.fxml")
        );

        Parent root = fxmlLoader.load(); //Contstruye la interfaz

        ctrlConnexio = fxmlLoader.getController();

        Scene scene = new Scene(root); //Creamos la escena

        stage.setScene(scene);
        stage.setTitle("Portal de conexión");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();
    }

    
}