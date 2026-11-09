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

    // Lista de asientos del recinto
    private final List<Asiento> asientos = new ArrayList<>();

    // Lista de zonas del recinto
    private final List<ZonaMapa> zonasMapa = new ArrayList<>();

    // Tamaño de los asientos
    private static final double TAMANO_ASIENTO = 18;

    // Tamaño de la sala que estamos mostrando
    private enum TamanoSala {
        PEQUENA, MEDIANA, GRANDE
    }

    // De momento probaremos con una sala pequeña
    private TamanoSala tamanoSala = TamanoSala.MEDIANA;

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
        dibujarAsientosPrueba();

        actualizarOcupacionZonas();

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

        double tamaño = 18;
        double separacion = 12;

        double inicioX = 50;
        double inicioY = 50;

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

                // Crear el asiento
                Asiento asiento = new Asiento(
                    id,
                    fila + 1,
                    columna + 1,
                    "Z1",
                    estat,
                    vendido,
                    x,
                    y
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
        gc.fillText("ZONA GENERAL", x + ancho / 2 - 45, y + 40);
        gc.fillText("Aforo: " + obtenerAforoGeneral(), x + ancho / 2 - 35, y + 65);
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
            double centroX = asiento.x() + TAMANO_ASIENTO / 2;
            double centroY = asiento.y() + TAMANO_ASIENTO / 2;

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

                    mostrarVentaPrueba(asiento);

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
            if ("Z1".equals(asiento.idZona()) && asiento.vendido()) {
                ocupadosNumerada++;
            }
        }

        // Calcular el aforo de cada zona
        int aforoNumerada = FILAS * obtenerColumnas();
        int aforoGeneral = obtenerAforoGeneral();

        // Ocupación ficticia de la zona general
        int ocupadosGeneral = switch (tamanoSala) {
            case PEQUENA -> 8;
            case MEDIANA -> 15;
            case GRANDE -> 25;
        };

        // Actualizar los Labels
        lblOcupacionNumerada.setText(
            "Numerada: " + ocupadosNumerada + "/" + aforoNumerada
        );

        lblOcupacionGeneral.setText(
            "General: " + ocupadosGeneral + "/" + aforoGeneral
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
    private void mostrarVentaPrueba(Asiento asiento) {

        lblLocalizador.setText("RES-" + asiento.id());
        lblComprador.setText("Comprador de prueba");
        lblCorreo.setText("prueba@platea.cat");
        lblImporte.setText("18,00 €");
    }

    private int obtenerAforoGeneral() {
        return switch (tamanoSala) {
            case PEQUENA -> 20;
            case MEDIANA -> 30;
            case GRANDE -> 40;
        };
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
        double x,
        double y
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

    // Recibir el evento seleccionado
    public void setEsdeveniment(CtrlLlistatVendes.Esdeveniment esdeveniment,String nombreRecinto) {
        this.esdeveniment = esdeveniment;

        // Mostrar los datos del evento
        lblNombreEvento.setText(esdeveniment.nom());
        lblNombreRecinto.setText(nombreRecinto);
    }

    private int obtenerColumnas() {
        return switch (tamanoSala) {
            case PEQUENA -> 10;
            case MEDIANA -> 15;
            case GRANDE -> 20;
        };
    }
}
