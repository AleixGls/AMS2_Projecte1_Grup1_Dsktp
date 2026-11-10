package grup1.desktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import tools.jackson.databind.ObjectMapper;

public class CtrlMapa {

    // Datos del evento y del recinto seleccionados
    private CtrlLlistatVendes.Esdeveniment esdeveniment;
    private CtrlLlistatVendes.Recinte recinto;

    // En la vista se muestra una sola zona numerada y una sola zona general.
    // La BBDD puede tener varias zonas físicas del mismo tipo; se agrupan visualmente.
    private final Set<String> idsZonasNumeradas = new LinkedHashSet<>();
    private final Set<String> idsZonasGenerales = new LinkedHashSet<>();

    private final List<Asiento> asientos = new ArrayList<>();
    private final Map<String, Posicion> posicionesAsientos = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private int aforoNumerada;
    private int ocupadosNumerada;
    private int aforoGeneral;
    private int ocupadosGeneral;

    // Distribución visual: siempre 5 filas y se añaden columnas hacia la derecha.
    private static final int FILAS = 5;
    private static final double TAMANO_ASIENTO = 18;
    private static final double SEPARACION_ASIENTO = 12;
    private static final double PASO_ASIENTO = TAMANO_ASIENTO + SEPARACION_ASIENTO;
    private static final double INICIO_X = 50;
    private static final double INICIO_Y = 55;
    private static final double ALTO_ZONA_GENERAL = 100;

    @FXML
    private Label lblNombreEvento;

    @FXML
    private Label lblNombreRecinto;

    @FXML
    private Canvas canvasMapa;

    @FXML
    private VBox contenedorOcupacionZonas;

    @FXML
    private Label lblAsiento;

    @FXML
    private Label lblZona;

    @FXML
    private Label lblLocalizador;

    @FXML
    private Label lblComprador;

    @FXML
    private Label lblCorreo;

    @FXML
    private Label lblImporte;

    @FXML
    private Label lblEstado;

    @FXML
    private Label lblOcupacionNumerada;

    @FXML
    private Label lblOcupacionGeneral;

    @FXML
    private void initialize() {
        canvasMapa.setOnMouseClicked(event ->
                seleccionarAsiento(event.getX(), event.getY())
        );
    }

    // Recibir el evento y el recinto seleccionado desde el listado.
    public void setEsdeveniment(
            CtrlLlistatVendes.Esdeveniment esdeveniment,
            CtrlLlistatVendes.Recinte recinto
    ) {
        this.esdeveniment = esdeveniment;
        this.recinto = recinto;

        lblNombreEvento.setText(esdeveniment.nom());
        lblNombreRecinto.setText(recinto.nom());

        limpiarMapa();

        // Cargar las zonas del catálogo como valor inicial.
        // cargarMapa() las sustituirá por las zonas enviadas por el servidor.
        idsZonasNumeradas.clear();
        idsZonasGenerales.clear();
        aforoNumerada = 0;
        aforoGeneral = 0;

        if (recinto.zones() != null) {
            for (CtrlLlistatVendes.Zona zona : recinto.zones()) {
                registrarZona(
                        zona.id(),
                        zona.tipus(),
                        zona.aforament(),
                        0
                );
            }
        }

        actualizarOcupacionZonas();
        limpiarDetalleVenta();
    }

    // Procesar la respuesta JSON enviada por el servidor.
    public void cargarMapa(String json) {
        try {
            Map<?, ?> respuesta = objectMapper.readValue(json, Map.class);

            if (!"mapa".equals(respuesta.get("type"))) {
                throw new IllegalArgumentException(
                        "La respuesta recibida no corresponde a un mapa."
                );
            }

            asientos.clear();
            posicionesAsientos.clear();

            // Si el servidor incluye las zonas, esta información es la fuente principal.
            Object datosZonas = respuesta.get("zonas");

            if (datosZonas instanceof List<?> zonas) {
                idsZonasNumeradas.clear();
                idsZonasGenerales.clear();
                aforoNumerada = 0;
                ocupadosNumerada = 0;
                aforoGeneral = 0;
                ocupadosGeneral = 0;

                for (Object objeto : zonas) {
                    if (!(objeto instanceof Map<?, ?> zona)) {
                        continue;
                    }

                    registrarZona(
                            obtenerTexto(zona.get("id"), ""),
                            obtenerTexto(zona.get("tipo"), ""),
                            obtenerEntero(zona.get("aforament")),
                            obtenerEntero(zona.get("ocupacion"))
                    );
                }
            }

            // Obtener los asientos. El servidor usa la clave "seients".
            Object datosAsientos = respuesta.get("seients");

            if (datosAsientos == null) {
                datosAsientos = respuesta.get("asientos");
            }

            if (datosAsientos instanceof List<?> listaAsientos) {
                Set<String> idsProcesados = new HashSet<>();

                for (Object objeto : listaAsientos) {
                    if (!(objeto instanceof Map<?, ?> datos)) {
                        continue;
                    }

                    String id = obtenerTexto(datos.get("id"), "-");
                    String idZona = obtenerTexto(datos.get("idZona"), "");

                    // Solo se muestran asientos de zonas numeradas.
                    if (!idsZonasNumeradas.contains(idZona)) {
                        continue;
                    }

                    // Evitar que una misma fila del servidor duplique un asiento.
                    if (!idsProcesados.add(id)) {
                        System.out.println("Asiento repetido recibido: " + id);
                        continue;
                    }

                    Venta venta = null;

                    if (datos.get("venta") instanceof Map<?, ?> datosVenta) {
                        venta = new Venta(
                                obtenerTexto(datosVenta.get("localizador"), "-"),
                                obtenerTexto(datosVenta.get("comprador"), "-"),
                                obtenerTexto(datosVenta.get("correo"), "-"),
                                obtenerDecimal(datosVenta.get("importe"))
                        );
                    }

                    asientos.add(new Asiento(
                            id,
                            obtenerEntero(datos.get("fila")),
                            obtenerEntero(datos.get("columna")),
                            idZona,
                            obtenerTexto(datos.get("estat"), "disponible"),
                            Boolean.TRUE.equals(datos.get("vendido")),
                            venta
                    ));
                }
            }

            dibujarMapa();
            actualizarOcupacionZonas();
            limpiarDetalleVenta();

            System.out.println("--------------------------------");
            System.out.println("Mapa: " +
                    (esdeveniment != null ? esdeveniment.id() : "sin evento"));
            System.out.println("Zonas numeradas agrupadas: " + idsZonasNumeradas.size());
            System.out.println("Zonas generales encontradas: " + idsZonasGenerales.size());
            System.out.println("Asientos dibujados: " + asientos.size());
            System.out.println("Aforo numerado total: " + aforoNumerada);
            System.out.println("Zona general configurada: " + !idsZonasGenerales.isEmpty());
            System.out.println("--------------------------------");

        } catch (Exception e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al cargar el mapa");
            alerta.setHeaderText("No se ha podido cargar el mapa del recinto");
            alerta.setContentText(e.getMessage());
            alerta.show();
        }
    }

    // Registrar y acumular los datos de una zona según su tipo.
    private void registrarZona(
            String id,
            String tipo,
            int aforo,
            int ocupacion
    ) {
        if (id == null || id.isBlank()) {
            return;
        }

        if ("numerada".equalsIgnoreCase(tipo.trim())) {
            if (idsZonasNumeradas.add(id)) {
                aforoNumerada += aforo;
                ocupadosNumerada += ocupacion;
            }
        } else if ("general".equalsIgnoreCase(tipo.trim())) {
            if (idsZonasGenerales.add(id)) {
                aforoGeneral += aforo;
                ocupadosGeneral += ocupacion;
            }
        }
    }

    // Dibujar toda la zona numerada como una única cuadrícula de cinco filas.
    private void dibujarMapa() {
        List<Asiento> asientosNumerados = obtenerAsientosNumerados();

        int columnas = Math.max(
                1,
                (int) Math.ceil(asientosNumerados.size() / (double) FILAS)
        );

        double anchoAsientos = columnas * PASO_ASIENTO - SEPARACION_ASIENTO;
        double anchoGeneral = Math.max(260, anchoAsientos);
        double yGeneral = INICIO_Y + FILAS * PASO_ASIENTO + 35;

        // El Canvas se adapta al número de columnas. El ScrollPane permite desplazarse.
        double anchoCanvas = Math.max(750, INICIO_X + anchoGeneral + 50);
        double altoCanvas = Math.max(
                600,
                yGeneral + ALTO_ZONA_GENERAL + 45
        );

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

            // Cada cinco asientos se crea una nueva columna a la derecha.
            int filaVisual = i % FILAS;
            int columnaVisual = i / FILAS;

            double x = INICIO_X + columnaVisual * PASO_ASIENTO;
            double y = INICIO_Y + filaVisual * PASO_ASIENTO;

            posicionesAsientos.put(asiento.id(), new Posicion(x, y));

            gc.setFill(obtenerColorAsiento(asiento));
            gc.fillOval(x, y, TAMANO_ASIENTO, TAMANO_ASIENTO);

            gc.setStroke(Color.DARKGREEN);
            gc.strokeOval(x, y, TAMANO_ASIENTO, TAMANO_ASIENTO);
        }

        // Siempre se dibuja un único rectángulo general debajo de los asientos.
        dibujarZonaGeneral(gc, INICIO_X, yGeneral, anchoGeneral);
    }

    // Dibujar un único rectángulo para la zona general, sin asientos dentro.
    private void dibujarZonaGeneral(
            GraphicsContext gc,
            double x,
            double y,
            double ancho
    ) {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(x, y, ancho, ALTO_ZONA_GENERAL);

        gc.setStroke(Color.STEELBLUE);
        gc.strokeRect(x, y, ancho, ALTO_ZONA_GENERAL);

        gc.setFill(Color.BLACK);
        gc.fillText(
                "ZONA GENERAL",
                x + ancho / 2 - 45,
                y + 38
        );

        String informacion;

        if (idsZonasGenerales.isEmpty()) {
            informacion = "No configurada en la base de datos";
        } else {
            informacion = "Aforo: " + aforoGeneral
                    + " | Ocupación: " + ocupadosGeneral;
        }

        gc.fillText(
                informacion,
                x + ancho / 2 - informacion.length() * 2.8,
                y + 65
        );
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

    // Seleccionar el asiento usando exactamente las posiciones con las que se dibujó.
    private void seleccionarAsiento(double x, double y) {
        for (Asiento asiento : asientos) {
            Posicion posicion = posicionesAsientos.get(asiento.id());

            if (posicion == null) {
                continue;
            }

            double centroX = posicion.x() + TAMANO_ASIENTO / 2;
            double centroY = posicion.y() + TAMANO_ASIENTO / 2;

            double distanciaX = x - centroX;
            double distanciaY = y - centroY;

            if (distanciaX * distanciaX + distanciaY * distanciaY
                    <= Math.pow(TAMANO_ASIENTO / 2, 2)) {

                lblAsiento.setText(asiento.id());
                lblZona.setText("Zona numerada");
                limpiarDetalleVenta();

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

    // Obtener todos los asientos numerados y ordenarlos de forma estable.
    private List<Asiento> obtenerAsientosNumerados() {
        return asientos.stream()
                .filter(a -> idsZonasNumeradas.contains(a.idZona()))
                .sorted(
                        Comparator.comparing(Asiento::idZona)
                                .thenComparingInt(Asiento::fila)
                                .thenComparingInt(Asiento::columna)
                                .thenComparing(Asiento::id)
                )
                .toList();
    }

    // Actualizar la ocupación de las dos zonas mostradas.
    private void actualizarOcupacionZonas() {
        if (aforoNumerada > 0) {
            lblOcupacionNumerada.setText(
                    "Zona numerada: " + ocupadosNumerada + "/" + aforoNumerada
            );
        } else {
            lblOcupacionNumerada.setText("Zona numerada: No disponible");
        }

        if (!idsZonasGenerales.isEmpty()) {
            lblOcupacionGeneral.setText(
                    "Zona general: " + ocupadosGeneral + "/" + aforoGeneral
            );
        } else {
            lblOcupacionGeneral.setText(
                    "Zona general: No configurada en BBDD"
            );
        }
    }

    private void limpiarDetalleVenta() {
        lblAsiento.setText("-");
        lblZona.setText("-");
        lblLocalizador.setText("-");
        lblComprador.setText("-");
        lblCorreo.setText("-");
        lblImporte.setText("-");
        lblEstado.setText("-");
    }

    private void mostrarVenta(Asiento asiento) {
        Venta venta = asiento.venta();

        if (venta == null) {
            return;
        }

        lblLocalizador.setText(venta.localizador());
        lblComprador.setText(venta.comprador());
        lblCorreo.setText(venta.correo());

        lblImporte.setText(
                String.format("%.2f", venta.importe()).replace('.', ',') + " €"
        );
    }

    private boolean esBloqueado(Asiento asiento) {
        return "bloquejat".equalsIgnoreCase(asiento.estat())
                || "bloqueado".equalsIgnoreCase(asiento.estat());
    }

    private Color obtenerColorAsiento(Asiento asiento) {
        if (esBloqueado(asiento)) {
            return Color.GRAY;
        }

        if (asiento.vendido()) {
            return Color.INDIANRED;
        }

        return Color.LIGHTGREEN;
    }

    private int obtenerEntero(Object valor) {
        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        if (valor != null) {
            try {
                return Integer.parseInt(valor.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    private double obtenerDecimal(Object valor) {
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }

        if (valor != null) {
            try {
                return Double.parseDouble(valor.toString());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        return 0.0;
    }

    private String obtenerTexto(Object valor, String valorPorDefecto) {
        return valor == null ? valorPorDefecto : String.valueOf(valor);
    }

    private void limpiarMapa() {
        asientos.clear();
        posicionesAsientos.clear();
        canvasMapa.getGraphicsContext2D().clearRect(
                0, 0, canvasMapa.getWidth(), canvasMapa.getHeight()
        );
    }

    private record Posicion(double x, double y) {
    }

    private record Asiento(
            String id,
            int fila,
            int columna,
            String idZona,
            String estat,
            boolean vendido,
            Venta venta
    ) {
    }

    private record Venta(
            String localizador,
            String comprador,
            String correo,
            double importe
    ) {
    }
}
