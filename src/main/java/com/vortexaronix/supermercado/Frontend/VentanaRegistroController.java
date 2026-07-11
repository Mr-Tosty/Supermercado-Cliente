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

import com.vortexaronix.supermercado.Frontend.Util.GeneradorCodigo12Digitos;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;

/**
 * ----------------------------------------------------------------------------
 * [ DESCRIPCIÓN TÉCNICA ]
 * ----------------------------------------------------------------------------
 * Descripción : 
 * Módulos     : 
 * Dependencias: 
 * 
 * @author  solda (VA Developer)
 * @version 1.0
 * @since   10 jul 2026
 * ----------------------------------------------------------------------------
 */
public class VentanaRegistroController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtStock;
    @FXML private Label lblContadorDesperdicio;
    @FXML private Button btnImprimir;
    @FXML private Canvas canvasPreview;

    private final int CAPACIDAD_HOJA = 9;
    private int registrosValidados = 0;
    private final GeneradorCodigo12Digitos generador = new GeneradorCodigo12Digitos();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String BASE_URL = "http://" + SecurityConfigLoader.getInstance().getServerIp() + 
                                     ":" + SecurityConfigLoader.getInstance().getServerPort() + "/api/productos";
    
    public void handleEscaneo(String codigo) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/escanear/" + codigo))
                .GET()
                .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(() -> procesarProductoExistente(response.body()));
                    } else if (response.statusCode() == 404) {
                        Platform.runLater(() -> activarModoRegistroNuevo(codigo));
                    }
                });
    }
    
    public void enviarRegistroHibrido(String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/registro-hibrido"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(() -> mostrarAlerta("Éxito", "Producto registrado correctamente."));
                    }
                });
    }
    
    public void enviarSalidaStock(String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/stock/salida"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("Salida de stock confirmada.");
                    } else {
                        Platform.runLater(() -> mostrarAlerta("Error", "No se pudo procesar la salida (Stock insuficiente)."));
                    }
                });
    }
    
    private void procesarProductoExistente(String json) {
        // Validación defensiva contra respuesta nula o vacía
        if (json == null || json.trim().isEmpty()) {
            mostrarAlerta("Error de Conexión", "El servidor no devolvió datos válidos.");
            return;
        }

        // Extracción segura de valores
        String nombre = extraerValorJson(json, "nombre");
        String descripcion = extraerValorJson(json, "descripcion");
        String precioStr = extraerValorJson(json, "precio");
        String stockStr = extraerValorJson(json, "stock");

        // Actualización de campos con manejo de valores por defecto
        txtNombre.setText(nombre.isEmpty() ? "Sin nombre" : nombre);
        txtDescripcion.setText(descripcion.isEmpty() ? "Sin descripción" : descripcion);
        txtPrecio.setText(precioStr.isEmpty() ? "0.00" : precioStr);
        txtStock.setText(stockStr.isEmpty() ? "0" : stockStr);

        // Habilitación de edición
        txtCodigo.setEditable(false);
        txtNombre.setEditable(true);
        txtDescripcion.setEditable(true);
        txtPrecio.setEditable(true);
        txtStock.setEditable(true);

        lblContadorDesperdicio.setText("Producto cargado correctamente.");
    }

    private String extraerValorJson(String json, String key) {
        if (json == null) return "";
        
        String quoteKey = "\"" + key + "\":";
        int start = json.indexOf(quoteKey);
        
        if (start == -1) return "";
        
        start += quoteKey.length();
        
        // Saltar espacios en blanco después de los dos puntos
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) {
            start++;
        }
        
        if (start >= json.length()) return "";

        // Si es String (comienza con comillas)
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return (end == -1) ? "" : json.substring(start, end);
        } else {
            // Si es número o booleano (hasta coma o llave de cierre)
            int endComma = json.indexOf(",", start);
            int endBrace = json.indexOf("}", start);
            
            int end = -1;
            if (endComma != -1 && endBrace != -1) {
                end = Math.min(endComma, endBrace);
            } else {
                end = (endComma != -1) ? endComma : endBrace;
            }
            
            if (end == -1) end = json.length();
            return json.substring(start, end).trim();
        }
    }

    private void activarModoRegistroNuevo(String codigo) {
        // Limpieza profunda de los campos de texto
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtStock.clear();
        
        // Fijación del código escaneado que no existe en BD
        txtCodigo.setText(codigo);
        txtCodigo.setEditable(false); // Bloqueado para mantener el 12 dígitos

        // Habilitación de campos de entrada para el nuevo producto
        txtNombre.setEditable(true);
        txtDescripcion.setEditable(true);
        txtPrecio.setEditable(true);
        txtStock.setEditable(true);

        // Estado visual de la interfaz
        lblContadorDesperdicio.setText("Código disponible. Ingrese los datos para registrar este nuevo producto.");
        
        // Bloqueo de acciones de impresión hasta completar validación de fila
        btnImprimir.setDisable(true);
        
        // Foco automático para eficiencia del trabajador
        txtNombre.requestFocus();
    }
    
    @FXML
    public void initialize() {
        btnImprimir.setDisable(true);
        lblContadorDesperdicio.setText("Planilla de Hoja: 0 / " + CAPACIDAD_HOJA);
    }

    @FXML
    public void handleAgregarFila() {
        if (validarCampos()) {
            if (registrosValidados < CAPACIDAD_HOJA) {
                registrosValidados++;
                actualizarEstadoUI();
                limpiarCampos();
            }
        } else {
            mostrarAlerta("Error de Validación", "Todos los campos deben estar completos y el código debe tener 12 dígitos.");
        }
    }

    private boolean validarCampos() {
        return !txtNombre.getText().isBlank() &&
               !txtDescripcion.getText().isBlank() &&
               !txtPrecio.getText().isBlank() &&
               txtCodigo.getText().matches("\\d{12}") &&
               !txtStock.getText().isBlank();
    }

    private void actualizarEstadoUI() {
        lblContadorDesperdicio.setText("Planilla de Hoja: " + registrosValidados + " / " + CAPACIDAD_HOJA);
        
        if (registrosValidados == CAPACIDAD_HOJA) {
            btnImprimir.setDisable(false);
            generador.dibujarCodigo(canvasPreview, txtCodigo.getText());
        }
    }

    @FXML
    private void handleImprimirYEnviar() {
                // 1. Obtener credenciales desde el gestor cargado en el arranque
        SecurityConfigLoader config = SecurityConfigLoader.getInstance();
        String url = "http://" + config.getServerIp() + ":" + config.getServerPort() + "/api/productos";

        // 2. Construir la conexión
        String jsonPayload = "{\"nombre\":\"Producto...\"}"; // Tu JSON

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.getApiToken()) // Conexión segura
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // 3. Ejecutar conexión de forma asíncrona
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("Conexión Exitosa");
                    }
                });
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtCodigo.clear();
        txtStock.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setContentText(mensaje);
        alert.show();
    }
}