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

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * ----------------------------------------------------------------------------
 * [ DESCRIPCIÓN TÉCNICA ]
 * ----------------------------------------------------------------------------
 * Descripción : Tarea en segundo plano para captura de cámara y escaneo.
 * Módulos : Frontend.Hardware Dependencias: DecodificadorFijo12, JavaFX
 *
 * @author solda (VA Developer)
 * @version 1.0
 * @since 10 jul 2026
 * ----------------------------------------------------------------------------
 */
public class LectorCamaraTask implements Runnable {

    private final ImageView visorGraficoFxml;
    private final Consumer<String> callbackCodigoDetectado;
    private volatile boolean ejecucionActiva = true;
    private long ultimoEscaneoExitoso = 0;

    // Instancia del descriptor del hardware físico de la cámara
    private Webcam webcamDispositivo = null;

    public LectorCamaraTask(ImageView visorGraficoFxml, Consumer<String> callbackCodigoDetectado) {
        this.visorGraficoFxml = visorGraficoFxml;
        this.callbackCodigoDetectado = callbackCodigoDetectado;
        inicializarLenteFisico();
    }

    /**
     * Interroga el sistema operativo y activa el canal de datos de la cámara
     * USB.
     */
    private void inicializarLenteFisico() {
        try {
            // Obtener la cámara web predeterminada del equipo
            webcamDispositivo = Webcam.getDefault();
            if (webcamDispositivo != null) {
                // Forzar resolución estándar de escaneo optimizada (640x480)
                webcamDispositivo.setViewSize(WebcamResolution.VGA.getSize());
                webcamDispositivo.open(); // Encender físicamente el lente (Activa el led de la cámara)
                System.out.println("[HARDWARE-LENTE] Lente físico USB encendido e inicializado.");
            } else {
                System.err.println("[HARDWARE-ERROR] No se detectó ninguna cámara web física conectada por USB.");
            }
        } catch (Exception e) {
            System.err.println("[HARDWARE-ERROR] Fallo crítico al abrir los drivers de la cámara: " + e.getMessage());
        }
    }

    public void detenerDispositivoHardware() {
        this.ejecucionActiva = false;
        if (webcamDispositivo != null && webcamDispositivo.isOpen()) {
            webcamDispositivo.close(); // Apagar el lente físicamente para liberar el hardware
            System.out.println("[HARDWARE-LENTE] Lente físico apagado limpiamente.");
        }
    }

    @Override
    public void run() {
        if (webcamDispositivo == null || !webcamDispositivo.isOpen()) {
            System.err.println("[HARDWARE-ERROR] Bucle cancelado: El driver de video no está activo.");
            return;
        }

        while (ejecucionActiva && !Thread.currentThread().isInterrupted()) {
            try {
                // 1. CAPTURA REAL: Extraer el fotograma vivo directamente del lente USB
                BufferedImage frameVideo = webcamDispositivo.getImage();

                if (frameVideo != null) {
                    // Dibujar el láser guía virtual rojo sobre el buffer antes de enviarlo a pantalla
                    java.awt.Graphics2D g = frameVideo.createGraphics();
                    g.setColor(java.awt.Color.RED);
                    g.setStroke(new java.awt.BasicStroke(2));
                    g.drawLine(50, 240, 590, 240); // Láser guía visual en Y/2
                    g.dispose();

                    // 2. BINARIZACIÓN ADAPTATIVA: Procesar la línea central horizontal (Y/2)
                    String codigoBarrasUPC = analizarLuminanciaCentral(frameVideo);
                    long ahora = System.currentTimeMillis();

                    // Delay de bloqueo de 2 segundos mandatorio anti-duplicados
                    if (codigoBarrasUPC != null && (ahora - ultimoEscaneoExitoso > 2000)) {
                        ultimoEscaneoExitoso = ahora;
                        String codigoFinal = codigoBarrasUPC;
                        Platform.runLater(() -> callbackCodigoDetectado.accept(codigoFinal));
                    }

                    // 3. RENDERIZADO EN VIVO: Pintar el cuadro real en el ImageView del FXML
                    WritableImage imagenFx = transferirBufferAJavaFX(frameVideo);
                    Platform.runLater(() -> visorGraficoFxml.setImage(imagenFx));
                }

                Thread.sleep(33); // Mantener 30 FPS reales

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ejecucionActiva = false;
            } catch (Exception e) {
                System.err.println("[HARDWARE-ERROR] Error en el buffer de video vivo: " + e.getMessage());
            }
        }
        detenerDispositivoHardware(); // Asegurar liberación en el apagado
    }

    private String analizarLuminanciaCentral(BufferedImage img) {
        int width = img.getWidth();
        int centerY = img.getHeight() / 2; // Línea central Y/2
        long sumaBrilloTotal = 0;
        int[] canalGris = new int[width];

        for (int x = 0; x < width; x++) {
            int rgb = img.getRGB(x, centerY);
            int luma = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
            canalGris[x] = luma;
            sumaBrilloTotal += luma;
        }

        int umbralAdaptativo = (int) (sumaBrilloTotal / width);
        boolean[] matrizBits = new boolean[width];
        for (int x = 0; x < width; x++) {
            matrizBits[x] = canalGris[x] < umbralAdaptativo;
        }

        List<Integer> listaTransiciones = new ArrayList<>();
        boolean estadoActual = matrizBits[0];
        int contadorModulos = 1;

        for (int x = 1; x < width; x++) {
            if (matrizBits[x] == estadoActual) {
                contadorModulos++;
            } else {
                listaTransiciones.add(contadorModulos);
                estadoActual = matrizBits[x];
                contadorModulos = 1;
            }
        }
        listaTransiciones.add(contadorModulos);

        int[] transicionesArr = listaTransiciones.stream().mapToInt(Integer::intValue).toArray();
        double moduloEstimado = width / 95.0;
        
        return DecodificadorFijo12.procesarTransiciones(transicionesArr, moduloEstimado);
    }

    private WritableImage transferirBufferAJavaFX(BufferedImage bImage) {
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
