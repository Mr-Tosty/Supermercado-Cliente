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
package com.vortexaronix.supermercado.Frontend.Util;

import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

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
public class GeneradorCodigo12Digitos {
    private static final Map<Integer, String> BITS = Map.of(
        0, "0001101", 1, "0011001", 2, "0010011", 3, "0111101", 
        4, "0100011", 5, "0110001", 6, "0101111", 7, "0111011", 
        8, "0110111", 9, "0001011"
    );

    public void dibujarCodigo(Canvas canvas, String codigo) {
        if (!codigo.matches("\\d{12}")) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.BLACK);

        double x = 10;
        // Patrón Inicio: 101
        dibujarBloque(gc, "101", x); x += 9;

        // Dígitos 1-6
        for(int i = 0; i < 6; i++) {
            dibujarBloque(gc, BITS.get(codigo.charAt(i) - '0'), x); x += 21;
        }

        // Central: 01010
        dibujarBloque(gc, "01010", x); x += 15;

        // Dígitos 7-12
        for(int i = 6; i < 12; i++) {
            dibujarBloque(gc, BITS.get(codigo.charAt(i) - '0'), x); x += 21;
        }

        // Patrón Fin: 101
        dibujarBloque(gc, "101", x);
    }

    private void dibujarBloque(GraphicsContext gc, String bits, double x) {
        for(int i = 0; i < bits.length(); i++) {
            if(bits.charAt(i) == '1') gc.fillRect(x + (i * 3), 10, 3, 50);
        }
    }
}