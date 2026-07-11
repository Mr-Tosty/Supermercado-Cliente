package com.vortexaronix.supermercado;

import com.vortexaronix.supermercado.Frontend.SecurityConfigLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Inicio extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Instancia y ejecuta la lógica de configuración robusta
            SecurityConfigLoader.getInstance().initializeConfiguration();
            
            // Si la inicialización es exitosa, cargamos la interfaz principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaRegistro.fxml"));
            primaryStage.setScene(new Scene(loader.load()));
            primaryStage.setTitle("Vortex Aronix - Sistema de Gestión");
            primaryStage.show();
            
        } catch (Exception e) {
            // Manejo de error: Si falla el cargador, no se levanta la app
            System.err.println("Error crítico de seguridad: " + e.getMessage());
            System.exit(1); 
        }
    }

    private void mostrarErrorFatal(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fallo de Seguridad");
        alert.setHeaderText("No se pudo iniciar el sistema");
        alert.setContentText(mensaje);
        alert.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}