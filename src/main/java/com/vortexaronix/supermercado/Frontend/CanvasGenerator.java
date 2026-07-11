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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

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
public class CanvasGenerator {

    private static final GeneradorCodigo12Digitos GENERADOR_REAL = new GeneradorCodigo12Digitos();

    /**
     * Limpia el Canvas del Frontend y renderiza una simbología de barras industrial real.
     * Sincronizado dinámicamente con los requerimientos de escaneo y hardware.
     */
    public static void renderCodeMarkup(Canvas canvas, String codigo) {
        if (canvas == null) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (codigo == null || !codigo.matches("\\d{12}")) {
            System.err.println("[RENDER] Advertencia: Intento de renderizado cancelado por código malformado.");
            return;
        }

        try{
            GENERADOR_REAL.dibujarCodigo(canvas, codigo);
        } catch (Exception e) {
            System.err.println("[RENDER CRÍTICO] Error al procesar la matriz de bits del Canvas: " + e.getMessage());
        }
    }
}