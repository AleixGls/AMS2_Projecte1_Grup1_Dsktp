
package grup1.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import tools.jackson.databind.ObjectMapper;

public class UtilsWS {

    private final HttpClient client;
    private volatile WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Metodo que recibira el catalogo del servidor
    private Consumer<String> onCatalogo;

    // Respuestas pendientes de los mapas solicitados
    private final Map<String, Consumer<String>> mapasPendientes =
            new ConcurrentHashMap<>();

    private final Map<String, Consumer<String>> erroresMapaPendientes =
            new ConcurrentHashMap<>();

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
                        "{\"type\":\"ping\"}", true
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

                    if (finalizado.compareAndSet(false, true)) {

                        onFailure.accept(
                                new TimeoutException(
                                        "El servidor no ha respondido."
                                )
                        );

                        webSocket.abort();
                    }
                });
            }

            // Recibir los mensajes del servidor
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
                        Map<?, ?> respuesta =
                                objectMapper.readValue(mensaje, Map.class);

                        String tipo = String.valueOf(respuesta.get("type"));

                        // Comprobar si el servidor ha respondido PONG
                        if ("pong".equals(tipo)) {

                            if (finalizado.compareAndSet(false, true)) {

                                System.out.println(
                                        "PONG recibido correctamente."
                                );

                                onSuccess.accept(webSocket);
                            }

                        // Comprobar si hemos recibido el catalogo
                        } else if ("catalog".equals(tipo)) {

                            System.out.println(
                                    "Catalogo recibido correctamente."
                            );

                            // Obtener solamente el contenido del catalogo
                            String catalogo = objectMapper.writeValueAsString(
                                    respuesta.get("catalog")
                            );

                            if (onCatalogo != null) {
                                onCatalogo.accept(catalogo);
                            }

                        // Respuesta del mapa de un evento
                        } else if ("mapa".equals(tipo)) {

                            String idEsdeveniment =
                                    String.valueOf(
                                            respuesta.get("esdeveniment")
                                    );

                            Consumer<String> callback =
                                    mapasPendientes.remove(idEsdeveniment);

                            erroresMapaPendientes.remove(idEsdeveniment);

                            if (callback != null) {

                                String mapaJSON =
                                        objectMapper.writeValueAsString(respuesta);

                                callback.accept(mapaJSON);

                                System.out.println(
                                        "Mapa recibido correctamente: "
                                        + idEsdeveniment
                                );

                            } else {

                                System.out.println(
                                        "No hay una petición de mapa pendiente."
                                );
                            }

                        // Comprobar si el servidor ha devuelto un error
                        } else if ("error".equals(tipo)) {

                            Object id = respuesta.get("esdeveniment");

                            if (id != null) {

                                String idEsdeveniment = String.valueOf(id);

                                mapasPendientes.remove(idEsdeveniment);

                                Consumer<String> callback =
                                        erroresMapaPendientes.remove(
                                                idEsdeveniment
                                        );

                                if (callback != null) {

                                    callback.accept(
                                            String.valueOf(
                                                    respuesta.get("message")
                                            )
                                    );
                                }

                            } else {

                                System.err.println(
                                        "Error del servidor: "
                                        + respuesta.get("message")
                                );
                            }

                        } else {

                            System.out.println(
                                    "Respuesta no reconocida: " + tipo
                            );
                        }

                    } catch (Exception e) {

                        System.err.println(
                                "Error procesando JSON: " + e.getMessage()
                        );

                        e.printStackTrace();
                    }
                }

                // Solicitar el siguiente mensaje
                webSocket.request(1);

                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {

                System.err.println(
                        "Error WebSocket: " + error.getMessage()
                );

                fallar.accept(error);
                notificarErroresMapas(error.getMessage());
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

                notificarErroresMapas(
                        "Se ha cerrado la conexión con el servidor."
                );

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

    // Solicitar el mapa de un evento
    public void solicitarMapa(
            String idEsdeveniment,
            Consumer<String> onRespuesta,
            Consumer<String> onError) {

        if (webSocket == null) {

            onError.accept("No hay conexión con el servidor.");
            return;
        }

        // Guardar las respuestas pendientes
        mapasPendientes.put(idEsdeveniment, onRespuesta);
        erroresMapaPendientes.put(idEsdeveniment, onError);

        try {

            // Crear la peticion JSON
            String peticion = objectMapper.writeValueAsString(
                    Map.of(
                            "type", "mapa",
                            "esdeveniment", idEsdeveniment
                    )
            );

            System.out.println(
                    "Solicitando mapa: " + idEsdeveniment
            );

            // Enviar la peticion al servidor
            webSocket.sendText(peticion, true).whenComplete((resultado, error) -> {

                        if (error != null) {

                            mapasPendientes.remove(idEsdeveniment);
                            erroresMapaPendientes.remove(idEsdeveniment);

                            onError.accept(error.getMessage());
                        }
                    });

        } catch (Exception e) {

            mapasPendientes.remove(idEsdeveniment);
            erroresMapaPendientes.remove(idEsdeveniment);

            onError.accept(e.getMessage());
        }
    }

    // Notificar errores a las peticiones de mapas pendientes
    private void notificarErroresMapas(String mensaje) {

        erroresMapaPendientes.forEach((id, callback) -> {

            callback.accept(mensaje);
        });

        erroresMapaPendientes.clear();
        mapasPendientes.clear();
    }

    // Desconectar del servidor WebSocket
    public void disconnect() {
    
        if (webSocket != null) {
        
            System.out.println("Desconectando del servidor...");
        
            webSocket.sendClose(
                    WebSocket.NORMAL_CLOSURE,
                    "Desconexion del cliente"
            ).whenComplete((resultado, error) -> {
            
                if (error != null) {
                
                    System.err.println(
                            "Error al desconectar: " + error.getMessage()
                    );
                
                    webSocket.abort();
                }
            
                webSocket = null;
            
            });
        
            notificarErroresMapas(
                    "Se ha cerrado la conexión con el servidor."
            );
        
        } else {
        
            System.out.println("No hay ninguna conexion activa.");
        
        }
    }
}
