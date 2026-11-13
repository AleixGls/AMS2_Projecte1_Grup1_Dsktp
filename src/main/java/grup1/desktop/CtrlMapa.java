package grup1.desktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.ReadOnlyStringWrapper;

import tools.jackson.databind.ObjectMapper;

public class CtrlMapa {

    // IDs de las zonas que se agrupan visualmente.
    private final Set<String> idsZonasNumeradas = new HashSet<>();
    private final Set<String> idsZonasGenerales = new HashSet<>();  

    // Datos recibidos y posiciones utilizadas para dibujar y seleccionar.
    private final List<Asiento> asientos = new ArrayList<>();
    private final Map<String, Posicion> posicionesAsientos = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private int aforoNumerada;
    private int ocupadosNumerada;
    private int aforoGeneral;
    private int ocupadosGeneral;

    // Distribución del mapa: cinco filas y columnas hacia la derecha.
    private static final int FILAS = 5;
    private static final double TAMANO_ASIENTO = 18;
    private static final double SEPARACION_ASIENTO = 12;
    private static final double PASO_ASIENTO = TAMANO_ASIENTO + SEPARACION_ASIENTO;
    private static final double INICIO_X = 50;
    private static final double INICIO_Y = 55;
    private static final double ALTO_ZONA_GENERAL = 100;

    @FXML private Label lblNombreEvento;
    @FXML private Label lblNombreRecinto;
    @FXML private Canvas canvasMapa;
    @FXML private Label lblAsiento;
    @FXML private Label lblZona;
    @FXML private Label lblLocalizador;
    @FXML private Label lblComprador;
    @FXML private Label lblCorreo;
    @FXML private Label lblImporte;
    @FXML private Label lblEstado;
    @FXML private Label lblOcupacionNumerada;
    @FXML private Label lblOcupacionGeneral;

    @FXML private TableView<EntradaVendida> tableEntradasVendidas;
    @FXML private TableColumn<EntradaVendida, String> localizadorEntradaMapa;
    @FXML private TableColumn<EntradaVendida, String> nombreEntradaMapa;
    @FXML private TableColumn<EntradaVendida, String> tipoEntradaMapa;
    @FXML private TableColumn<EntradaVendida, String> asientoEntradaMapa;
    @FXML private TableColumn<EntradaVendida, String> estadoEntradaMapa;
    @FXML private TableColumn<EntradaVendida, String> fechaEntradaMapa;

    private final List<EntradaVendida> entradasVendidas = new ArrayList<>();


    @FXML
    private void initialize() {
        canvasMapa.setOnMouseClicked(event ->
                seleccionarAsiento(event.getX(), event.getY())
        );
        configurarTablaEntradasInicial();
    }

    // Recibir el evento y el recinto seleccionado desde el listado.
    public void setEsdeveniment(
            CtrlLlistatVendes.Esdeveniment esdeveniment,
            CtrlLlistatVendes.Recinte recinto
    ) {
        lblNombreEvento.setText(esdeveniment.nom());
        lblNombreRecinto.setText(recinto.nom());

        limpiarMapa();
        limpiarZonas();

        // Utilizar inicialmente las zonas del catálogo.
        // La respuesta del servidor las sustituirá si incluye sus propias zonas.
        if (recinto.zones() != null) {
            for (CtrlLlistatVendes.Zona zona : recinto.zones()) {
                registrarZona(new ZonaMapa(
                        zona.id(),
                        zona.nom(),
                        zona.tipus(),
                        zona.aforament(),
                        0
                ));
            }
        }

        actualizarOcupacionZonas();
        limpiarSeleccion();
    }

    // Convertir el JSON directamente en records y actualizar el mapa.
    public void cargarMapa(String json) {
        try {
            RespuestaMapa respuesta = objectMapper.readValue(json, RespuestaMapa.class);

            if (!"mapa".equals(respuesta.type())) {
                throw new IllegalArgumentException(
                        "La respuesta recibida no corresponde a un mapa."
                );
            }

            asientos.clear();
            posicionesAsientos.clear();
            cargarEntradasVendidas(respuesta.entradesVenudes());

            // Si el servidor envía las zonas, sustituir las del catálogo.
            if (respuesta.zonas() != null) {
                limpiarZonas();
                respuesta.zonas().forEach(this::registrarZona);
            }

            // Guardar únicamente asientos pertenecientes a zonas numeradas.
            Set<String> idsProcesados = new HashSet<>();

            if (respuesta.seients() != null) {
                for (Asiento asiento : respuesta.seients()) {
                    if (asiento == null || asiento.id() == null
                            || asiento.idZona() == null
                            || !idsZonasNumeradas.contains(asiento.idZona())) {
                        continue;
                    }

                    if (idsProcesados.add(asiento.id())) {
                        asientos.add(asiento);
                    } else {
                        System.out.println("Asiento repetido recibido: " + asiento.id());
                    }
                }
            }

            dibujarMapa();
            actualizarOcupacionZonas();
            limpiarSeleccion();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al cargar el mapa");
            alerta.setHeaderText("No se ha podido cargar el mapa del recinto");
            alerta.setContentText(e.getMessage());
            alerta.show();
        }
    }

    // Reiniciar los datos de las zonas antes de volver a registrarlas.
    private void limpiarZonas() {
        idsZonasNumeradas.clear();
        idsZonasGenerales.clear();
        aforoNumerada = 0;
        ocupadosNumerada = 0;
        aforoGeneral = 0;
        ocupadosGeneral = 0;
    }

    // Acumular el aforo y la ocupación de cada tipo de zona.
    private void registrarZona(ZonaMapa zona) {
        if (zona == null || zona.id() == null || zona.id().isBlank()) {
            return;
        }

        if ("numerada".equalsIgnoreCase(zona.tipo())) {
            if (idsZonasNumeradas.add(zona.id())) {
                aforoNumerada += zona.aforament();
                ocupadosNumerada += zona.ocupacion();
            }
        } else if ("general".equalsIgnoreCase(zona.tipo())) {
            if (idsZonasGenerales.add(zona.id())) {
                aforoGeneral += zona.aforament();
                ocupadosGeneral += zona.ocupacion();
            }
        }
    }

    // Dibujar todos los asientos como una única cuadrícula de cinco filas.
    private void dibujarMapa() {
        List<Asiento> asientosNumerados = obtenerAsientosNumerados();
        int columnas = Math.max(1, (int) Math.ceil(asientosNumerados.size() / (double) FILAS));

        double anchoAsientos = columnas * PASO_ASIENTO - SEPARACION_ASIENTO;
        double anchoGeneral = Math.max(260, anchoAsientos);
        double yGeneral = INICIO_Y + FILAS * PASO_ASIENTO + 35;
        double anchoCanvas = Math.max(750, INICIO_X + anchoGeneral + 50);
        double altoCanvas = Math.max(600, yGeneral + ALTO_ZONA_GENERAL + 45);

        canvasMapa.setWidth(anchoCanvas);
        canvasMapa.setHeight(altoCanvas);

        GraphicsContext gc = canvasMapa.getGraphicsContext2D();
        gc.clearRect(0, 0, anchoCanvas, altoCanvas);
        posicionesAsientos.clear();

        dibujarLeyenda(gc);
        gc.setFill(Color.BLACK);
        gc.fillText("ZONA NUMERADA", INICIO_X, 45);

        for (int i = 0; i < asientosNumerados.size(); i++) {
            Asiento asiento = asientosNumerados.get(i);
            int fila = i % FILAS;
            int columna = i / FILAS;
            double x = INICIO_X + columna * PASO_ASIENTO;
            double y = INICIO_Y + fila * PASO_ASIENTO;

            posicionesAsientos.put(asiento.id(), new Posicion(x, y));
            gc.setFill(obtenerColorAsiento(asiento));
            gc.fillOval(x, y, TAMANO_ASIENTO, TAMANO_ASIENTO);
            gc.setStroke(Color.DARKGREEN);
            gc.strokeOval(x, y, TAMANO_ASIENTO, TAMANO_ASIENTO);
        }

        dibujarZonaGeneral(gc, INICIO_X, yGeneral, anchoGeneral);
    }

    // Dibujar el único rectángulo de la zona general.
    private void dibujarZonaGeneral(GraphicsContext gc, double x, double y, double ancho) {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(x, y, ancho, ALTO_ZONA_GENERAL);
        gc.setStroke(Color.STEELBLUE);
        gc.strokeRect(x, y, ancho, ALTO_ZONA_GENERAL);

        gc.setFill(Color.BLACK);
        gc.fillText("ZONA GENERAL", x + ancho / 2 - 45, y + 38);

        String informacion = idsZonasGenerales.isEmpty()
                ? "No configurada en la base de datos"
                : "Aforo: " + aforoGeneral + " | Ocupación: " + ocupadosGeneral;

        gc.fillText(informacion, x + ancho / 2 - informacion.length() * 2.8, y + 65);
    }

    // Dibujar la leyenda de los estados de los asientos.
    private void dibujarLeyenda(GraphicsContext gc) {
        double y = 20;
        double tamano = 12;

        gc.setFill(Color.LIGHTGREEN);
        gc.fillOval(50, y, tamano, tamano);
        gc.setFill(Color.BLACK);
        gc.fillText("Disponible", 67, y + 10);

        gc.setFill(Color.INDIANRED);
        gc.fillOval(180, y, tamano, tamano);
        gc.setFill(Color.BLACK);
        gc.fillText("Vendido", 197, y + 10);

        gc.setFill(Color.GRAY);
        gc.fillOval(300, y, tamano, tamano);
        gc.setFill(Color.BLACK);
        gc.fillText("Bloqueado", 317, y + 10);
    }

    // Buscar el asiento situado en las coordenadas del clic.
    private void seleccionarAsiento(double x, double y) {
        for (Asiento asiento : asientos) {
            Posicion posicion = posicionesAsientos.get(asiento.id());
            if (posicion == null) {
                continue;
            }

            double centroX = posicion.x() + TAMANO_ASIENTO / 2;
            double centroY = posicion.y() + TAMANO_ASIENTO / 2;
            double dx = x - centroX;
            double dy = y - centroY;

            if (dx * dx + dy * dy <= Math.pow(TAMANO_ASIENTO / 2, 2)) {
                lblAsiento.setText(asiento.id());
                lblZona.setText("Zona numerada");
                limpiarDatosVenta();

                if (esBloqueado(asiento)) {
                    lblEstado.setText("Bloqueado");
                } else if (asiento.vendido()) {
                    lblEstado.setText("Vendido");
                    mostrarVenta(asiento);
                } else {
                    lblEstado.setText("Disponible");
                }
                return;
            }
        }
    }

    // Ordenar los asientos para que la distribución sea estable.
    private List<Asiento> obtenerAsientosNumerados() {
        return asientos.stream()
                .filter(asiento -> idsZonasNumeradas.contains(asiento.idZona()))
                .sorted(Comparator.comparing(Asiento::idZona)
                        .thenComparingInt(Asiento::fila)
                        .thenComparingInt(Asiento::columna)
                        .thenComparing(Asiento::id))
                .toList();
    }

    // Actualizar la ocupación mostrada para los dos tipos de zona.
    private void actualizarOcupacionZonas() {
        lblOcupacionNumerada.setText(aforoNumerada > 0
                ? "Zona numerada: " + ocupadosNumerada + "/" + aforoNumerada
                : "Zona numerada: No disponible");

        lblOcupacionGeneral.setText(!idsZonasGenerales.isEmpty()
                ? "Zona general: " + ocupadosGeneral + "/" + aforoGeneral
                : "Zona general: No configurada en BBDD");
    }

    // Limpiar los datos asociados a la venta, sin borrar el asiento seleccionado.
    private void limpiarDatosVenta() {
        lblLocalizador.setText("-");
        lblComprador.setText("-");
        lblCorreo.setText("-");
        lblImporte.setText("-");
    }

    // Mostrar los datos de la venta asociada al asiento.
    private void mostrarVenta(Asiento asiento) {
        Venta venta = asiento.venta();
        if (venta == null) {
            return;
        }

        lblLocalizador.setText(venta.localizador());
        lblComprador.setText(venta.comprador());
        lblCorreo.setText(venta.correo());
        lblImporte.setText(String.format("%.2f", venta.importe()).replace('.', ',') + " €");
    }

    // Comprobar si el asiento está bloqueado.
    private boolean esBloqueado(Asiento asiento) {
        return "bloquejat".equalsIgnoreCase(asiento.estat())
                || "bloqueado".equalsIgnoreCase(asiento.estat());
    }

    // Obtener el color según el estado del asiento.
    private Color obtenerColorAsiento(Asiento asiento) {
        if (esBloqueado(asiento)) {
            return Color.GRAY;
        }
        return asiento.vendido() ? Color.INDIANRED : Color.LIGHTGREEN;
    }

    // Limpiar la selección y los datos de la venta.
    private void limpiarSeleccion() {
        lblAsiento.setText("-");
        lblZona.setText("-");
        lblEstado.setText("-");
        limpiarDatosVenta();
    }

    // Limpiar los datos del mapa anterior.
    private void limpiarMapa() {
        asientos.clear();
        posicionesAsientos.clear();
        canvasMapa.getGraphicsContext2D().clearRect(
                0, 0, canvasMapa.getWidth(), canvasMapa.getHeight()
        );
    }

    private void configurarTablaEntradasInicial() { 

        localizadorEntradaMapa.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(dato.getValue().localizador()));

        nombreEntradaMapa.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(dato.getValue().comprador()));

        tipoEntradaMapa.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(dato.getValue().tipoEntrada()));

        asientoEntradaMapa.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(dato.getValue().asiento()));

        estadoEntradaMapa.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(dato.getValue().estadoEntrada()));

        fechaEntradaMapa.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(dato.getValue().fechaHoraCompra()));
    }

    private void cargarEntradasVendidas(List<EntradaVendida> entradas) {
        tableEntradasVendidas.getItems().clear();

        if (entradas != null) {
            tableEntradasVendidas.getItems().setAll(entradas);
        }
    }

    // Records utilizados directamente por Jackson para convertir el JSON.
    public record RespuestaMapa(
        String type,
        String esdeveniment,
        String recinte,
        List<ZonaMapa> zonas,
        List<Asiento> seients,
        List<EntradaVendida> entradesVenudes
    ) {
    }

    public record ZonaMapa(
        String id,
        String nombre,
        String tipo,
        int aforament,
        int ocupacion
    ) {
    }

    public record Asiento(
        String id,
        int fila,
        int columna,
        String idZona,
        String nombreZona,
        String estat,
        boolean vendido,
        Venta venta
    ) {
    }

    public record Venta(
        String localizador,
        String idVenta,
        String comprador,
        String correo,
        double importe,
        String estadoEntrada
    ) {
    }

    public record EntradaVendida(
        String fechaHoraCompra,
        String localizador,
        String tipoEntrada,
        String asiento,
        String comprador,
        String estadoEntrada
    ) {
    }

    public record Posicion(double x, double y) {
    }
}
