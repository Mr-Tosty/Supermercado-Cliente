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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

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
    @FXML private Label lblUsuario, lblMac, lblAuditoria, lblError;
    @FXML private PasswordField txtTokenMaestro;
    @FXML private Button btnDesbloquear;

    @FXML
    public void initialize() {
        lblUsuario.setText("Usuario: " + System.getProperty("user.name"));
    }

    @FXML
    private void handleDesbloquear() {
        if (SecurityConfigLoader.validarTokenMaestroTI(txtTokenMaestro.getText())) {
            btnDesbloquear.getScene().getWindow().hide();
        } else {
            lblError.setText("Credencial inválida.");
            txtTokenMaestro.clear();
        }
    }
}