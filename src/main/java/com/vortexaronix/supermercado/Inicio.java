package com.vortexaronix.supermercado;

import com.vortexaronix.supermercado.Frontend.SecurityConfigLoader;
import com.vortexaronix.supermercado.Frontend.VentanaRegistroController;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Inicio extends Application {

    private String servidorIp;
    private int servidorPuerto;
    private String tokenSeguridad;

    public static class ContextoConfiguracion {

        public static String ip;
        public static int puerto;
        public static String token;
    }

    private boolean cargarConfiguracion() {
        Path configPath = Paths.get("server.vaconect");
        if (!Files.exists(configPath)) {
            mostrarErrorFatal("Archivo de conexión crítica 'server.vaconect' no encontrado en la raíz: " + configPath.toAbsolutePath());
            return false;
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath, java.nio.charset.StandardCharsets.UTF_8)) {
            java.util.Properties propiedadesConfig = new java.util.Properties();
            propiedadesConfig.load(reader);

            this.servidorIp = propiedadesConfig.getProperty("server.ip");
            String puertoStr = propiedadesConfig.getProperty("server.port");
            this.tokenSeguridad = propiedadesConfig.getProperty("server.token");

            if (this.servidorIp == null || puertoStr == null || this.tokenSeguridad == null) {
                mostrarErrorFatal("Formato inválido en 'server.vaconect'. Se esperaban las propiedades: server.ip, server.port y server.token");
                return false;
            }

            this.servidorIp = this.servidorIp.strip();
            this.servidorPuerto = Integer.parseInt(puertoStr.strip());
            this.tokenSeguridad = this.tokenSeguridad.strip();

            if (this.servidorIp.isEmpty() || this.tokenSeguridad.isEmpty()) {
                mostrarErrorFatal("La IP o el Token de seguridad no pueden estar vacíos.");
                return false;
            }

            ContextoConfiguracion.ip = this.servidorIp;
            ContextoConfiguracion.puerto = this.servidorPuerto;
            ContextoConfiguracion.token = this.tokenSeguridad;

            System.out.printf("[CONFIG] Conexión establecida exitosamente -> IP: %s | Puerto: %d%n",
                    ContextoConfiguracion.ip, ContextoConfiguracion.puerto);
            return true;

        } catch (IOException e) {
            mostrarErrorFatal("Error de Entrada/Salida al leer 'server.vaconect': " + e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            mostrarErrorFatal("El puerto especificado en 'server.vaconect' no es un número válido.");
            return false;
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        if (!cargarConfiguracion()) {
            return;
        }

        try {
            SecurityConfigLoader.getInstance().initializeConfiguration();
            
            ContextoConfiguracion.ip = SecurityConfigLoader.getInstance().getServerIp();
            ContextoConfiguracion.puerto = SecurityConfigLoader.getInstance().getServerPort();
            ContextoConfiguracion.token = SecurityConfigLoader.getInstance().getApiToken();

            if (ContextoConfiguracion.ip == null || ContextoConfiguracion.token == null || ContextoConfiguracion.puerto <= 0) {
                throw new IllegalStateException("Los parámetros de red descifrados contienen valores nulos o inválidos.");
            }

            FXMLLoader lockLoader = new FXMLLoader(getClass().getResource("/fxml/PantallaBloqueo.fxml"));
            Parent lockRoot = lockLoader.load();

            Stage lockStage = new Stage();
            lockStage.initOwner(stage);
            lockStage.initModality(Modality.APPLICATION_MODAL);
            lockStage.initStyle(StageStyle.UNDECORATED);
            lockStage.setTitle("Acceso Restringido - TI Perimetral");
            lockStage.setScene(new Scene(lockRoot));

            lockStage.setOnCloseRequest(e -> {
                e.consume();
                shutdownSequence();
            });

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VentanaRegistro.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setTitle("Sistema de Gestión Comercial - Supermercado (VORTEX-PROD)");
            stage.setScene(scene);

            stage.setOnCloseRequest(event -> {
                event.consume();
                shutdownSequence();
            });

            System.out.println("[PERÍMETRO] Bloqueo gráfico activo. Requiere Token Maestro de TI...");
            lockStage.showAndWait();

            stage.show();
            System.out.println("[CLIENTE] Interfaz comercial de registro liberada con éxito en la LAN.");

        } catch (IOException | NullPointerException e) {
            System.err.println("[CRÍTICO] Fallo en el arranque de infraestructura: " + e.getMessage());
            System.err.println("\n[🔍 COMPILADOR TELEMETRÍA - DIAGNÓSTICO EN TIEMPO REAL]");
            System.err.println("======================================================================");
            System.err.println("► Clase del Fallo: " + e.getClass().getName());
            System.err.println("► Causa Raíz FXML: " + e.getCause());
            System.err.println("► Mensaje del Kernel: " + e.getMessage());
            System.err.println("======================================================================\n");
            mostrarErrorFatal("Detalle del error: " + e.getLocalizedMessage());
            throw e;
        }
    }

    private void mostrarErrorFatal(String mensaje) {
        if (Platform.isFxApplicationThread()) {
            ejecutarAlertaYSalir(mensaje);
        } else {
            Platform.runLater(() -> ejecutarAlertaYSalir(mensaje));
        }
    }

    private void ejecutarAlertaYSalir(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Infraestructura Técnica");
        alert.setHeaderText("Carga de Aplicación Cancelada");
        alert.setContentText(mensaje);
        alert.showAndWait();
        shutdownSequence();
    }

    private void shutdownSequence() {
        System.out.println("[CLIENTE] Cerrando recursos de red, sockets LAN y hardware...");
        System.out.println("[CLIENTE] Cerrando recursos de red, sockets LAN y hardware...");

        if (VentanaRegistroController.getTareaLectorCamara() != null) {
            VentanaRegistroController.getTareaLectorCamara().detenerDispositivoHardware();
        }
        Platform.exit();
        System.exit(0);
    }
}
