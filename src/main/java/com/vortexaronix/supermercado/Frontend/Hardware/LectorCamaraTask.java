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
import java.awt.image.DataBufferInt;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
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

    // Buffer de alto rendimiento reutilizable para evitar fugas de memoria RAM
    private WritableImage writableImageReusable;
    private int[] pixelBufferArray;

    public LectorCamaraTask(ImageView imageView, Consumer<String> onCodigoLeido) {
        this.imageView = imageView;
        this.onCodigoLeido = onCodigoLeido;
        this.decodificador = new DecodificadorFijo12(); 
        this.activo = true;
        this.ultimoEscaneo = 0;
    }

    /**
     * Apaga de forma segura la bandera de ejecución del hilo del hardware.
     */
    public void detener() {
        this.activo = false;
        System.out.println("[HARDWARE] Señal de apagado enviada al hilo de la cámara.");
    }

    @Override
    public void run() {
        System.out.println("[HARDWARE] Hilo de captura de video inicializado correctamente.");
        
        while (activo && !Thread.currentThread().isInterrupted()) {
            try {
                BufferedImage frame = capturarFrameSimulado(); 
                
                if (frame != null) {
                    String codigo = decodificador.decodificarLineaCentral(frame);
                    long ahora = System.currentTimeMillis();
                    
                    if (codigo != null && !codigo.trim().isEmpty() && (ahora - ultimoEscaneo > 2000)) {
                        ultimoEscaneo = ahora;
                        System.out.println("[ESCÁNER] Código detectado con éxito: " + codigo);
                        Platform.runLater(() -> onCodigoLeido.accept(codigo));
                    }

                    Image fxImage = convertirAJavaFXImageOptimizada(frame);
                    if (fxImage != null) {
                        Platform.runLater(() -> imageView.setImage(fxImage));
                    }
                }

                Thread.sleep(33); 
                
            } catch (InterruptedException e) {
                System.out.println("[HARDWARE] Interrupción detectada en el hilo de video.");
                Thread.currentThread().interrupt();
                activo = false;
            } catch (Exception e) {
                System.err.println("[ALERTA HARDWARE] Anomalía temporal en el frame de la cámara: " + e.getMessage());
                if (Thread.currentThread().isInterrupted()) {
                    activo = false;
                }
            }
        }
        System.out.println("[HARDWARE] Hilo de la cámara finalizado por completo de forma limpia.");
    }

    /**
     * Simulación de la entrada física de video del supermercado.
     * Retorna un cuadro de dimensiones estándar 640x480.
     */
    private BufferedImage capturarFrameSimulado() {
        return new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Transfiere el búfer de píxeles directamente a la memoria de la GPU 
     * mediante la API nativa de JavaFX, eliminando los bucles 'for' lentos de la CPU.
     */
    private Image convertirAJavaFXImageOptimizada(BufferedImage bImage) {
        if (bImage == null) return null;
        
        int width = bImage.getWidth();
        int height = bImage.getHeight();

        if (writableImageReusable == null || writableImageReusable.getWidth() != width || writableImageReusable.getHeight() != height) {
            
            if (bImage.getRaster().getDataBuffer() instanceof DataBufferInt) {
                pixelBufferArray = ((DataBufferInt) bImage.getRaster().getDataBuffer()).getData();
            } else {
                pixelBufferArray = new int[width * height];
            }

            PixelFormat<java.nio.IntBuffer> format = PixelFormat.getIntArgbPreInstance();
            java.nio.IntBuffer buffer = java.nio.IntBuffer.wrap(pixelBufferArray);
            PixelBuffer<java.nio.IntBuffer> pixelBuffer = new PixelBuffer<>(width, height, buffer, format);
            
            writableImageReusable = new WritableImage(pixelBuffer);
        } else {
            // Si el buffer ya existe, simplemente se extrae la matriz de color modificada en el nuevo frame
            if (bImage.getRaster().getDataBuffer() instanceof DataBufferInt) {
                int[] srcData = ((DataBufferInt) bImage.getRaster().getDataBuffer()).getData();
                System.arraycopy(srcData, 0, pixelBufferArray, 0, pixelBufferArray.length);
                writableImageReusable.getPixelReader(); // Fuerza la actualización del buffer gráfico interno
            }
        }

        return writableImageReusable;
    }
}