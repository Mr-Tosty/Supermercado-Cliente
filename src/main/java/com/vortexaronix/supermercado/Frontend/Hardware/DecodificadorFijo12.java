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

    // Patrones de codificación estándar para el lado izquierdo del código de barras
    private static final int[][] L_PATTERNS = {
        {3, 2, 1, 1}, {2, 2, 2, 1}, {2, 1, 2, 2}, {1, 4, 1, 1}, {1, 1, 3, 2},
        {1, 2, 3, 1}, {1, 1, 1, 4}, {1, 3, 1, 2}, {1, 2, 1, 3}, {3, 1, 1, 2}
    };
    
    // Patrones de codificación estándar para el lado derecho del código de barras
    private static final int[][] R_PATTERNS = {
        {3, 2, 1, 1}, {2, 2, 2, 1}, {2, 1, 2, 2}, {1, 4, 1, 1}, {1, 1, 3, 2},
        {1, 2, 3, 1}, {1, 1, 1, 4}, {1, 3, 1, 2}, {1, 2, 1, 3}, {3, 1, 1, 2}
    };

    /**
     * Analiza la línea central de la imagen capturada por la cámara del supermercado.
     * Utiliza binarización adaptativa para aislar barras negras de espacios blancos.
     */
    public String decodificarLineaCentral(BufferedImage image) {
        if (image == null) return null;
        
        int width = image.getWidth();
        int centerY = image.getHeight() / 2;
        int[] pixels = new int[width];
        long sumaBrillo = 0;
        for (int x = 0; x < width; x++) {
            int rgb = image.getRGB(x, centerY);
            int luma = (int)(0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
            pixels[x] = luma;
            sumaBrillo += luma;
        }

        int umbralAdaptativo = (int) (sumaBrillo / width);
        boolean[] bits = new boolean[width];
        for (int x = 0; x < width; x++) {
            bits[x] = pixels[x] < umbralAdaptativo; // True = Barra Negra, False = Espacio Blanco
        }

        List<Integer> transiciones = new ArrayList<>();
        boolean estadoActual = bits[0];
        int contador = 1;

        for (int x = 1; x < width; x++) {
            if (bits[x] == estadoActual) {
                contador++;
            } else {
                transiciones.add(contador);
                estadoActual = bits[x];
                contador = 1;
            }
        }
        transiciones.add(contador);

        if (transiciones.size() < 59) return null;

        for (int i = 0; i <= transiciones.size() - 59; i++) {
            float unidadModulo = (transiciones.get(i) + transiciones.get(i + 1) + transiciones.get(i + 2)) / 3.0f;
            if (unidadModulo < 0.7f) continue;

            StringBuilder codigo = new StringBuilder();
            int puntero = i + 3;
            
            boolean exitoIzq = true;
            for (int d = 0; d < 6; d++) {
                int digito = mapearPatronConTolerancia(transiciones, puntero, unidadModulo, L_PATTERNS);
                if (digito == -1) { exitoIzq = false; break; }
                codigo.append(digito);
                puntero += 4;
            }
            if (!exitoIzq) continue;

            puntero += 5;
            
            boolean exitoDer = true;
            for (int d = 0; d < 6; d++) {
                int digito = mapearPatronConTolerancia(transiciones, puntero, unidadModulo, R_PATTERNS);
                if (digito == -1) { exitoDer = false; break; }
                codigo.append(digito);
                puntero += 4;
            }

            if (exitoDer && codigo.length() == 12) {
                return codigo.toString();
            }
        }
        return null;
    }

    /**
     * Compara las transiciones de la cámara contra la matriz de patrones usando 
     * algoritmos de distancia de error mínimo en lugar de redondeo estricto.
     */
    private int mapearPatronConTolerancia(List<Integer> transiciones, int inicio, float modulo, int[][] patrones) {
        if (inicio + 4 > transiciones.size()) return -1;

        float[] proporcionesReales = new float[4];
        float sumaProporcional = 0;
        for (int i = 0; i < 4; i++) {
            proporcionesReales[i] = transiciones.get(inicio + i) / modulo;
            sumaProporcional += proporcionesReales[i];
        }
        
        if (Math.abs(sumaProporcional - 7.0f) > 1.5f) return -1;

        int mejorDigitoDetectado = -1;
        float menorErrorRegistrado = Float.MAX_VALUE;
        
        for (int d = 0; d < patrones.length; d++) {
            float errorAcumulado = 0;
            for (int i = 0; i < 4; i++) {
                errorAcumulado += Math.abs(proporcionesReales[i] - patrones[d][i]);
            }

            if (errorAcumulado < menorErrorRegistrado && errorAcumulado < 1.8f) {
                menorErrorRegistrado = errorAcumulado;
                mejorDigitoDetectado = d;
            }
        }

        return mejorDigitoDetectado;
    }
}