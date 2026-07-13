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
 *              Para mas informacion en https://vortexaronix.com/FAQs/App/Escritorio/Modificaciones.
 * ----------------------------------------------------------------------------
 */
package com.vortexaronix.supermercado.Frontend;

import com.vortexaronix.supermercado.Inicio.ContextoConfiguracion;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.paint.Color;

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
public class PantallaBloqueoController {

    @FXML private Label lblUsuario;
    @FXML private Label lblMac;
    @FXML private Label lblAuditoria;
    @FXML private Label lblError;
    @FXML private PasswordField txtTokenMaestro;
    @FXML private Button btnDesbloquear;

    /**
     * Inicializa los metadatos de auditoría física en el panel de bloqueo de seguridad.
     * Limpia advertencias del IDE inyectando variables dinámicas de entorno.
     */
    @FXML
    public void initialize() {
        lblUsuario.setText("Usuario: " + System.getProperty("user.name"));
        String ipLocal = detectarIpClienteMesaAyuda();
        lblMac.setText("IP Terminal: " + ipLocal);
        
        lblAuditoria.setText("Entorno: VORTEX-PROD-2026");
        
        Platform.runLater(() -> txtTokenMaestro.requestFocus());
    }

    /**
     * Intercepta la acción de desbloqueo evaluando el Token Maestro mediante Reflexión FXML.
     */
    @FXML
    @SuppressWarnings("unused")
    private void handleDesbloquear() {
        String tokenIngresado = txtTokenMaestro.getText();

        if (tokenIngresado == null || tokenIngresado.strip().isEmpty()) {
            lblError.setTextFill(Color.RED);
            lblError.setText("Ingrese una credencial.");
            return;
        }

        if (SecurityConfigLoader.validarTokenMaestroTI(tokenIngresado)) {
            System.out.println("[SEGURIDAD] Credencial de TI autorizada. Levantando bloqueo de terminal.");
            
            if (btnDesbloquear.getScene() != null && btnDesbloquear.getScene().getWindow() != null) {
                btnDesbloquear.getScene().getWindow().hide(); // Cierre limpio de la pasarela de bloqueo
            }
        } else {
            System.err.println("[ALERTA SEGURIDAD] Intento fallido de desbloqueo con token maestro inválido.");
            lblError.setTextFill(Color.RED);
            lblError.setText("Token maestro TI inválido.");
            txtTokenMaestro.clear();
            txtTokenMaestro.requestFocus();
        }
    }

    /**
     * Utiliza un socket virtual UDP rápido hacia la dirección del servidor configurado
     * para extraer la IP exacta de la subred del cliente de cara a soporte técnico.
     */
    private String detectarIpClienteMesaAyuda() {
        String ipServidor = ContextoConfiguracion.ip != null ? ContextoConfiguracion.ip : "8.8.8.8";
        int puertoServidor = ContextoConfiguracion.puerto > 0 ? ContextoConfiguracion.puerto : 10002;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(ipServidor), puertoServidor);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ex) {
                return "127.0.0.1";
            }
        }
    }
}