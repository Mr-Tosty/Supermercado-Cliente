/*
 *  __      __  _______ 
 *  \ \    / / |  ___  |
 *   \ \  / /  | |___| |
 *    \ \/ /   |  ___  |   VORTEX
 *     \__/    |_|   |_|   ARONIX
 * ----------------------------------------------------------------------------
 *  @Copyright: (c) 2026 Vortex Aronix.
 *  @License  : Open Source (Licencia Permisiva / Código Abierto)
 *  @Terms    : Se permite el uso, modificación y distribución de este código.
 *              Debe incluirse este aviso de derechos de autor original y 
 *              acreditar a Vortex Aronix como desarrollador original.
 *              EL SOFTWARE SE PROPORCIONA "TAL CUAL", SIN GARANTÍAS DE NINGÚN 
 *              TIPO. La compañía no se hace responsable de daños, fallos o 
 *              reclamaciones derivadas de su uso.
 * ----------------------------------------------------------------------------
 */
package com.vortexaronix.supermercado.Frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vortexaronix.supermercado.Frontend.Hardware.LectorCamaraTask;
import com.vortexaronix.supermercado.Frontend.Network.ApiClient;
import com.vortexaronix.supermercado.Frontend.Util.ProductoFX;
import com.vortexaronix.supermercado.Frontend.Util.ServicioImpresoraPlanilla;
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
    @SuppressWarnings("unused")
    private Button btnRandomCode;
    @FXML
    @SuppressWarnings("unused")
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
    private final ApiClient apiClient = new ApiClient();
    private static LectorCamaraTask tareaLectorCamara;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String urlBaseServer;
    private ServicioImpresoraPlanilla servicioImpresora;

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

        btnRandomCode.setAccessibleText("Genera un código de barras criptográfico de 12 dígitos de forma segura.");
        btnAgregarFila.setAccessibleText("Indexa una nueva fila vacía en la planilla de control de hardware.");

        btnRandomCode.setStyle("-fx-background-color: #8E44AD; -fx-text-fill: white; -fx-cursor: hand;");
        btnAgregarFila.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand;");

        btnImprimir.setDisable(true);
        lblContadorDesperdicio.setText("Planilla de Hoja: 0 / " + CAPACIDAD_HOJA);
        txtCodigo.setEditable(true);
        lblStatusNet.setText("Red LAN: Activa -> Canal seguro apuntando a " + urlBaseServer);

        this.tareaLectorCamara = new LectorCamaraTask(webcamImageView, this::handleEscaneo);
        Thread.startVirtualThread(tareaLectorCamara);
        this.servicioImpresora = new ServicioImpresoraPlanilla(
                txtNombre, txtDescripcion, txtPrecio, txtCodigo, txtStock, lblContadorDesperdicio, btnImprimir, canvasPreview
        );
    }

    /**
     * CUMPLIMIENTO BIFURCACIÓN REGRESIÓN: Aplica las reglas 200 OK y 404 Not
     * Found de la API.
     */
    public void handleEscaneo(String codigo) {
        if (codigo == null || codigo.strip().isEmpty()) {
            return;
        }

        // Mantener exactamente tu estructura original e invariable de petición HTTP nativa
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

                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode raizNodo = mapper.readTree(json);

                            String jsonCodigo = raizNodo.has("codigo_barras") ? raizNodo.get("codigo_barras").asText().trim() : codigo.trim();
                            String jsonNombre = raizNodo.has("nombre") ? raizNodo.get("nombre").asText().trim() : "Sin nombre";
                            String jsonDesc = raizNodo.has("descripcion") ? raizNodo.get("descripcion").asText().trim() : "Sin descripción";
                            String jsonPrecio = raizNodo.has("precio") ? raizNodo.get("precio").asText().trim() : "0.00";
                            String jsonStock = raizNodo.has("stock_actual") ? raizNodo.get("stock_actual").asText().trim() : "0";

                            txtCodigo.setText(jsonCodigo.isEmpty() ? codigo.trim() : jsonCodigo);
                            txtNombre.setText(jsonNombre.isEmpty() ? "Sin nombre" : jsonNombre);
                            txtDescripcion.setText(jsonDesc.isEmpty() ? "Sin descripción" : jsonDesc);
                            txtPrecio.setText(jsonPrecio.isEmpty() ? "0.00" : jsonPrecio);
                            txtStock.setText(jsonStock.isEmpty() ? "0" : jsonStock);

                            txtCodigo.setEditable(false);
                            CanvasGenerator.renderCodeMarkup(canvasPreview, codigo.trim());
                            lblAlertaCam.setText("Estado: JSON parseado con éxito por librería.");

                            ProductoFX productoMapeado = new ProductoFX(
                                    jsonCodigo.isEmpty() ? codigo.trim() : jsonCodigo,
                                    jsonNombre.isEmpty() ? "Sin nombre" : jsonNombre,
                                    jsonDesc.isEmpty() ? "Sin descripción" : jsonDesc,
                                    Double.parseDouble(jsonPrecio.isEmpty() ? "0.00" : jsonPrecio),
                                    Integer.parseInt(jsonStock.isEmpty() ? "0" : jsonStock)
                            );
                            listaTablaMemoria.add(productoMapeado);

                        } catch (Exception e) {
                            System.err.println("[JSON-ERROR] El procesador Jackson rechazó el payload: " + e.getMessage());
                            lblStatusNet.setText("Red LAN: Error grave en la estructura del JSON.");
                        }

                    } else if (response.statusCode() == 404) {
                        limpiarCamposFormulario();
                        txtCodigo.setText(codigo.trim());
                        txtCodigo.setEditable(false);
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

    /**
     * Envía la estructura JSON hacia el endpoint híbrido de Spring Boot.
     * Ubicado exactamente debajo de handleEscaneo compartiendo la misma
     * infraestructura de red.
     */
    /**
     * Envía la estructura JSON hacia el endpoint híbrido de Spring Boot.
     */
    public void enviarRegistroHibrido(String jsonPayload) {
        if (jsonPayload == null || jsonPayload.strip().isEmpty()) {
            return;
        }

        Thread.startVirtualThread(() -> {
            try {
                HttpResponse<String> response = apiClient.post(urlBaseServer + "/registro-hibrido", jsonPayload);

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    System.out.println("[LAN-POST] Sincronización comercial de lote completada con éxito.");
                } else {
                    System.err.println("[LAN-ERROR] El servidor rechazó la transacción de stock: Status " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[LAN-ERROR] Error de Entrada/Salida en la transmisión: " + e.getMessage());
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

    /**
     * Procesa el código escaneado consultando al backend por medio del
     * ApiClient.
     */
    @FXML
    public void handleAgregarFila(ActionEvent event) {
        if (validarCampos()) {
            ProductoFX productoFila = new ProductoFX(
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    Double.parseDouble(txtPrecio.getText().trim()),
                    Integer.parseInt(txtStock.getText().trim())
            );
            listaTablaMemoria.add(productoFila);
            limpiarCamposFormulario();
            filaActualContabilizada = false;
            lblAlertaCam.setText("Estado: Fila indexada. Nueva celda abierta para monitoreo.");
        }
        servicioImpresora.registrarAccionAgregarFila();
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleImprimirYEnviar(ActionEvent event) {
        if (registrosValidados != CAPACIDAD_HOJA) {
            return;
        }
        System.out.println("[HARDWARE] Generando lote de etiquetas comerciales trilineales en hoja A4...");

        if (servicioImpresora != null) {
            servicioImpresora.ejecutarImpresionYEnvioConcurrente();
        }

        for (ProductoFX producto : listaTablaMemoria) {
            String jsonPayload = String.format(
                    "{\"codigoBarras\":\"%s\",\"nombre\":\"%s\",\"descripcion\":\"%s\",\"precio\":%s,\"stockActual\":%s}",
                    producto.getCodigoBarras(), producto.getNombre(), producto.getDescripcion(), producto.getPrecio(), producto.getStockActual()
            );

            enviarRegistroHibrido(jsonPayload);
        }

        servicioImpresora.ejecutarImpresionYEnvioConcurrente();
        lblAlertaCam.setText("Estado: Planilla impresa y sincronizada.");

        btnImprimir.setDisable(true);
        lblContadorDesperdicio.setText("Planilla de Hoja: 0 / " + CAPACIDAD_HOJA);

        registrosValidados = 0;
        filaActualContabilizada = false;
        listaTablaMemoria.clear();
        txtCodigo.setEditable(true);
        evaluarBloqueoDeImpresion();
        limpiarCamposFormulario();
        lblAlertaCam.setText("Estado: Lote enviado con éxito. Planilla vaciada.");
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

    @FXML
    @SuppressWarnings("unused")
    private void handleGenerateCryptoCode(ActionEvent event) {
        SecureRandom randomSeguro = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            sb.append(randomSeguro.nextInt(10));
        }
        String codigoGenerado = sb.toString();

        txtCodigo.setText(codigoGenerado);
        txtCodigo.setEditable(false);

        CanvasGenerator.renderCodeMarkup(canvasPreview, codigoGenerado);

        lblAlertaCam.setText("Estado: Código Seguro Aleatorio 12D creado y graficado.");
    }

    /**
     * Procesa el nodo JSON validado e inyecta la información en las cajas y en
     * la TableView. Evita fallos de strings vacíos mediante el árbol de datos
     * de Jackson.
     */
    private void procesarProductoExistente(JsonNode raizNodo, String codigo) {
        if (raizNodo == null) {
            return;
        }

        String jsonCodigo = raizNodo.has("codigo_barras") ? raizNodo.get("codigo_barras").asText().trim() : codigo.trim();
        String jsonNombre = raizNodo.has("nombre") ? raizNodo.get("nombre").asText().trim() : "Sin nombre";
        String jsonDesc = raizNodo.has("descripcion") ? raizNodo.get("descripcion").asText().trim() : "Sin descripción";
        String jsonPrecio = raizNodo.has("precio") ? raizNodo.get("precio").asText().trim() : "0.00";
        String jsonStock = raizNodo.has("stock_actual") ? raizNodo.get("stock_actual").asText().trim() : "0";

        txtCodigo.setText(jsonCodigo.isEmpty() ? codigo.trim() : jsonCodigo);
        txtNombre.setText(jsonNombre.isEmpty() ? "Sin nombre" : jsonNombre);
        txtDescripcion.setText(jsonDesc.isEmpty() ? "Sin descripción" : jsonDesc);
        txtPrecio.setText(jsonPrecio.isEmpty() ? "0.00" : jsonPrecio);
        txtStock.setText(jsonStock.isEmpty() ? "0" : jsonStock);

        txtCodigo.setEditable(false);
        txtNombre.setEditable(true);
        txtDescripcion.setEditable(true);
        txtPrecio.setEditable(true);
        txtStock.setEditable(true);

        lblContadorDesperdicio.setText("Producto cargado. Presione agregar para planilla.");
        CanvasGenerator.renderCodeMarkup(canvasPreview, codigo.trim());

        try {
            ProductoFX productoMapeado = new ProductoFX(
                    jsonCodigo.isEmpty() ? codigo.trim() : jsonCodigo,
                    jsonNombre.isEmpty() ? "Sin nombre" : jsonNombre,
                    jsonDesc.isEmpty() ? "Sin descripción" : jsonDesc,
                    Double.parseDouble(jsonPrecio.isEmpty() ? "0.00" : jsonPrecio),
                    Integer.parseInt(jsonStock.isEmpty() ? "0" : jsonStock)
            );

            listaTablaMemoria.add(productoMapeado);

        } catch (NumberFormatException e) {
            System.err.println("[PARSER-ERROR] Error al castear tipos numéricos en el objeto gráfico: " + e.getMessage());
        }
    }

    private void activarModoRegistroNuevo(String codigo) {
        limpiarCampos();
        txtCodigo.setText(codigo);
        txtCodigo.setEditable(false); // Forzar fijación perimetral de los 12 dígitos escaneados
        lblContadorDesperdicio.setText("Código nuevo detectado en la LAN. Ingrese los datos de registro.");
        btnImprimir.setDisable(true); // .setDisable(true) mandatorio inicial
        txtNombre.requestFocus();    // Foco automático para optimizar la velocidad del operario
    }

    private void limpiarCampos() {
        txtCodigo.clear();
        txtCodigo.setEditable(true);
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtStock.clear();
    }

    public static LectorCamaraTask getTareaLectorCamara() {
        return tareaLectorCamara;
    }
}
