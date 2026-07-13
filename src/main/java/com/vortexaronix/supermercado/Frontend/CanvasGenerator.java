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