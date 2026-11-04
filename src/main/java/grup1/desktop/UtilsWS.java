
package grup1.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;

public class UtilsWS {

    private final HttpClient client;
    private volatile WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Metodo que recibira el catalogo del servidor
    private Consumer<String> onCatalogo;

    public UtilsWS() {

        // Crear el cliente HTTP
        client = HttpClient.newHttpClient();
    }

    // Indicar quien recibira el catalogo
    public void setOnCatalogo(Consumer<String> onCatalogo) {
        this.onCatalogo = onCatalogo;
    }

    // Conectar con el servidor WebSocket
    public void connect(
            String serverUrl,
            Consumer<WebSocket> onSuccess,
            Consumer<Throwable> onFailure) {

        // Controlar que el resultado solo se ejecute una vez
        AtomicBoolean finalizado = new AtomicBoolean(false);

        StringBuilder mensajeRecibido = new StringBuilder();

        // Gestionar errores
        Consumer<Throwable> fallar = error -> {

            if (finalizado.compareAndSet(false, true)) {
                onFailure.accept(error);
            }
        };

        // Escuchar los acontecimientos del WebSocket
        WebSocket.Listener listener = new WebSocket.Listener() {

            @Override
            public void onOpen(WebSocket webSocket) {

                // Guardar la conexion
                UtilsWS.this.webSocket = webSocket;

                System.out.println("WebSocket conectado.");

                // Solicitar mensajes al servidor
                webSocket.request(1);

                // Enviar PING para comprobar la conexion
                webSocket.sendText(
                        "{\"type\":\"ping\"}",true
                ).whenComplete((resultado, error) -> {

                    if (error != null) {
                        fallar.accept(error);
                        webSocket.abort();
                    }
                });

                // Esperar como maximo 8 segundos la respuesta
                CompletableFuture.delayedExecutor(
                        8, TimeUnit.SECONDS
                ).execute(() -> {

                    if (finalizado.compareAndSet(false, true)) { //Comprobamos si ya ha terminado la conexion 

                        onFailure.accept( //Si hay un error, avisamos 
                                new TimeoutException(
                                        "El servidor no ha respondido."
                                )
                        );

                        webSocket.abort(); //Interrumpimos la conexion 
                    }
                });
            }

            // Recibir mensajes del servidor
            @Override
            public CompletionStage<?> onText(
                    WebSocket webSocket,
                    CharSequence data,
                    boolean last) {

                // Guardar el mensaje recibido
                mensajeRecibido.append(data);

                if (last) {

                    String mensaje = mensajeRecibido.toString();
                    mensajeRecibido.setLength(0);

                    System.out.println("Mensaje recibido: " + mensaje);

                    try {

                        // Convertir el mensaje recibido en un mapa
                        Map<?, ?> respuesta = objectMapper.readValue(mensaje, Map.class);

                        String tipo = String.valueOf(respuesta.get("type"));

                        // Comprobar si el servidor ha respondido PONG
                        if ("pong".equals(tipo)) {

                            if (finalizado.compareAndSet(false, true)) {

                                System.out.println("PONG recibido correctamente.");

                                onSuccess.accept(webSocket);
                            }

                        // Comprobar si hemos recibido el catalogo
                        } else if ("catalog".equals(tipo)) {

                            System.out.println("Catalogo recibido correctamente.");

                            // Obtener solamente el contenido del catalogo
                            String catalogo = objectMapper.writeValueAsString(
                                    respuesta.get("catalog")
                            );

                            if (onCatalogo != null) {
                                onCatalogo.accept(catalogo);
                            }

                        // Comprobar si el servidor ha devuelto un error
                        } else if ("error".equals(tipo)) {

                            System.err.println(
                                    "Error del servidor: " + respuesta.get("message")
                            );

                        } else {

                            System.out.println("Respuesta no reconocida: " + tipo);
                        }

                    } catch (Exception e) {

                        System.err.println("Error procesando JSON: " + e.getMessage());
                    }
                }

                // Solicitar el siguiente mensaje
                webSocket.request(1);

                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {

                System.err.println("Error WebSocket: " + error.getMessage());

                fallar.accept(error);
            }

            @Override
            public CompletionStage<?> onClose(
                    WebSocket webSocket,
                    int statusCode,
                    String reason) {

                System.out.println("Conexion cerrada: " + reason);

                if (!finalizado.get()) {
                    fallar.accept(
                            new IllegalStateException(
                                    "El servidor ha cerrado la conexion."
                            )
                    );
                }

                UtilsWS.this.webSocket = null;

                return CompletableFuture.completedFuture(null);
            }
        };

        try {

            // Construir la direccion del servidor
            URI uri = URI.create(serverUrl);

            System.out.println("Conectando a: " + uri);

            // Iniciar la conexion
            client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .buildAsync(uri, listener)
                    .whenComplete((ws, error) -> {

                        if (error != null) {
                            fallar.accept(error);
                        }
                    });

        } catch (Exception e) {

            fallar.accept(e);
        }
    }

    // Solicitar los eventos al servidor
    public void solicitarEventos() {

        if (webSocket != null) {

            System.out.println("Solicitando eventos...");

            webSocket.sendText(
                    "{\"type\":\"cataleg\"}",
                    true
            ).whenComplete((resultado, error) -> {

                if (error != null) {
                    System.err.println(
                            "Error solicitando eventos: " + error.getMessage()
                    );
                }
            });

        } else {

            System.err.println("No hay conexion con el servidor.");
        }
    }
}
