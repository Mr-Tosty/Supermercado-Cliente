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

    // Inyección obligatoria de Campos de Entrada de Texto FXML
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

    // Inyección obligatoria de Componentes de Alerta y Estado de UI
    @FXML
    private Label lblContadorDesperdicio;
    @FXML
    private Label lblAlertaCam;
    @FXML
    private Label lblStatusNet;

    // Controles de Acción y Renderizado Óptico
    @FXML
    private Button btnImprimir;
    @FXML
    private Canvas canvasPreview;

    private final int CAPACIDAD_HOJA = 9;
    private int registrosValidados = 0;
    private boolean filaActualYaContabilizada = false;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String urlBaseServer;

    @FXML
    public void initialize() {
        urlBaseServer = "http://" + SecurityConfigLoader.getInstance().getServerIp()
                + ":" + SecurityConfigLoader.getInstance().getServerPort() + "/api/productos";

        ChangeListener<String> listenerCamposPlanta = (observable, oldValue, newValue) -> evaluarYAcumularPlanillaDinamica();

        txtNombre.textProperty().addListener(listenerCamposPlanta);
        txtDescripcion.textProperty().addListener(listenerCamposPlanta);
        txtPrecio.textProperty().addListener(listenerCamposPlanta);
        txtCodigo.textProperty().addListener(listenerCamposPlanta);
        txtStock.textProperty().addListener(listenerCamposPlanta);

        btnImprimir.setDisable(true);
        lblContadorDesperdicio.setText("Planilla de Hoja: " + registrosValidados + " / " + CAPACIDAD_HOJA);
        txtCodigo.setEditable(true);
        lblStatusNet.setText("Red LAN: Activa -> Canal de datos apuntando a " + urlBaseServer);
    }

    private void evaluarYAcumularPlanillaDinamica() {
        // Corrección: Escapado doble para la expresión regular
        boolean todosCamposCompletos = !txtNombre.getText().isBlank()
                && !txtDescripcion.getText().isBlank()
                && !txtPrecio.getText().isBlank()
                && txtCodigo.getText().matches("\\d{12}")
                && !txtStock.getText().isBlank();

        if (todosCamposCompletos && !filaActualYaContabilizada) {
            if (registrosValidados < CAPACIDAD_HOJA) {
                registrosValidados++;
                filaActualYaContabilizada = true;
                evaluarBloqueoDeImpresion();
            }
        } else if (!todosCamposCompletos && filaActualYaContabilizada) {
            if (registrosValidados > 0) {
                registrosValidados--;
                filaActualYaContabilizada = false;
                evaluarBloqueoDeImpresion();
            }
        }
    }

    private void evaluarBloqueoDeImpresion() {
        lblContadorDesperdicio.setText("Planilla de Hoja: " + registrosValidados + " / " + CAPACIDAD_HOJA);

        if (registrosValidados == CAPACIDAD_HOJA) {
            btnImprimir.setDisable(false);
            CanvasGenerator.renderCodeMarkup(canvasPreview, txtCodigo.getText().trim());
            lblAlertaCam.setText("Estado: Planilla Llena. Listo para despacho.");
        } else {
            btnImprimir.setDisable(true);
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleImprimirYEnviar(ActionEvent event) {
        if (registrosValidados != CAPACIDAD_HOJA) {
            return;
        }

        String jsonPayload = String.format(
                "{\"codigoBarras\":\"%s\",\"nombre\":\"%s\",\"descripcion\":\"%s\",\"precio\":%s,\"stockActual\":%s}",
                txtCodigo.getText().trim(), txtNombre.getText().trim(), txtDescripcion.getText().trim(),
                txtPrecio.getText().trim(), txtStock.getText().trim()
        );

        Thread.startVirtualThread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlBaseServer + "/generate-hibrido"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SecurityConfigLoader.getInstance().getApiToken())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    System.out.println("[LAN-POST] Sincronización exitosa.");
                } else {
                    System.err.println("[LAN-ERROR] Servidor rechazó la inyección: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[LAN-ERROR] Fallo crítico: " + e.getMessage());
            }
        });

        registrosValidados = 0;
        filaActualYaContabilizada = false;
        evaluarBloqueoDeImpresion();
        limpiarCamposFormulario();
    }

    @FXML
    @SuppressWarnings("unused")
    public void handleAgregarFila(ActionEvent event) {
        limpiarCamposFormulario();
        filaActualYaContabilizada = false;
        lblAlertaCam.setText("Estado: Nueva fila habilitada para monitoreo.");
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleGenerateCryptoCode(ActionEvent event) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        String codigoGenerado = sb.toString();

        txtCodigo.setText(codigoGenerado);
        txtCodigo.setEditable(false);
        CanvasGenerator.renderCodeMarkup(canvasPreview, codigoGenerado);
    }

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
                    } else if (response.statusCode() == 404) {
                        limpiarCamposFormulario();
                        txtCodigo.setText(codigo.trim());
                        txtCodigo.setEditable(false);
                        lblContadorDesperdicio.setText("Código disponible. Ingrese los datos.");
                        txtNombre.requestFocus();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblStatusNet.setText("Red LAN: Error de conexión."));
            }
        });
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
        // Corrección crítica: Escapado correcto de comillas en Java
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
            int end = (endComma != -1 && (endBrace == -1 || endComma < endBrace)) ? endComma : endBrace;
            if (end == -1) {
                end = json.length();
            }
            return json.substring(start, end).trim();
        }
    }
}
