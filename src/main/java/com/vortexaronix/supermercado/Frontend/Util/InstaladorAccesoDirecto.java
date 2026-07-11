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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
public class InstaladorAccesoDirecto {

    /**
     * Verifica la existencia y aprovisiona el acceso directo en el escritorio del usuario.
     * Fuerza la sincronización de las carpetas de ejecución para evitar fallos de lectura criptográfica.
     */
    public void verificarYCrearAccesoDirecto() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return;
        }

        String userHome = System.getProperty("user.home");
        Path desktopPath = Paths.get(userHome, "Desktop");
        Path shortcutPath = desktopPath.resolve("SuperMarket Manager Pro.lnk");

        if (Files.exists(shortcutPath)) {
            return; 
        }
        
        Path currentJar = Paths.get(InstaladorAccesoDirecto.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
       
        Path carpetaDeEjecucionReal = currentJar.getParent().toAbsolutePath();

        Path appDataDir = Paths.get(System.getenv("APPDATA"), "VortexAronix");
        Files.createDirectories(appDataDir);
        Path iconFile = appDataDir.resolve("appIcon.ico");

        if (!Files.exists(iconFile)) {
            try (InputStream is = InstaladorAccesoDirecto.class.getResourceAsStream("/assets/appIcon.ico")) {
                if (is != null) {
                    Files.copy(is, iconFile, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.createFile(iconFile);
                }
            }
        }
        
        String jarRutaEscapada = currentJar.toAbsolutePath().toString().replace("\\", "\\\\");
        String carpetaRutaEscapada = carpetaDeEjecucionReal.toString().replace("\\", "\\\\");
        String iconoRutaEscapada = iconFile.toAbsolutePath().toString().replace("\\", "\\\\");

        String vbsScript = 
            "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
            "sLinkFile = \"" + shortcutPath.toAbsolutePath().toString() + "\"\n" +
            "Set oLink = oWS.CreateShortcut(sLinkFile)\n" +
            "oLink.TargetPath = \"javaw.exe\"\n" +
            "oLink.Arguments = \"-jar " + jarRutaEscapada + "\"\n" +
            "oLink.Description = \"SuperMarket Manager Pro - Sistema de Gestión Comercial\"\n" +
            "oLink.IconLocation = \"" + iconoRutaEscapada + ", 0\"\n" +
            "oLink.WorkingDirectory = \"" + carpetaRutaEscapada + "\"\n" +
            "oLink.Save\n";

        Path vbsFile = appDataDir.resolve("create_shortcut.vbs");

        Files.writeString(vbsFile, vbsScript);

        ProcessBuilder pb = new ProcessBuilder("wscript.exe", vbsFile.toAbsolutePath().toString());
        Process process = pb.start();
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("[ALERTA BUILD] WScript finalizó con un código de salida anómalo durante la instalación.");
        }

        Files.deleteIfExists(vbsFile);
        System.out.println("[DESPLIEGUE] Acceso directo nativo 'SuperMarket Manager Pro' instalado con éxito en el escritorio.");
    }
}