package grup1.desktop;

import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class CtrlPorta {

    @FXML
    TextField inputIdEntrada;

    @FXML
    Label txtMissatgeVerificacio;



    @FXML // Al pulsar el boton de verificar
    private void verificarID() {

        String idEntrada = inputIdEntrada.getText();

        // Comprobar que se ha introducido un identificador
        if (idEntrada.isEmpty()) {

            txtMissatgeVerificacio.setText("Has d'introduir un ID d'entrada.");

            return;
        }

        if (idEntrada.length() == 6){

            // Mostrar que estamos realizando la comprobación
            txtMissatgeVerificacio.setText("Verificant entrada...");

            // Enviar la petición al servidor
            Main.utilsWS.validarEntrada(
                idEntrada,
            
                // Respuesta recibida del servidor
                json -> javafx.application.Platform.runLater(() -> {
                    try {
                        Map<?, ?> respuesta = new tools.jackson.databind
                            .ObjectMapper()
                            .readValue(json, Map.class);
                        
                        String tipo = String.valueOf(respuesta.get("type"));
                        String mensaje = String.valueOf(respuesta.get("message"));
                        
                        txtMissatgeVerificacio.setText(mensaje);
                        
                        if ("validacioOk".equals(tipo)) {
                            System.out.println("Entrada validada correctamente.");

                        } else {
                            System.out.println("Entrada no válida: " + mensaje);
                        }
                        
                    } catch (Exception e) {
                        
                        txtMissatgeVerificacio.setText("Error procesando la respuesta del servidor.");
                        
                        e.printStackTrace();
                    }
                }),

                // Error de conexión o de envío
                error -> javafx.application.Platform.runLater(() -> {
                
                    txtMissatgeVerificacio.setText(
                        "Error de conexión: " + error
                    );
                })
            );

        } else if (idEntrada.length() > 6) {
            txtMissatgeVerificacio.setText("ID de la entrada molt llarga, ha de ser de 6 caracters.");
            
        } else if (idEntrada.length() < 6) {
            txtMissatgeVerificacio.setText("ID de la entrada molt curta, ha de ser de 6 caracters.");
            
        } else {
            txtMissatgeVerificacio.setText(
                "L'ID de la entrada ha de ser de 6 caracters."
            );
        }
    }
}
