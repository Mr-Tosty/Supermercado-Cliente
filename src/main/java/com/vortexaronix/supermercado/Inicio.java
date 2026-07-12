package com.vortexaronix.supermercado;

import com.vortexaronix.supermercado.Frontend.SecurityConfigLoader;
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

    /**
     * Carga y parsea de forma estricta el archivo plano de configuración externa 'server.vaconect'.
     * Formato requerido: IP,PUERTO,TOKEN (Ej: 192.168.1.74,8080,VORTEX-PROD-2026-TOKEN)
     */
    private boolean cargarConfiguracion() {
        Path configPath = Paths.get("server.vaconect");
        if (!Files.exists(configPath)) {
            mostrarErrorFatal("Archivo de conexión crítica 'server.vaconect' no encontrado en la raíz: " + configPath.toAbsolutePath());
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            String linea = reader.readLine();
            if (linea == null || linea.strip().isEmpty()) {
                mostrarErrorFatal("El archivo 'server.vaconect' está vacío.");
                return false;
            }
            String[] partes = linea.split(",");
            if (partes.length < 3) {
                mostrarErrorFatal("Formato inválido en 'server.vaconect'. Se esperaba: IP,PUERTO,TOKEN");
                return false;
            }
            this.servidorIp = partes[0].strip();
            this.servidorPuerto = Integer.parseInt(partes[1].strip());
            this.tokenSeguridad = partes[2].strip();

            if (this.servidorIp.isEmpty() || this.tokenSeguridad.isEmpty()) {
                mostrarErrorFatal("La IP o el Token de seguridad no pueden estar vacíos.");
                return false;
            }
            ContextoConfiguracion.ip = this.servidorIp;
            ContextoConfiguracion.puerto = this.servidorPuerto;
            ContextoConfiguracion.token = this.tokenSeguridad;
            System.out.printf("[CONFIG] Conexión establecida exitosamente -> IP: %s | Puerto: %d%n", ContextoConfiguracion.ip, ContextoConfiguracion.puerto);
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

            // 3. CARGA DEL ACCESO DE SEGURIDAD MODAL (PANTALLA DE BLOQUEO DE TI)
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
            mostrarErrorFatal("Detalle del error: " + e.getLocalizedMessage());
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

    /**
     * Asegura que al cerrar la app se maten por completo todos los hilos
     * de red locales y de hardware en segundo plano de manera limpia.
     */
    private void shutdownSequence() {
        System.out.println("[CLIENTE] Cerrando recursos de red, sockets LAN y hardware...");
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}