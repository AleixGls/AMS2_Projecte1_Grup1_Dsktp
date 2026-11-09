package grup1.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import tools.jackson.databind.ObjectMapper;

public class CtrlMapa {

    // Evento seleccionado desde el listado
    private CtrlLlistatVendes.Esdeveniment esdeveniment;

    // Recinto seleccionado desde el listado
    private CtrlLlistatVendes.Recinte recinto;

    // Zonas del recinto seleccionado
    private CtrlLlistatVendes.Zona zonaNumerada;
    private CtrlLlistatVendes.Zona zonaGeneral;

    // Lista de asientos recibidos del servidor
    private final List<Asiento> asientos = new ArrayList<>();

    // Convertir los datos JSON recibidos del servidor
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Ocupación de cada zona
    private int ocupadosNumerada;
    private int ocupadosGeneral;

    // Tamaño de los asientos
    private static final double TAMANO_ASIENTO = 18;

    private static final double SEPARACION_ASIENTO = 12;
    private static final double INICIO_X = 50;
    private static final double INICIO_Y = 50;

    // Todas las salas tendrán 5 filas
    private static final int FILAS = 5;

    @FXML
    private Label lblNombreEvento;

    @FXML
    private Label lblNombreRecinto;

    // Canvas donde representaremos los asientos
    @FXML
    private Canvas canvasMapa;

    // Contenedor de ocupación de las zonas
    @FXML
    private VBox contenedorOcupacionZonas;

    // Datos del detalle de venta
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

        // Detectar los clics sobre el Canvas
        canvasMapa.setOnMouseClicked(event -> {
            seleccionarAsiento(event.getX(), event.getY());
        });

    }

    // Dibujar los asientos recibidos del servidor
    private void dibujarMapa() {

        GraphicsContext gc = canvasMapa.getGraphicsContext2D();

        gc.clearRect(0, 0, canvasMapa.getWidth(), canvasMapa.getHeight());

        dibujarLeyenda(gc);

        // Dibujar cada asiento recibido del servidor
        for (Asiento asiento : asientos) {

            double x = INICIO_X +
                    (asiento.columna() - 1) *
                    (TAMANO_ASIENTO + SEPARACION_ASIENTO);

            double y = INICIO_Y +
                    (asiento.fila() - 1) *
                    (TAMANO_ASIENTO + SEPARACION_ASIENTO);

            gc.setFill(obtenerColorAsiento(asiento));
            gc.fillOval(x, y, TAMANO_ASIENTO, TAMANO_ASIENTO);

            gc.setStroke(Color.DARKGREEN);
            gc.strokeOval(x, y, TAMANO_ASIENTO, TAMANO_ASIENTO);
        }

        // Dibujar la zona general solamente si existe
        if (zonaGeneral != null) {

            dibujarZonaGeneral(
                    gc,
                    obtenerColumnas(),
                    TAMANO_ASIENTO,
                    SEPARACION_ASIENTO,
                    INICIO_X,
                    INICIO_Y
            );
        }

    }

    // Dibujar la zona general del recinto
    private void dibujarZonaGeneral(
            GraphicsContext gc,
            int columnas,
            double tamaño,
            double separacion,
            double inicioX,
            double inicioY
    ) {

        // Calcular el ancho de la zona según las columnas de la sala
        double ancho = columnas * (tamaño + separacion) - separacion;

        // Colocar la zona debajo de los asientos
        double x = inicioX;
        double y = inicioY + FILAS * (tamaño + separacion) + 10;

        double alto = 100;

        // Dibujar el rectángulo
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(x, y, ancho, alto);

        gc.setStroke(Color.STEELBLUE);
        gc.strokeRect(x, y, ancho, alto);

        // Mostrar información de la zona
        gc.setFill(Color.BLACK);

        gc.fillText(
                zonaGeneral.nom(),
                x + ancho / 2 - 45,
                y + 40
        );

        gc.fillText(
                "Aforo: " + zonaGeneral.aforament(),
                x + ancho / 2 - 35,
                y + 65
        );

    }

    // Dibujar la leyenda de los estados de los asientos
    private void dibujarLeyenda(GraphicsContext gc) {

        double y = 20;
        double tamaño = 12;

        // Disponible
        gc.setFill(Color.LIGHTGREEN);
        gc.fillOval(50, y, tamaño, tamaño);

        gc.setFill(Color.BLACK);
        gc.fillText("Disponible", 67, y + 10);

        // Vendido
        gc.setFill(Color.INDIANRED);
        gc.fillOval(180, y, tamaño, tamaño);

        gc.setFill(Color.BLACK);
        gc.fillText("Vendido", 197, y + 10);

        // Bloqueado
        gc.setFill(Color.GRAY);
        gc.fillOval(300, y, tamaño, tamaño);

        gc.setFill(Color.BLACK);
        gc.fillText("Bloqueado", 317, y + 10);

    }

    // Identificar el asiento seleccionado
    private void seleccionarAsiento(double x, double y) {

        // Recorrer todos los asientos
        for (Asiento asiento : asientos) {

            // Calcular la posición del asiento
            double xAsiento = INICIO_X +
                    (asiento.columna() - 1) *
                    (TAMANO_ASIENTO + SEPARACION_ASIENTO);

            double yAsiento = INICIO_Y +
                    (asiento.fila() - 1) *
                    (TAMANO_ASIENTO + SEPARACION_ASIENTO);

            // Calcular el centro del asiento
            double centroX = xAsiento + TAMANO_ASIENTO / 2;
            double centroY = yAsiento + TAMANO_ASIENTO / 2;

            // Calcular la distancia entre el clic y el centro del asiento
            double distanciaX = x - centroX;
            double distanciaY = y - centroY;

            // Comprobar si el clic está dentro del asiento
            if (distanciaX * distanciaX + distanciaY * distanciaY
                    <= Math.pow(TAMANO_ASIENTO / 2, 2)) {

                // Mostrar los datos básicos del asiento
                lblAsiento.setText(asiento.id());
                lblZona.setText(obtenerNombreZona(asiento.idZona()));

                // Limpiar los datos de la venta anterior
                limpiarDetalleVenta();

                // Comprobar si el asiento está bloqueado
                if (esBloqueado(asiento)) {

                    lblEstado.setText("Bloqueado");

                // Comprobar si el asiento está vendido
                } else if (asiento.vendido()) {

                    lblEstado.setText("Vendido");

                    // Mostrar los datos de la venta
                    mostrarVenta(asiento);

                // Si no está bloqueado ni vendido, está disponible
                } else {

                    lblEstado.setText("Disponible");

                }

                return;

            }

        }

        System.out.println("No se ha seleccionado ningún asiento.");

    }

    // Actualizar la ocupación de las zonas
    private void actualizarOcupacionZonas() {

        // Mostrar ocupación de la zona numerada
        if (zonaNumerada != null) {

            lblOcupacionNumerada.setText(
                    zonaNumerada.nom() + ": " +
                    ocupadosNumerada + "/" + zonaNumerada.aforament()
            );

        } else {

            lblOcupacionNumerada.setText("Zona numerada: No disponible");
        }

        // Mostrar ocupación de la zona general
        if (zonaGeneral != null) {

            lblOcupacionGeneral.setText(
                    zonaGeneral.nom() + ": " +
                    ocupadosGeneral + "/" + zonaGeneral.aforament()
            );

        } else {

            lblOcupacionGeneral.setText("Zona general: No disponible");
        }

    }

    // Limpiar los datos de la venta
    private void limpiarDetalleVenta() {

        lblLocalizador.setText("-");
        lblComprador.setText("-");
        lblCorreo.setText("-");
        lblImporte.setText("-");

    }

    // Mostrar los datos de una venta
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

    // Obtener el nombre de una zona a partir de su ID
    private String obtenerNombreZona(String idZona) {

        for (CtrlLlistatVendes.Zona zona : recinto.zones()) {

            if (zona.id().equals(idZona)) {
                return zona.nom();
            }

        }

        return idZona;

    }

    // Comprobar si un asiento está bloqueado
    private boolean esBloqueado(Asiento asiento) {

        return "bloquejat".equalsIgnoreCase(asiento.estat())
                || "bloqueado".equalsIgnoreCase(asiento.estat());

    }

    // Obtener el color correspondiente al estado del asiento
    private Color obtenerColorAsiento(Asiento asiento) {

        if (esBloqueado(asiento)) {
            return Color.GRAY;
        }

        if (asiento.vendido()) {
            return Color.INDIANRED;
        }

        return Color.LIGHTGREEN;

    }

    // Obtener el número de columnas de la sala
    private int obtenerColumnas() {

        // Usar las columnas reales de los asientos recibidos
        int columnas = asientos.stream()
                .mapToInt(Asiento::columna)
                .max()
                .orElse(0);

        // Si no hay asientos, calcular las columnas según el aforo
        if (columnas == 0 && zonaNumerada != null) {
            columnas = (int) Math.ceil(
                    (double) zonaNumerada.aforament() / FILAS
            );
        }

        return Math.max(1, columnas);
    }

    // Recibir el evento y el recinto seleccionado
    public void setEsdeveniment(
            CtrlLlistatVendes.Esdeveniment esdeveniment,
            CtrlLlistatVendes.Recinte recinto
    ) {

        this.esdeveniment = esdeveniment;
        this.recinto = recinto;

        // Mostrar los datos del evento y del recinto
        lblNombreEvento.setText(esdeveniment.nom());
        lblNombreRecinto.setText(recinto.nom());

        // Identificar las zonas del recinto
        zonaNumerada = null;
        zonaGeneral = null;

        System.out.println("================================");
        System.out.println("MAPA: " + esdeveniment.id());
        System.out.println("RECINTO: " + recinto.id() + " - " + recinto.nom());

        if (recinto.zones() == null || recinto.zones().isEmpty()) {
            throw new IllegalStateException(
                    "El recinto no tiene zonas registradas."
            );
        }

        for (CtrlLlistatVendes.Zona zona : recinto.zones()) {

            System.out.println(
                    "Zona: " + zona.id() +
                    " - " + zona.nom() +
                    " - Tipo: " + zona.tipus()
            );

            if ("numerada".equalsIgnoreCase(zona.tipus())
                    && zonaNumerada == null) {

                zonaNumerada = zona;

            } else if ("general".equalsIgnoreCase(zona.tipus())
                    && zonaGeneral == null) {

                zonaGeneral = zona;
            }
        }

        // Informar de las zonas disponibles
        System.out.println("Zona numerada: " +
                (zonaNumerada != null ? zonaNumerada.nom() : "No disponible"));

        System.out.println("Zona general: " +
                (zonaGeneral != null ? zonaGeneral.nom() : "No disponible"));

        System.out.println("================================");

        // Limpiar datos anteriores
        asientos.clear();

        ocupadosNumerada = 0;
        ocupadosGeneral = 0;

        lblAsiento.setText("-");
        lblZona.setText("-");
        lblEstado.setText("-");

        limpiarDetalleVenta();

    }

    // Recibir y procesar los datos del servidor
    public void cargarMapa(String json) {

        try {

            Map<?, ?> respuesta = objectMapper.readValue(json, Map.class);

            // Comprobar que la respuesta corresponde a un mapa
            if (!"mapa".equals(respuesta.get("type"))) {

                throw new IllegalArgumentException(
                        "La respuesta recibida no corresponde a un mapa."
                );

            }

            // Limpiar los datos anteriores
            asientos.clear();

            ocupadosNumerada = 0;
            ocupadosGeneral = 0;

            // Obtener la ocupación de las zonas
            Object datosZonas = respuesta.get("zonas");

            if (datosZonas instanceof List<?> zonas) {

                for (Object objeto : zonas) {

                    if (!(objeto instanceof Map<?, ?> zona)) {
                        continue;
                    }

                    String idZona = String.valueOf(zona.get("id"));

                    int ocupacion = obtenerEntero(zona.get("ocupacion"));

                    // Comprobar la ocupación de la zona numerada
                    if (zonaNumerada != null && zonaNumerada.id().equals(idZona)) {
                        ocupadosNumerada = ocupacion;
                    }

                    // Comprobar la ocupación de la zona general
                    if (zonaGeneral != null && zonaGeneral.id().equals(idZona)) {
                        ocupadosGeneral = ocupacion;
                    }

                }

            }

            // Obtener los asientos recibidos del servidor
            Object datosAsientos = respuesta.get("seients");

            // Compatibilidad por si el servidor utiliza la clave "asientos"
            if (datosAsientos == null) {
                datosAsientos = respuesta.get("asientos");
            }

            if (datosAsientos instanceof List<?> listaAsientos) {

                for (Object objeto : listaAsientos) {

                    if (!(objeto instanceof Map<?, ?> datos)) {
                        continue;
                    }

                    Venta venta = null;

                    // Obtener la venta si el asiento está vendido
                    if (datos.get("venta") instanceof Map<?, ?> datosVenta) {

                        venta = new Venta(
                                obtenerTexto(datosVenta.get("localizador"), "-"),
                                obtenerTexto(datosVenta.get("comprador"), "-"),
                                obtenerTexto(datosVenta.get("correo"), "-"),
                                obtenerDecimal(datosVenta.get("importe"))
                        );

                    }

                    // Crear el asiento con los datos recibidos
                    Asiento asiento = new Asiento(
                            obtenerTexto(datos.get("id"), "-"),
                            obtenerEntero(datos.get("fila")),
                            obtenerEntero(datos.get("columna")),
                            obtenerTexto(datos.get("idZona"), ""),
                            obtenerTexto(datos.get("estat"), "lliure"),
                            Boolean.TRUE.equals(datos.get("vendido")),
                            venta
                    );

                    asientos.add(asiento);

                }

            }

            // Actualizar la interfaz
            dibujarMapa();
            actualizarOcupacionZonas();

            // Limpiar el detalle de venta
            lblAsiento.setText("-");
            lblZona.setText("-");
            lblEstado.setText("-");

            limpiarDetalleVenta();

            System.out.println("Mapa cargado correctamente.");
            System.out.println("Asientos recibidos: " + asientos.size());

        } catch (Exception e) {

            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);

            alerta.setTitle("Error al cargar el mapa");
            alerta.setHeaderText("No se ha podido cargar el mapa del recinto");
            alerta.setContentText(e.getMessage());

            alerta.show();

        }

    }

    // Convertir un valor JSON a entero
    private int obtenerEntero(Object valor) {

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        if (valor != null) {

            try {
                return Integer.parseInt(valor.toString());
            } catch (NumberFormatException e) {
                return 0;
            }

        }

        return 0;

    }

    // Convertir un valor JSON a decimal
    private double obtenerDecimal(Object valor) {

        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }

        if (valor != null) {

            try {
                return Double.parseDouble(valor.toString());
            } catch (NumberFormatException e) {
                return 0.0;
            }

        }

        return 0.0;

    }

    // Obtener un texto del JSON evitando valores nulos
    private String obtenerTexto(Object valor, String valorPorDefecto) {

        if (valor == null) {
            return valorPorDefecto;
        }

        return String.valueOf(valor);

    }

    // Estructura para almacenar la información de cada asiento
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

    // Estructura para almacenar la información de una venta
    private record Venta(
            String localizador,
            String comprador,
            String correo,
            double importe
    ) {
    }

}