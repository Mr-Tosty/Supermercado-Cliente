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
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * ----------------------------------------------------------------------------
 * [ DESCRIPCIÓN TÉCNICA ]
 * ----------------------------------------------------------------------------
 * Descripción : Tarea en segundo plano para captura de cámara y escaneo.
 * Módulos     : Frontend.Hardware
 * Dependencias: DecodificadorFijo12, JavaFX
 * 
 * @author  solda (VA Developer)
 * @version 1.0
 * @since   10 jul 2026
 * ----------------------------------------------------------------------------
 */
public class LectorCamaraTask implements Runnable {

    private final ImageView imageView;
    private final Consumer<String> onCodigoLeido;
    private final DecodificadorFijo12 decodificador;
    private volatile boolean activo;
    private long ultimoEscaneo;

    public LectorCamaraTask(ImageView imageView, Consumer<String> onCodigoLeido) {
        this.imageView = imageView;
        this.onCodigoLeido = onCodigoLeido;
        this.decodificador = new DecodificadorFijo12(); 
        this.activo = true;
        this.ultimoEscaneo = 0;
    }

    public void detener() {
        this.activo = false;
    }

    @Override
    public void run() {
        while (activo) {
            try {
                BufferedImage frame = capturarFrameSimulado(); 
                if (frame != null) {
                    String codigo = decodificador.decodificarLineaCentral(frame);
                    long ahora = System.currentTimeMillis();
                    if (codigo != null && !codigo.trim().isEmpty() && (ahora - ultimoEscaneo > 2000)) {
                        ultimoEscaneo = ahora;
                        Platform.runLater(() -> onCodigoLeido.accept(codigo));
                    }
                    Image fxImage = convertirAJavaFXImage(frame);
                    Platform.runLater(() -> imageView.setImage(fxImage));
                }
                Thread.sleep(33); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activo = false;
            } catch (Exception e) {
            }
        }
    }

    private BufferedImage capturarFrameSimulado() {
        return new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
    }

    private Image convertirAJavaFXImage(BufferedImage bImage) {
        if (bImage == null) return null;
        WritableImage wr = new WritableImage(bImage.getWidth(), bImage.getHeight());
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < bImage.getWidth(); x++) {
            for (int y = 0; y < bImage.getHeight(); y++) {
                pw.setArgb(x, y, bImage.getRGB(x, y));
            }
        }
        return wr;
    }
}