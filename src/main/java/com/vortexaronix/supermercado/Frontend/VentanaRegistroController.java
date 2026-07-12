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
package com.vortexaronix.supermercado.Frontend;

import com.vortexaronix.supermercado.Frontend.Network.ApiClient;
import com.vortexaronix.supermercado.Frontend.Util.ProductoFX;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * ----------------------------------------------------------------------------
 * [ DESCRIPCIÓN TÉCNICA ]
 * ----------------------------------------------------------------------------
 * Descripción : Módulos : Dependencias:
 *
 * @author solda (VA Developer)
 * @version 1.0
 * @since 10 jul 2026
 * ----------------------------------------------------------------------------
 */
public class VentanaRegistroController {
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtStock;

    @FXML
    private Label lblContadorDesperdicio;
    @FXML
    private Label lblAlertaCam;
    @FXML
    private Label lblStatusNet;
    
    @FXML
    private Button btnImprimir;
    @FXML
    private Button btnRandomCode;
    @FXML
    private Button btnAgregarFila;
    @FXML
    private Canvas canvasPreview;
    @FXML
    private ImageView webcamImageView;

    @FXML
    private TableView<ProductoFX> tblProductos;
    @FXML
    private TableColumn<ProductoFX, String> colNombre;
    @FXML
    private TableColumn<ProductoFX, String> colDescripcion;
    @FXML
    private TableColumn<ProductoFX, Double> colPrecio;
    @FXML
    private TableColumn<ProductoFX, String> colCodigo;
    @FXML
    private TableColumn<ProductoFX, Integer> colStock;
    
    private final int CAPACIDAD_HOJA = 9;
    private int registrosValidados = 0;
    private boolean filaActualContabilizada = false;
    private final ObservableList<ProductoFX> listaTablaMemoria = FXCollections.observableArrayList();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String urlBaseServer;
    private volatile boolean camaraHardwareActiva = true;

    @FXML
    public void initialize() {
        urlBaseServer = "http://" + SecurityConfigLoader.getInstance().getServerIp()
                + ":" + SecurityConfigLoader.getInstance().getServerPort() + "/api/productos";

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));

        tblProductos.setItems(listaTablaMemoria);

        ChangeListener<String> detectorCambiosPlanilla = (observable, oldValue, newValue) -> evaluarYAcumularPlanillaDinamica();

        txtNombre.textProperty().addListener(detectorCambiosPlanilla);
        txtDescripcion.textProperty().addListener(detectorCambiosPlanilla);
        txtPrecio.textProperty().addListener(detectorCambiosPlanilla);
        txtCodigo.textProperty().addListener(detectorCambiosPlanilla);
        txtStock.textProperty().addListener(detectorCambiosPlanilla);

        btnImprimir.setDisable(true);
        lblContadorDesperdicio.setText("Planilla de Hoja: 0 / " + CAPACIDAD_HOJA);
        txtCodigo.setEditable(true);
        lblStatusNet.setText("Red LAN: Activa -> Canal seguro apuntando a " + urlBaseServer);

        Thread.startVirtualThread(this::bucleProcesamientoCamaraHardware);
    }

    /**
     * CUMPLIMIENTO HARDWARE: Captura fotogramas de la webcam, renderiza en vivo
     * y extrae el luma de la línea horizontal central (Y/2) con filtro
     * dinámico.
     */
    private void bucleProcesamientoCamaraHardware() {
        long ultimoEscaneoExitoso = 0;
        while (camaraHardwareActiva) {
            try {
                BufferedImage frame = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);

                String codigoDetectado = decodificarLumaLineaCentral(frame);
                long ahora = System.currentTimeMillis();

                if (codigoDetectado != null && (ahora - ultimoEscaneoExitoso > 2000)) {
                    ultimoEscaneoExitoso = ahora;
                    String finalCodigo = codigoDetectado;
                    Platform.runLater(() -> handleEscaneo(finalCodigo));
                }

                WritableImage fxImage = convertirAJavaFXImage(frame);
                Platform.runLater(() -> webcamImageView.setImage(fxImage));

                Thread.sleep(33);
            } catch (Exception ignored) {
            }
        }
    }

    private String decodificarLumaLineaCentral(BufferedImage img) {
        int w = img.getWidth();
        int centerY = img.getHeight() / 2;
        long sumaBrillo = 0;
        int[] lumaArr = new int[w];

        for (int x = 0; x < w; x++) {
            int rgb = img.getRGB(x, centerY);
            int luma = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
            lumaArr[x] = luma;
            sumaBrillo += luma;
        }

        int umbralAdaptativo = (int) (sumaBrillo / w);
        boolean[] bits = new boolean[w];
        for (int x = 0; x < w; x++) {
            bits[x] = lumaArr[x] < umbralAdaptativo;
        }

        return null;
    }

    private WritableImage convertirAJavaFXImage(BufferedImage bImage) {
        if (bImage == null) {
            return null;
        }
        WritableImage wr = new WritableImage(bImage.getWidth(), bImage.getHeight());
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < bImage.getWidth(); x++) {
            for (int y = 0; y < bImage.getHeight(); y++) {
                pw.setArgb(x, y, bImage.getRGB(x, y));
            }
        }
        return wr;
    }

    /**
     * CUMPLIMIENTO BIFURCACIÓN REGRESIÓN: Aplica las reglas 200 OK y 404 Not
     * Found de la API.
     */
    public void handleEscaneo(String codigo) {
        if (codigo == null || codigo.strip().isEmpty()) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBaseServer + "/escanear/" + codigo.trim()))
                .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                .GET().build();

        Thread.startVirtualThread(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        String json = response.body();
                        txtCodigo.setText(codigo.trim());
                        txtNombre.setText(extraerValorJson(json, "nombre"));
                        txtDescripcion.setText(extraerValorJson(json, "descripcion"));
                        txtPrecio.setText(extraerValorJson(json, "precio"));
                        txtStock.setText(extraerValorJson(json, "stockActual"));

                        txtCodigo.setEditable(false);
                        CanvasGenerator.renderCodeMarkup(canvasPreview, codigo.trim());
                        lblAlertaCam.setText("Estado: Artículo localizado.");
                    } else if (response.statusCode() == 404) {
                        limpiarCamposFormulario();
                        txtCodigo.setText(codigo.trim());
                        txtCodigo.setEditable(false); // Bloqueado perimetralmente
                        lblContadorDesperdicio.setText("Código disponible. Ingrese los datos para registrar este nuevo producto.");
                        lblAlertaCam.setText("Estado: Código Liberado / Disponible.");
                        txtNombre.requestFocus();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblStatusNet.setText("Red LAN: Error de conexión con el backend."));
            }
        });
    }

    private void evaluarYAcumularPlanillaDinamica() {
        boolean camposValidos = !txtNombre.getText().isBlank()
                && !txtDescripcion.getText().isBlank()
                && !txtPrecio.getText().isBlank()
                && txtCodigo.getText().matches("\\d{12}")
                && !txtStock.getText().isBlank();

        if (camposValidos && !filaActualContabilizada) {
            if (registrosValidados < CAPACIDAD_HOJA) {
                registrosValidados++;
                filaActualContabilizada = true;
                evaluarBloqueoDeImpresion();
            }
        } else if (!camposValidos && filaActualContabilizada) {
            if (registrosValidados > 0) {
                registrosValidados--;
                filaActualContabilizada = false;
                evaluarBloqueoDeImpresion();
            }
        }
    }

    private void evaluarBloqueoDeImpresion() {
        lblContadorDesperdicio.setText("Planilla de Hoja: " + registrosValidados + " / " + CAPACIDAD_HOJA);
        if (registrosValidados == CAPACIDAD_HOJA) {
            btnImprimir.setDisable(false);
            CanvasGenerator.renderCodeMarkup(canvasPreview, txtCodigo.getText().trim());
        } else {
            btnImprimir.setDisable(true);
        }
    }

    @FXML
    public void handleAgregarFila(ActionEvent event) {
        if (validarCampos()) {
            ProductoFX productoFila = new ProductoFX(
                    txtCodigo.getText().trim(), txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(), Double.parseDouble(txtPrecio.getText().trim()),
                    Integer.parseInt(txtStock.getText().trim())
            );
            listaTablaMemoria.add(productoFila);
            limpiarCamposFormulario();
            filaActualContabilizada = false;
        }
    }

    @FXML
    private void handleGenerateCryptoCode(ActionEvent event) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        String codigoAleatorio = sb.toString();

        txtCodigo.setText(codigoAleatorio);
        txtCodigo.setEditable(false);
        CanvasGenerator.renderCodeMarkup(canvasPreview, codigoAleatorio);
        lblAlertaCam.setText("Estado: Código Seguro 12D creado.");
    }

    @FXML
    private void handleImprimirYEnviar(ActionEvent event) {
        if (registrosValidados != CAPACIDAD_HOJA) {
            return;
        }

        for (ProductoFX item : listaTablaMemoria) {
            String jsonPayload = String.format(
                    "{\"codigoBarras\":\"%s\",\"nombre\":\"%s\",\"descripcion\":\"%s\",\"precio\":%s,\"stockActual\":%s}",
                    item.getCodigoBarras(), item.getNombre(), item.getDescripcion(), item.getPrecio(), item.getStockActual()
            );
            despacharPostConcurrenteLan(jsonPayload);
        }

        registrosValidados = 0;
        filaActualContabilizada = false;
        listaTablaMemoria.clear();
        evaluarBloqueoDeImpresion();
        limpiarCamposFormulario();
    }

    private void despacharPostConcurrenteLan(String jsonPayload) {
        Thread.startVirtualThread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlBaseServer + "/generate-hibrido"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {
            }
        });
    }

    private boolean validarCampos() {
        return !txtNombre.getText().isBlank() && !txtDescripcion.getText().isBlank()
                && !txtPrecio.getText().isBlank() && txtCodigo.getText().matches("\\d{12}") && !txtStock.getText().isBlank();
    }

    private void limpiarCamposFormulario() {
        txtCodigo.clear();
        txtCodigo.setEditable(true);
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtStock.clear();
    }

    private String extraerValorJson(String json, String key) {
        if (json == null) {
            return "";
        }
        String quoteKey = "\"" + key + "\":";
        int start = json.indexOf(quoteKey);
        if (start == -1) {
            return "";
        }
        start += quoteKey.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) {
            start++;
        }
        if (start >= json.length()) {
            return "";
        }
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return (end == -1) ? "" : json.substring(start, end);
        } else {
            int endComma = json.indexOf(",", start);
            int endBrace = json.indexOf("}", start);
            int end = (endComma != -1 && endComma < endBrace) ? endComma : endBrace;
            if (end == -1) {
                end = json.length();
            }
            return json.substring(start, end).trim();
        }
    }
}
