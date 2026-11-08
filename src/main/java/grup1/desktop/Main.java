
package grup1.desktop;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;

public class Main extends Application {

    public static CtrlConnexio ctrlConnexio;
    public static CtrlLlistatVendes ctrlLlistatVendes;
    public static UtilsWS utilsWS = new UtilsWS();

    private static Stage stagePrincipal;

    public static void main(String[] args) {
        launch(args);
    }

    public static void connectToServer() { // Metodo del cliente para conectarse al servidor

        String location = ctrlConnexio.getComboBoxValue();
        String server = ctrlConnexio.getTextServidor();

        // Comprobar que los campos no esten vacios
        if (location == null || location.isBlank() || server == null || server.isBlank()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campos incompletos",
                    "Rellena todos los campos antes de conectar."
            );

            return;
        }

        // Construir la direccion del servidor
        String serverUrl = "wss://" + server.trim();

        System.out.println("Conectando a: " + serverUrl);

        // Indicar donde se mostraran los eventos recibidos
        utilsWS.setOnCatalogo(json -> {

            if (ctrlLlistatVendes != null) {
                ctrlLlistatVendes.cargarCatalogo(json);
            }

        });

        utilsWS.connect(
                serverUrl,

                ws -> { // En caso de que conecte bien al servidor

                    System.out.println("Conexion establecida correctamente.");

                    // Guardar la configuracion despues de conectar correctamente
                    try {

                        ConfigManager.updateConfigXML(location, server.trim());

                    } catch (Exception e) {

                        System.err.println("Error guardando la configuracion: " + e.getMessage());
                    }

                    // Cambiar de pantalla en el hilo de JavaFX
                    Platform.runLater(() -> obrirLlistatVendes());  

                },

                error -> { // Caso en el que no conecte al servidor

                    System.err.println("Error de conexion: " + error.getMessage());

                    Platform.runLater(() -> mostrarAlerta(
                            Alert.AlertType.ERROR,
                            "Error de conexion",
                            error.getMessage()
                    ));
                }
        );
    }

    // Mostrar mensajes de error o informacion
    private static void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }

    @Override
    public void start(Stage stage) throws Exception { // Este metodo consigue que al ejecutar el codigo aparezca la interfaz grafica

        stagePrincipal = stage;

        final int windowWidth = 1000;
        final int windowHeight = 700;

        FXMLLoader fxmlLoader = new FXMLLoader( // Cargamos la interfaz grafica
                getClass().getResource("/assets/connecta.fxml")
        );

        Parent root = fxmlLoader.load(); // Construye la interfaz

        ctrlConnexio = fxmlLoader.getController();

        Scene scene = new Scene(root); // Creamos la escena

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

            // Obtener el controlador de la pantalla
            ctrlLlistatVendes = fxmlLoader.getController();

            Scene scene = new Scene(root);

            stagePrincipal.setScene(scene);
            stagePrincipal.setTitle("Llistat de vendes");

            // Solicitar el catalogo al servidor
            utilsWS.solicitarEventos();

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de interfaz",
                    "No se ha podido abrir la siguiente pantalla."
            );
        }
    }

    
    // Abrir la ventana del mapa del recinto
    public static void abrirMapa(CtrlLlistatVendes.Esdeveniment esdeveniment) {

        try {

            FXMLLoader fxmlLoader = new FXMLLoader(
                    Main.class.getResource("/assets/mapa.fxml")
            );

            Parent root = fxmlLoader.load();

            // Obtener el controlador de la ventana
            CtrlMapa ctrlMapa = fxmlLoader.getController();

            // Obtener el nombre del recinto
            String nombreRecinto = ctrlLlistatVendes.obtenerNombreRecinto(
                esdeveniment.recinte()
            );

            // Pasar la información al controlador del mapa
            ctrlMapa.setEsdeveniment(esdeveniment, nombreRecinto);

            // Crear una nueva ventana
            Stage stageMapa = new Stage();

            stageMapa.setTitle("Mapa del recinto");
            stageMapa.setScene(new Scene(root));
            stageMapa.setMinWidth(900);
            stageMapa.setMinHeight(600);

            stageMapa.initOwner(stagePrincipal);

            stageMapa.show();

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de interfaz",
                    "No se ha podido abrir el mapa del recinto."
            );
        }
    }

}
