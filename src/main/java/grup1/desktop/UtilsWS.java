package grup1.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import java.util.function.Consumer;

public class UtilsWS { // Clase encargada de gestionar la conexion Cliente-Servidor
    private final HttpClient client;
    private WebSocket webSocket; 

    public UtilsWS() {
        client = HttpClient.newHttpClient();
    }

    //*onSuccess se ejeutara cuando se conecte correctamente al server
    // onFailure se ejecutara al fallar la conexion */

    public void connect(String serverUrl, Consumer<WebSocket> onSuccess, Consumer<Throwable> onFailure) {
        WebSocket.Listener listener = new WebSocket.Listener() { //Creamos un listener de websockets
            @Override
            public void onOpen(WebSocket webSocket) { //Se ejecuta cuando se establece la conexion 
                UtilsWS.this.webSocket = webSocket;
                onSuccess.accept(webSocket); //Notificas el exito de la conexion 

                System.out.println("Conexión WebSocket establecida.");

                webSocket.request(1);
            }

            //Metodo que recibira el JSON del servidor 
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                System.out.println("Mensaje recibido del servidor: " + data);
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) { //Cuando hay algun error en la conexion 
                System.err.println("Error WebSocket: " + error.getMessage());
            }

            @Override
            public CompletionStage<?> onClose( //Cuando se cierra la conexion 
                WebSocket webSocket, int statusCode, String reason) {
                    System.out.println("Conexión cerrada: " + reason);
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };

        client.newWebSocketBuilder() // Constructor de conexiones WebSocket
            .buildAsync(java.net.URI.create(serverUrl), listener)
            .exceptionally(error -> { //Esta parte se ejecuta si la operacion falla 
                System.err.println("No se pudo conectar: " + error.getMessage());
                onFailure.accept(error);
                return null;
            });
    }
}


