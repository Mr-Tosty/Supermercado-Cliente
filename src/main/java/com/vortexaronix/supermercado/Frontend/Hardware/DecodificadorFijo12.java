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

    private static final int[][] L_PATTERNS = {
        {3, 2, 1, 1}, {2, 2, 2, 1}, {2, 1, 2, 2}, {1, 4, 1, 1}, {1, 1, 3, 2},
        {1, 2, 3, 1}, {1, 1, 1, 4}, {1, 3, 1, 2}, {1, 2, 1, 3}, {3, 1, 1, 2}
    };
    private static final int[][] R_PATTERNS = {
        {3, 2, 1, 1}, {2, 2, 2, 1}, {2, 1, 2, 2}, {1, 4, 1, 1}, {1, 1, 3, 2},
        {1, 2, 3, 1}, {1, 1, 1, 4}, {1, 3, 1, 2}, {1, 2, 1, 3}, {3, 1, 1, 2}
    };

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
        for (int x = 0; x < width; x++) bits[x] = pixels[x] < umbralAdaptativo;
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
        for (int i = 0; i < transiciones.size() - 56; i++) {
            float unidadModulo = (transiciones.get(i) + transiciones.get(i+1) + transiciones.get(i+2)) / 3.0f;
            if (unidadModulo < 1.0f) continue;
            StringBuilder codigo = new StringBuilder();
            int puntero = i + 3; 
            boolean exitoIzq = true;
            for (int d = 0; d < 6; d++) {
                int digito = mapearPatron(transiciones, puntero, unidadModulo, L_PATTERNS);
                if (digito == -1) { exitoIzq = false; break; }
                codigo.append(digito);
                puntero += 4;
            }
            if (!exitoIzq) continue;
            puntero += 5;
            boolean exitoDer = true;
            for (int d = 0; d < 6; d++) {
                int digito = mapearPatron(transiciones, puntero, unidadModulo, R_PATTERNS);
                if (digito == -1) { exitoDer = false; break; }
                codigo.append(digito);
                puntero += 4;
            }
            if (exitoDer && codigo.length() == 12) return codigo.toString();
        }
        return null;
    }

    private int mapearPatron(List<Integer> transiciones, int inicio, float modulo, int[][] patrones) {
        if (inicio + 4 > transiciones.size()) return -1;
        int[] redondeado = new int[4];
        int sumaModulos = 0;
        for (int i = 0; i < 4; i++) {
            redondeado[i] = Math.round(transiciones.get(inicio + i) / modulo);
            if (redondeado[i] < 1 || redondeado[i] > 4) return -1;
            sumaModulos += redondeado[i];
        }
        if (sumaModulos != 7) return -1;
        for (int d = 0; d < patrones.length; d++) {
            if (patrones[d][0] == redondeado[0] && patrones[d][1] == redondeado[1] &&
                patrones[d][2] == redondeado[2] && patrones[d][3] == redondeado[3]) return d;
        }
        return -1;
    }
    
}