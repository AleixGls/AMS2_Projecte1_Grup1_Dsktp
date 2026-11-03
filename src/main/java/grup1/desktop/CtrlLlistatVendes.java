package grup1.desktop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import tools.jackson.databind.ObjectMapper;

public class CtrlLlistatVendes {

    // Tablas de la interfaz grafica
    @FXML
    private TableView<Esdeveniment> tableEsdeveniments;

    @FXML
    private TableColumn<Esdeveniment, String> idEventoColumn;

    @FXML
    private TableColumn<Esdeveniment, String> nombreEventoColumn;

    @FXML
    private TableColumn<Esdeveniment, String> recintoEventoColumn;

    @FXML
    private TableColumn<Esdeveniment, String> fechaEventoColumn;

    @FXML
    private TableColumn<Esdeveniment, String> categoriaEventoColumn;

    @FXML
    private TableView<TipusEntrada> tableTipusEntrades;

    @FXML
    private TableColumn<TipusEntrada, String> idEntradaColumn;

    @FXML
    private TableColumn<TipusEntrada, String> nombreEntradaColumn;

    @FXML
    private TableColumn<TipusEntrada, String> zonaEntradaColumn;

    @FXML
    private TableColumn<TipusEntrada, String> preuEntradaColumn;

    @FXML
    private TableColumn<TipusEntrada, String> quotaEntradaColumn;


    // Objeto para leer el JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mapas para relacionar los identificadores con sus nombres
    private final Map<String, String> nombresRecintos = new HashMap<>();
    private final Map<String, String> nombresZonas = new HashMap<>();


    @FXML
    private void initialize() {

        // Configurar las columnas de eventos
        idEventoColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().id())
        );

        nombreEventoColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().nom())
        );

        recintoEventoColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        nombresRecintos.getOrDefault(
                                fila.getValue().recinte(),
                                fila.getValue().recinte()
                        )
                )
        );

        fechaEventoColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().dataHora())
        );

        categoriaEventoColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().categoria())
        );


        // Configurar las columnas de tipos de entrada
        idEntradaColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().id())
        );

        nombreEntradaColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().nom())
        );

        zonaEntradaColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        nombresZonas.getOrDefault(
                                fila.getValue().zona(),
                                fila.getValue().zona()
                        )
                )
        );

        preuEntradaColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        fila.getValue().preu() + " €"
                )
        );

        quotaEntradaColumn.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        String.valueOf(fila.getValue().quota())
                )
        );


        // Cuando se selecciona un evento, mostrar sus entradas
        tableEsdeveniments.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, seleccionado) -> {

                    mostrarEntradas(seleccionado);
                });

    }



    // Recibir y procesar el JSON del servidor
    public void cargarCatalogo(String json) {

        try {

            // Convertir el JSON en un objeto Catalogo
            Catalogo catalogo = objectMapper.readValue(
                    json,
                    Catalogo.class
            );

            if (catalogo == null) {
                throw new IllegalArgumentException("El catalogo esta vacio.");
            }

            // Preparar los recintos y sus zonas
            Runnable actualizar = () -> {

                nombresRecintos.clear();
                nombresZonas.clear();

                if (catalogo.recintes() != null) {

                    for (Recinte recinte : catalogo.recintes()) {

                        nombresRecintos.put(
                                recinte.id(),
                                recinte.nom()
                        );

                        if (recinte.zones() != null) {

                            for (Zona zona : recinte.zones()) {

                                nombresZonas.put(
                                        zona.id(),
                                        zona.nom()
                                );
                            }
                        }
                    }
                }

                // Cargar los eventos en la tabla
                List<Esdeveniment> esdeveniments =
                        catalogo.esdeveniments() != null
                                ? catalogo.esdeveniments()
                                : List.of();

                tableEsdeveniments.setItems(
                        FXCollections.observableArrayList(esdeveniments)
                );

                // Limpiar las entradas anteriores
                tableTipusEntrades.getItems().clear();

                // Seleccionar el primer evento si existe
                if (!esdeveniments.isEmpty()) {

                    tableEsdeveniments.getSelectionModel().selectFirst();
                }
            };

            // Actualizar la interfaz desde el hilo de JavaFX
            if (Platform.isFxApplicationThread()) {

                actualizar.run();

            } else {

                Platform.runLater(actualizar);
            }

        } catch (Exception e) {

            System.err.println(
                    "Error procesando el catalogo: " + e.getMessage()
            );

            Platform.runLater(() -> {

                Alert alerta = new Alert(Alert.AlertType.ERROR);

                alerta.setTitle("Error");
                alerta.setHeaderText("Error al cargar el catálogo");
                alerta.setContentText(e.getMessage());

                alerta.showAndWait();
            });
        }
    }


    // Mostrar las entradas del evento seleccionado
    private void mostrarEntradas(Esdeveniment esdeveniment) {

        if (esdeveniment == null || esdeveniment.tipusEntrades() == null) {

            tableTipusEntrades.setItems(
                    FXCollections.observableArrayList()
            );

            return;
        }

        tableTipusEntrades.setItems(
                FXCollections.observableArrayList(
                        esdeveniment.tipusEntrades()
                )
        );
    }


    // Estructuras para recibir los datos del JSON

    public record Catalogo(
            List<Recinte> recintes,
            List<Esdeveniment> esdeveniments
    ) {
    }

    public record Recinte(
            String id,
            String nom,
            List<Zona> zones
    ) {
    }

    public record Zona(
            String id,
            String nom,
            String tipus,
            int aforament
    ) {
    }

    public record Esdeveniment(
            String id,
            String recinte,
            String nom,
            String dataHora,
            String categoria,
            String descripcio,
            String imatge,
            List<TipusEntrada> tipusEntrades
    ) {
    }

    public record TipusEntrada(
            String id,
            String nom,
            String zona,
            double preu,
            int quota
    ) {
    }


}