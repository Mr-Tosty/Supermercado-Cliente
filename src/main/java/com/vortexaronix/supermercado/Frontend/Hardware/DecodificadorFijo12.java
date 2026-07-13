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
package com.vortexaronix.supermercado.Frontend.Hardware;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class DecodificadorFijo12 {

    private static final Map<Integer, String> L_PATTERNS = Map.of(
        0, "0001101", 1, "0011001", 2, "0010011", 3, "0111101", 4, "0100011",
        5, "0110001", 6, "0101111", 7, "0111011", 8, "0110111", 9, "0001011"
    );

    private static final Map<Integer, String> R_PATTERNS = Map.of(
        0, "1110010", 1, "1100110", 2, "1101100", 3, "1000010", 4, "1011100",
        5, "1001110", 6, "1010000", 7, "1000110", 8, "1001000", 9, "1110100"
    );

    public static String procesarTransiciones(int[] transiciones, double modulo) {
        for (int i = 0; i <= transiciones.length - 59; i++) {
            double guardawidth = transiciones[i] + transiciones[i + 1] + transiciones[i + 2];
            if (Math.abs((guardawidth / 3.0) - modulo) > modulo * 0.5) continue;

            StringBuilder codigoBuilder = new StringBuilder();
            int puntero = i + 3;
            boolean exitoIzquierdo = true;

            for (int d = 0; d < 6; d++) {
                int digito = mapearPatronHardware(transiciones, puntero, modulo, L_PATTERNS);
                if (digito == -1) { exitoIzquierdo = false; break; }
                codigoBuilder.append(digito);
                puntero += 4;
            }
            if (!exitoIzquierdo) continue;

            puntero += 5; // Saltar guarda central

            boolean exitoDerecho = true;
            for (int d = 0; d < 6; d++) {
                int digito = mapearPatronHardware(transiciones, puntero, modulo, R_PATTERNS);
                if (digito == -1) { exitoDerecho = false; break; }
                codigoBuilder.append(digito);
                puntero += 4;
            }

            if (exitoDerecho && codigoBuilder.length() == 12) {
                return codigoBuilder.toString();
            }
        }
        return null;
    }

    private static int mapearPatronHardware(int[] transiciones, int inicio, double modulo, Map<Integer, String> patrones) {
        if (inicio + 4 > transiciones.length) return -1;
        int mejorDigito = -1;
        double menorError = Double.MAX_VALUE;

        for (Map.Entry<Integer, String> entrada : patrones.entrySet()) {
            double error = 0;
            String patronTeorico = entrada.getValue();
            for (int i = 0; i < 4; i++) {
                double anchoTeorico = Character.getNumericValue(patronTeorico.charAt(i)) * modulo;
                error += Math.abs(transiciones[inicio + i] - anchoTeorico);
            }
            if (error < menorError && error < modulo * 2.0) {
                menorError = error;
                mejorDigito = entrada.getKey();
            }
        }
        return mejorDigito;
    }
}