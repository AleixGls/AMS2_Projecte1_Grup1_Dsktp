package grup1.desktop;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.application.Platform;


import javafx.fxml.FXMLLoader;

public class Main extends Application {

    public static CtrlConnexio ctrlConnexio;
    public static UtilsWS utilsWS = new UtilsWS();

    private static Stage stagePrincipal;
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
            return ;
        }

        utilsWS.connect(serverUrl,
        ws -> { //En caso de que conecte bien al servidor 
            System.out.println("La conexion se ha establecido con exito ");
            ConfigManager.updateConfigXML(location, server);
            Platform.runLater(() -> obrirLlistatVendes()); 
            // Ejecutamos el cambio de vista en el hilo de JavaFX

        },
        error -> { //Caso en el que no conecte al servidor 
            System.out.println("La conexion ha fallado");
        }
    );

        

    }

    @Override
    public void start(Stage stage) throws Exception { //Este metodo consigue que al ejectuar el codigo aparezca la interfaz grafica
        stagePrincipal = stage;
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

    public static void obrirLlistatVendes() { 
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                Main.class.getResource("/assets/events.fxml")
            );

            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);

            stagePrincipal.setScene(scene);
            stagePrincipal.setTitle("Llistat de vendes");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}