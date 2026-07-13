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
package com.vortexaronix.supermercado.Frontend.Util;

import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

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

    // Bloque Izquierdo: Paridad Impar (L-Pattern) - Inicia con espacio, termina con barra
    private static final Map<Integer, String> L_PATTERNS = Map.of(
        0, "0001101", 1, "0011001", 2, "0010011", 3, "0111101", 4, "0100011",
        5, "0110001", 6, "0101111", 7, "0111011", 8, "0110111", 9, "0001011"
    );

    // Es el complemento óptico exacto del patrón L (las barras se vuelven espacios y viceversa)
    private static final Map<Integer, String> R_PATTERNS = Map.of(
        0, "1110010", 1, "1100110", 2, "1101100", 3, "1000010", 4, "1011100",
        5, "1001110", 6, "1010000", 7, "1000110", 8, "1001000", 9, "1110100"
    );

    /**
     * Dibuja un código UPC-A estandarizado adaptando las dimensiones al Canvas.
     * Incluye caracteres de guarda y tipografía inferior legible.
     */
    public void dibujarCodigo(Canvas canvas, String codigo) {
        if (canvas == null || codigo == null || !codigo.matches("\\d{12}")) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        int totalModulosEan = 95;
        double paddingLateral = 15.0;
        double moduloWidth = (canvasWidth - (paddingLateral * 2)) / totalModulosEan;
        
        double alturaBarrasNormales = canvasHeight * 0.70;
        double alturaBarrasDeGuarda = canvasHeight * 0.82;

        double currentX = paddingLateral;
        gc.setFill(Color.BLACK);

        currentX = dibujarMóduloBits(gc, "101", currentX, moduloWidth, alturaBarrasDeGuarda);

        for (int i = 0; i < 6; i++) {
            int digito = codigo.charAt(i) - '0';
            currentX = dibujarMóduloBits(gc, L_PATTERNS.get(digito), currentX, moduloWidth, alturaBarrasNormales);
        }

        currentX = dibujarMóduloBits(gc, "01010", currentX, moduloWidth, alturaBarrasDeGuarda);

        for (int i = 6; i < 12; i++) {
            int digito = codigo.charAt(i) - '0';
            currentX = dibujarMóduloBits(gc, R_PATTERNS.get(digito), currentX, moduloWidth, alturaBarrasNormales);
        }

        dibujarMóduloBits(gc, "101", currentX, moduloWidth, alturaBarrasDeGuarda);

        gc.setFont(Font.font("Monospaced", canvasHeight * 0.14));
        gc.setTextAlign(TextAlignment.CENTER);
        
        String textoFormateado = String.format("%s  %s", codigo.substring(0, 6), codigo.substring(6, 12));
        gc.fillText(textoFormateado, canvasWidth / 2.0, canvasHeight - (canvasHeight * 0.04));
    }

    /**
     * Procesa la cadena binaria de un módulo y dibuja rectángulos proporcionales.
     */
    private double dibujarMóduloBits(GraphicsContext gc, String bits, double startX, double moduloWidth, double altura) {
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                // Dibujar la barra vertical negra con precisión decimal
                gc.fillRect(startX, 5, moduloWidth + 0.2, altura); // El +0.2 elimina líneas blancas por redondeo de píxeles
            }
            startX += moduloWidth;
        }
        return startX;
    }
}