package grup1.desktop;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CtrlMapa {

    // Evento seleccionado desde el listado
    private CtrlLlistatVendes.Esdeveniment esdeveniment;

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
    private void initialize() {
        dibujarAsientosPrueba();

    }

    // Dibujar asientos de prueba en el Canvas
    private void dibujarAsientosPrueba() {

        GraphicsContext gc = canvasMapa.getGraphicsContext2D();

        // Tamaño de los asientos
        double tamaño = 18;

        // Separación entre asientos
        double separacion = 12;

        // Posición inicial
        double inicioX = 50;
        double inicioY = 50;

        // Dibujar 5 filas de 10 asientos
        for (int fila = 0; fila < 5; fila++) {

            for (int columna = 0; columna < 10; columna++) {

                // Alternar entre asientos libres y vendidos
                if ((fila + columna) % 4 == 0) {
                    gc.setFill(Color.RED);
                } else {
                    gc.setFill(Color.GREEN);
                }

                // Calcular la posición del asiento
                double x = inicioX + columna * (tamaño + separacion);
                double y = inicioY + fila * (tamaño + separacion);

                // Dibujar el asiento
                gc.fillOval(x, y, tamaño, tamaño);
            }
        }
    }

    // Recibir el evento seleccionado
    public void setEsdeveniment(CtrlLlistatVendes.Esdeveniment esdeveniment,String nombreRecinto) {
        this.esdeveniment = esdeveniment;

        // Mostrar los datos del evento
        lblNombreEvento.setText(esdeveniment.nom());
        lblNombreRecinto.setText(nombreRecinto);
    }
}
