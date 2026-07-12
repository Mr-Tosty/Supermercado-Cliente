/*
 *  __      __  _______ 
 *  \ \    / / |  ___  |
 *   \ \  / /  | |___| |
 *    \ \/ /   |  ___  |   VORTEX
 *     \__/    |_|   |_|   ARONIX
 * ----------------------------------------------------------------------------
 *  @Copyright: (c) 2026 Vortex Aronix.
 *  @License  : Privativa - Todos los derechos reservados.
 *  @Terms    : La copia, distribución o ingeniería inversa de este código 
 *              fuente está estrictamente prohibida bajo los términos de VA.
 *              Para mas informacion en https://vortexaronix.com/FAQs/App/Escritorio/Modificaciones.
 * ----------------------------------------------------------------------------
 */
package com.vortexaronix.supermercado.Frontend.Util;

import com.vortexaronix.supermercado.Frontend.SecurityConfigLoader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.PageLayout;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * ----------------------------------------------------------------------------
 * [ DESCRIPCIÓN TÉCNICA ]
 * ----------------------------------------------------------------------------
 * Descripción : Módulos : Dependencias:
 *
 * @author solda (VA Developer)
 * @version 1.0
 * @since 11 jul 2026
 * ----------------------------------------------------------------------------
 */
public class ServicioImpresoraPlanilla {

    private final int CAPACIDAD_HOJA = 9;
    private int registrosValidados = 0;
    private boolean filaActualContabilizada = false;
    private GridPane panelHojaA4Produccion;

    private final ObservableList<ProductoFX> colaDeImpresion = FXCollections.observableArrayList();

    private TextField txtNombre, txtDescripcion, txtPrecio, txtCodigo, txtStock;
    private Label lblContadorDesperdicio;
    private Button btnImprimir;
    private Canvas canvasPreview;
    private String urlBaseServer;
    private javafx.scene.layout.VBox contenedorHojaImpresion;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ServicioImpresoraPlanilla(TextField txtNombre, TextField txtDescripcion, TextField txtPrecio,
            TextField txtCodigo, TextField txtStock, Label lblContadorDesperdicio,
            Button btnImprimir, Canvas canvasPreview) {
        this.txtNombre = txtNombre;
        this.txtDescripcion = txtDescripcion;
        this.txtPrecio = txtPrecio;
        this.txtCodigo = txtCodigo;
        this.txtStock = txtStock;
        this.lblContadorDesperdicio = lblContadorDesperdicio;
        this.btnImprimir = btnImprimir;
        this.canvasPreview = canvasPreview;

        this.urlBaseServer = "http://" + SecurityConfigLoader.getInstance().getServerIp()
                + ":" + SecurityConfigLoader.getInstance().getServerPort() + "/api/productos";

        inicializarListenersReactivos();
    }

    /**
     * CUMPLIMIENTO REQUISITO: Configura la escucha en tiempo real de los 5
     * campos.
     */
    private void inicializarListenersReactivos() {
        ChangeListener<String> listenerCambioFila = (obs, oldVal, newVal) -> evaluarYAcumularAutomatico();

        txtNombre.textProperty().addListener(listenerCambioFila);
        txtDescripcion.textProperty().addListener(listenerCambioFila);
        txtPrecio.textProperty().addListener(listenerCambioFila);
        txtCodigo.textProperty().addListener(listenerCambioFila);
        txtStock.textProperty().addListener(listenerCambioFila);

        btnImprimir.setDisable(true);
        lblContadorDesperdicio.setText("Planilla de Hoja: " + registrosValidados + " / " + CAPACIDAD_HOJA);
    }

    /**
     * Evalúa dinámicamente si todos los campos de una fila están completos y
     * validados.
     */
    private void evaluarYAcumularAutomatico() {
        boolean filaCompletaYValida = !txtNombre.getText().isBlank()
                && !txtDescripcion.getText().isBlank()
                && !txtPrecio.getText().isBlank()
                && txtCodigo.getText().matches("\\d{12}")
                && !txtStock.getText().isBlank();

        if (filaCompletaYValida && !filaActualContabilizada) {
            if (registrosValidados < CAPACIDAD_HOJA) {
                registrosValidados++;
                filaActualContabilizada = true;
                actualizarEstadoImpresionYUI();
            }
        } else if (!filaCompletaYValida && filaActualContabilizada) {
            if (registrosValidados > 0) {
                registrosValidados--;
                filaActualContabilizada = false;
                actualizarEstadoImpresionYUI();
            }
        }
    }

    /**
     * CUMPLIMIENTO REQUISITO: Mantiene deshabilitado el botón si X != Y
     * (Planilla incompleta).
     */
    private void actualizarEstadoImpresionYUI() {
    lblContadorDesperdicio.setText("Planilla de Hoja: " + registrosValidados + " / " + CAPACIDAD_HOJA);
    
    // REGLA DE NEGOCIO: Habilitación e impresión automática exclusiva si X == Y (9/9)
    if (registrosValidados == CAPACIDAD_HOJA) {
        btnImprimir.setDisable(false); 
        
        // AUTOMÁTICO: Calcular las dimensiones de la hoja A4 y dibujar la vista en miniatura 3x3 en el Canvas
        calcularYGenerarMatrizA4Automatica();
    } else {
        btnImprimir.setDisable(true);
        GraphicsContext gc = canvasPreview.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasPreview.getWidth(), canvasPreview.getHeight());
    }
}

public void ejecutarImpresionYEnvioConcurrente() {
    if (registrosValidados != CAPACIDAD_HOJA) return;

    System.out.println("[PROCESAMIENTO LOTE] Despachando cola de impresión A4 y peticiones REST concurrentes...");

    // 1. IMPRIMIR HOJA A4 DE FORMA AUTOMÁTICA EN HARDWARE
    enviarImpresionFisicaPapelA4();

    // 2. DISPARAR ENVÍO HTTP POST EN PARALELO POR LA LAN (Sincronización MariaDB XAMPP)
    for (ProductoFX producto : colaDeImpresion) {
        String jsonPayload = String.format(
            "{\"codigoBarras\":\"%s\",\"nombre\":\"%s\",\"descripcion\":\"%s\",\"precio\":%s,\"stockActual\":%s}",
            producto.getCodigoBarras(), producto.getNombre(), producto.getDescripcion(), producto.getPrecio(), producto.getStockActual()
        );
        
        Thread.startVirtualThread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlBaseServer + "/generate-hibrido")) // Activación de regla de reciclaje caótico
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("[LAN-ERROR] Fallo de transmisión en la subred local: " + e.getMessage());
            }
        });
    }

    // Purga y limpieza atómica de parámetros tras concluir el lote comercial
    registrosValidados = 0;
    filaActualContabilizada = false;
    colaDeImpresion.clear();
    actualizarEstadoImpresionYUI();
    limpiarCampos();
}


    /**
     * Registra el artículo actual en la cola interna y limpia el formulario
     * para permitir el monitoreo de una nueva fila de datos. Integrado con el
     * botón 'btnAgregarFila'.
     */
    public void registrarAccionAgregarFila() {
        if (registrosValidados <= CAPACIDAD_HOJA && filaActualContabilizada) {
            try {
                ProductoFX itemValidado = new ProductoFX(
                        txtCodigo.getText().trim(),
                        txtNombre.getText().trim(),
                        txtDescripcion.getText().trim(),
                        Double.parseDouble(txtPrecio.getText().trim()),
                        Integer.parseInt(txtStock.getText().trim())
                );
                colaDeImpresion.add(itemValidado);

                limpiarCampos();
                filaActualContabilizada = false;
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void limpiarCampos() {
        txtCodigo.clear();
        txtCodigo.setEditable(true);
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtStock.clear();
    }

    private void ejecutarImpresionFisicaJavaFX() {
        // Buscar la impresora predeterminada configurada en el Sistema Operativo de la caja
        javafx.print.Printer printer = javafx.print.Printer.getDefaultPrinter();
        if (printer == null) {
            System.err.println("[HARDWARE-ERROR] No se detectó ninguna impresora activa en la terminal.");
            return;
        }

        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob(printer);
        if (job != null) {
            // Configurar los parámetros de página a tamaño Carta/A4 con orientación Vertical
            javafx.print.PageLayout pageLayout = printer.createPageLayout(
                    javafx.print.Paper.NA_LETTER,
                    javafx.print.PageOrientation.PORTRAIT,
                    javafx.print.Printer.MarginType.DEFAULT
            );
boolean exitoSpool = job.printPage(pageLayout, contenedorHojaImpresion);

            if (exitoSpool) {
                job.endJob();
                System.out.println("[HARDWARE] Documento de etiquetas enviado a la cola de la impresora física.");
            } else {
                System.err.println("[HARDWARE-ERROR] El controlador de la impresora rechazó el spool de páginas.");
            }
        }
    }

    private void generarVistaPreviaYHoja() {
        contenedorHojaImpresion = new javafx.scene.layout.VBox(10);
        contenedorHojaImpresion.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 20;");
GraphicsContext gc = canvasPreview.getGraphicsContext2D();
        double cvW = canvasPreview.getWidth();
        double cvH = canvasPreview.getHeight();
        gc.clearRect(0, 0, cvW, cvH);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, cvW, cvH);

        // Calcular factor de escalado para meter la miniatura de los 9 códigos en tu Canvas de previsualización
        double escalaX = cvW / 3.0;
        double escalaY = cvH / 3.0;

        int fila = 0;
        int columna = 0;

        // Instancia limpia del motor de dibujo de paridades ópticas GS1
        GeneradorCodigo12Digitos motorGrafico = new GeneradorCodigo12Digitos();

        System.out.println("[GRAFICADOR] Estructurando matriz óptica para el lienzo de previsualización...");

        for (ProductoFX producto : colaDeImpresion) {
            // A. Crear un sub-canvas individual en alta resolución para la hoja física real de papel
            Canvas canvasEtiquetaPapel = new Canvas(300, 100);
            motorGrafico.dibujarCodigo(canvasEtiquetaPapel, producto.getCodigoBarras());

            // Agregar la etiqueta de barras con dimensiones industriales al contenedor real
            contenedorHojaImpresion.getChildren().add(canvasEtiquetaPapel);

            // B. Renderizar la miniatura correspondiente dentro de la cuadrícula de tu Canvas visual del FXML
            gc.save();
            // Mover el puntero del contexto gráfico a la posición matricial 3x3 del Canvas del panel
            gc.translate(columna * escalaX, fila * escalaY);

            // Crear un renderizado temporal escalado para la vista en pantalla del cajero
            Canvas canvasMiniaturaAux = new Canvas(escalaX - 4, escalaY - 4);
            motorGrafico.dibujarCodigo(canvasMiniaturaAux, producto.getCodigoBarras());

            // Dibujar instantáneamente el resultado sobre la sección de la interfaz comercial
            gc.drawImage(canvasMiniaturaAux.snapshot(null, null), 2, 2);
            gc.restore();

            columna++;
            if (columna >= 3) {
                columna = 0;
                fila++;
            }
        }
    }

    private void calcularYGenerarMatrizA4Automatica() {
    // 1. DIMENSIONAMIENTO INDUSTRIAL: Definición matemática de una hoja A4 en puntos gráficos (210mm x 297mm)
    double anchoHojaA4 = 595.0; 
    double altoHojaA4 = 842.0;

    // Inicializar el contenedor que se enviará directamente al hardware de la impresora
    panelHojaA4Produccion = new javafx.scene.layout.GridPane();
    panelHojaA4Produccion.setPrefSize(anchoHojaA4, altoHojaA4);
    panelHojaA4Produccion.setMinSize(anchoHojaA4, altoHojaA4);
    panelHojaA4Produccion.setMaxSize(anchoHojaA4, altoHojaA4);
    panelHojaA4Produccion.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 35; -fx-alignment: center;");
    panelHojaA4Produccion.setHgap(20); // Espacio de corte horizontal entre etiquetas
    panelHojaA4Produccion.setVgap(25); // Espacio de corte vertical entre etiquetas
    GraphicsContext gc = canvasPreview.getGraphicsContext2D();
    double canvasW = canvasPreview.getWidth();
    double canvasHeight = canvasPreview.getHeight();
    gc.clearRect(0, 0, canvasW, canvasHeight);
    gc.setFill(javafx.scene.paint.Color.WHITE);
    gc.fillRect(0, 0, canvasW, canvasHeight);

    // Calcular factor de escala para proyectar la hoja A4 completa dentro de las dimensiones de tu Canvas FXML
    double factorEscalaX = canvasW / anchoHojaA4;
    double factorEscalaY = canvasHeight / altoHojaA4;

    // Calcular el tamaño exacto que debe tener cada una de las 9 etiquetas para encajar perfectamente en la cuadrícula 3x3
    double anchoEtiquetaA4 = (anchoHojaA4 - 70 - 40) / 3.0; // Restando paddings laterales (70) y gaps (40)
    double altoEtiquetaA4 = (altoHojaA4 - 70 - 50) / 3.0;  // Restando paddings verticales (70) y gaps (50)

    int columna = 0;
    int fila = 0;

    GeneradorCodigo12Digitos motorCriptoGrafico = new GeneradorCodigo12Digitos();
    System.out.println("[AUTOMATIZACIÓN A4] Renderizando matriz 3x3 con estructura vertical de 3 niveles...");

    for (ProductoFX producto : colaDeImpresion) {
        Canvas canvasEtiquetaReal = new Canvas(anchoEtiquetaA4, altoEtiquetaA4);
        GraphicsContext gcEtiqueta = canvasEtiquetaReal.getGraphicsContext2D();
        
        gcEtiqueta.setFill(javafx.scene.paint.Color.WHITE);
        gcEtiqueta.fillRect(0, 0, anchoEtiquetaA4, altoEtiquetaA4);
        
        gcEtiqueta.setFill(javafx.scene.paint.Color.BLACK);
        gcEtiqueta.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, altoEtiquetaA4 * 0.12));
        gcEtiqueta.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gcEtiqueta.fillText(producto.getNombre(), anchoEtiquetaA4 / 2.0, altoEtiquetaA4 * 0.15);

        double altoBarrasEan = altoEtiquetaA4 * 0.55;
        Canvas canvasBarrasInterno = new Canvas(anchoEtiquetaA4, altoBarrasEan);
        motorCriptoGrafico.dibujarCodigo(canvasBarrasInterno, producto.getCodigoBarras());
        
        gcEtiqueta.drawImage(canvasBarrasInterno.snapshot(null, null), 0, altoEtiquetaA4 * 0.22);

        gcEtiqueta.setFill(javafx.scene.paint.Color.DARKSLATEGRAY);
        gcEtiqueta.setFont(javafx.scene.text.Font.font("Courier New", javafx.scene.text.FontWeight.BOLD, altoEtiquetaA4 * 0.14));
        gcEtiqueta.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        
        // Formatear los 12 dígitos separándolos visualmente en bloques de 6
        String textoFormateado = String.format("%s  %s", producto.getCodigoBarras().substring(0, 6), producto.getCodigoBarras().substring(6, 12));
        gcEtiqueta.fillText(textoFormateado, anchoEtiquetaA4 / 2.0, altoEtiquetaA4 * 0.92);
        
        panelHojaA4Produccion.add(canvasEtiquetaReal, columna, fila);

        gc.save();
        double xMiniatura = (35 + columna * (anchoEtiquetaA4 + 20)) * factorEscalaX;
        double yMiniatura = (35 + fila * (altoEtiquetaA4 + 25)) * factorEscalaY;
        double wMiniatura = anchoEtiquetaA4 * factorEscalaX;
        double hMiniatura = altoEtiquetaA4 * factorEscalaY;

        gc.translate(xMiniatura, yMiniatura);
        
        gc.drawImage(canvasEtiquetaReal.snapshot(null, null), 0, 0, wMiniatura, hMiniatura);
        gc.restore();

        columna++;
        if (columna >= 3) {
            columna = 0;
            fila++;
        }
    }
}


    private void enviarImpresionFisicaPapelA4() {
    javafx.print.Printer impresoraDefault = javafx.print.Printer.getDefaultPrinter();
    if (impresoraDefault == null) {
        System.err.println("[HARDWARE-CRÍTICO] Error: No se localizó ninguna impresora de etiquetas en la subred local.");
        return;
    }

    javafx.print.PrinterJob tareaImpresion = javafx.print.PrinterJob.createPrinterJob(impresoraDefault);
    if (tareaImpresion != null) {
        PageLayout layoutA4 = impresoraDefault.createPageLayout(
                javafx.print.Paper.A4,
                javafx.print.PageOrientation.PORTRAIT, 
                javafx.print.Printer.MarginType.HARDWARE_MINIMUM
        );

        boolean resultadoSpool = tareaImpresion.printPage(layoutA4, panelHojaA4Produccion);
        
        if (resultadoSpool) {
            tareaImpresion.endJob();
            System.out.println("[HARDWARE-A4] Spool de páginas unificado enviado con éxito a la impresora.");
        } else {
            System.err.println("[HARDWARE-ERROR] El driver local rechazó el renderizado de la cuadrícula A4.");
        }
    }
}

}
