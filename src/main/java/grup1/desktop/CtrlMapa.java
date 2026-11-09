package grup1.desktop;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CtrlMapa {

    // Evento seleccionado desde el listado
    private CtrlLlistatVendes.Esdeveniment esdeveniment;

    // Recinto seleccionado desde el listado
    private CtrlLlistatVendes.Recinte recinto;

    // Zonas del recinto seleccionado
    private CtrlLlistatVendes.Zona zonaNumerada;
    private CtrlLlistatVendes.Zona zonaGeneral;

    // Lista de asientos del recinto
    private final List<Asiento> asientos = new ArrayList<>();

    // Lista de zonas del recinto
    private final List<ZonaMapa> zonasMapa = new ArrayList<>();

    // Tamaño de los asientos
    private static final double TAMANO_ASIENTO = 18;

    private static final double SEPARACION_ASIENTO = 12;
    private static final double INICIO_X = 50;
    private static final double INICIO_Y = 50;

    // Todas las salas tendrán 5 filas
    private final int FILAS = 5;

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
        //dibujarAsientosPrueba();

        //actualizarOcupacionZonas();

        // Detectar los clics sobre el Canvas
        canvasMapa.setOnMouseClicked(event -> {
            seleccionarAsiento(event.getX(), event.getY());

        });

    }

    // Dibujar asientos de prueba en el Canvas
    private void dibujarAsientosPrueba() {
        GraphicsContext gc = canvasMapa.getGraphicsContext2D();

        // Limpiar el Canvas
        gc.clearRect(0, 0, canvasMapa.getWidth(), canvasMapa.getHeight());
        dibujarLeyenda(gc);

        // Limpiar la lista de asientos
        asientos.clear();

        int columnas = obtenerColumnas();

        double tamaño = TAMANO_ASIENTO;
        double separacion = SEPARACION_ASIENTO;

        double inicioX = INICIO_X;
        double inicioY = INICIO_Y;

        for (int fila = 0; fila < FILAS; fila++) {
            for (int columna = 0; columna < columnas; columna++) {

                double x = inicioX + columna * (tamaño + separacion);
                double y = inicioY + fila * (tamaño + separacion);

                String id = "F" + (fila + 1) + "-C" + (columna + 1);

                // De momento todos los asientos estarán disponibles
                // Datos ficticios para probar los diferentes estados
                String estat = "disponible";
                boolean vendido = false;

                int numero = fila * columnas + columna;

                if (numero % 13 == 0) {
                    estat = "bloquejat";
                } else if (numero % 5 == 0) {
                    vendido = true;
                }

                Venta venta = null;

                if (vendido) {
                    venta = new Venta(
                        "RES-" + id,
                        "Comprador " + id,
                        "comprador@platea.cat",
                        18.00
                    );
                }

                // Crear el asiento
                Asiento asiento = new Asiento(
                    id,
                    fila + 1,
                    columna + 1,
                    zonaNumerada.id(),
                    estat,
                    vendido,
                    venta
                );

                asientos.add(asiento);

                // Dibujar según el estado del asiento
                gc.setFill(obtenerColorAsiento(asiento));
                gc.fillOval(x, y, tamaño, tamaño);

                gc.setStroke(Color.DARKGREEN);
                gc.strokeOval(x, y, tamaño, tamaño);
            }
        }

        dibujarZonaGeneral(
            gc,
            columnas,
            tamaño,
            separacion,
            inicioX,
            inicioY
        );
    }

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

        gc.fillText(zonaGeneral.nom(), x + ancho / 2 - 45, y + 40);
        gc.fillText("Aforo: " + zonaGeneral.aforament(),x + ancho / 2 - 35, y + 65);
    }

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

            // Calcular el centro del asiento
            double xAsiento = INICIO_X +
        (asiento.columna() - 1) * (TAMANO_ASIENTO + SEPARACION_ASIENTO);

        double yAsiento = INICIO_Y + (asiento.fila() - 1) * (TAMANO_ASIENTO + SEPARACION_ASIENTO);

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
                lblZona.setText(asiento.idZona());

                // Limpiar los datos de la venta anterior
                limpiarDetalleVenta();

                // Comprobar si el asiento está bloqueado
                if ("bloquejat".equals(asiento.estat())) {

                    lblEstado.setText("Bloqueado");

                // Comprobar si el asiento está vendido
                } else if (asiento.vendido()) {

                    lblEstado.setText("Vendido");

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

    private void actualizarOcupacionZonas() {
        // Contar los asientos vendidos de la zona numerada
        int ocupadosNumerada = 0;

        for (Asiento asiento : asientos) {
            if (zonaNumerada.id().equals(asiento.idZona()) && asiento.vendido()) {
                ocupadosNumerada++;
            }
        }

        // Calcular el aforo de cada zona
        int aforoNumerada = zonaNumerada.aforament();
        int aforoGeneral = zonaGeneral.aforament();

        // Ocupación ficticia de la zona general
        int ocupadosGeneral = Math.min(8, aforoGeneral);

        // Actualizar los Labels
        lblOcupacionNumerada.setText(
            zonaNumerada.nom() + ": " + ocupadosNumerada + "/" + aforoNumerada
        );

        lblOcupacionGeneral.setText(
            zonaGeneral.nom() + ": " + ocupadosGeneral + "/" + aforoGeneral
        );
    }

    // Limpiar los datos de la venta
    private void limpiarDetalleVenta() {

        lblLocalizador.setText("-");
        lblComprador.setText("-");
        lblCorreo.setText("-");
        lblImporte.setText("-");
    }

    // Mostrar los datos ficticios de una venta
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

    private int obtenerAforoGeneral() {
        return zonaGeneral.aforament();
    }

    private Color obtenerColorAsiento(Asiento asiento) {
        if (asiento.estat().equals("bloquejat")) {
            return Color.GRAY;
        }

        if (asiento.vendido()) {
            return Color.INDIANRED;
        }

        return Color.LIGHTGREEN;
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

    private record Venta(
        String localizador,
        String comprador,
        String correo,
        double importe
    ) {
    }

    // Estructura para almacenar la información de cada zona
    private record ZonaMapa(
            String id,
            String nombre,
            String tipo,
            int aforo,
            double x,
            double y,
            double ancho,
            double alto
    ) {
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

        System.out.println("Recinto recibido: " + recinto.nom());

        for (CtrlLlistatVendes.Zona zona : recinto.zones()) {

            System.out.println(
                "Zona: " + zona.id() +
                " - " + zona.nom() +
                " - Tipo: " + zona.tipus()
            );

            if ("numerada".equalsIgnoreCase(zona.tipus())) {
                zonaNumerada = zona;

            } else if ("general".equalsIgnoreCase(zona.tipus())) {
                zonaGeneral = zona;
            }
        }

        // Comprobar que se han encontrado las dos zonas
        if (zonaNumerada == null || zonaGeneral == null) {
            throw new IllegalStateException(
                "No se han encontrado las zonas numerada y general del recinto."
            );
        }

        // Dibujar el mapa con los datos del recinto
        dibujarAsientosPrueba();
        actualizarOcupacionZonas();
    }

    private int obtenerColumnas() {
        return zonaNumerada.aforament() / FILAS;
    }
}
