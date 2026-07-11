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

    public void verificarYCrearAccesoDirecto() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            // Lógica exclusiva de persistencia en entornos basados en Windows por especificación del método del sistema (.ico)
            return;
        }

        String userHome = System.getProperty("user.home");
        Path desktopPath = Paths.get(userHome, "Desktop");
        Path shortcutPath = desktopPath.resolve("SuperMarket Manager Pro.lnk");

        if (Files.exists(shortcutPath)) {
            return; 
        }

        // Extraer ejecutable JAR actual y recurso de icono
        Path currentJar = Paths.get(InstaladorAccesoDirecto.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        
        Path appDataDir = Paths.get(System.getenv("APPDATA"), "VortexAronix");
        Files.createDirectories(appDataDir);
        Path iconFile = appDataDir.resolve("appIcon.ico");

        if (!Files.exists(iconFile)) {
            try (InputStream is = InstaladorAccesoDirecto.class.getResourceAsStream("/assets/appIcon.ico")) {
                if (is != null) {
                    Files.copy(is, iconFile, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Fallback vacío por robustez si el recurso estructural no existe en el classpath raíz
                    Files.createFile(iconFile);
                }
            }
        }

        // Script VBScript inyectado dinámicamente para manipular el componente COM de Windows y la API Shell
        String vbsScript = 
            "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
            "sLinkFile = \"" + shortcutPath.toAbsolutePath().toString() + "\"\n" +
            "Set oLink = oWS.CreateShortcut(sLinkFile)\n" +
            "oLink.TargetPath = \"javaw.exe\"\n" +
            "oLink.Arguments = \"-jar \"\"" + currentJar.toAbsolutePath().toString() + "\"\"\"\n" +
            "oLink.Description = \"SuperMarket Manager Pro\"\n" +
            "oLink.IconLocation = \"" + iconFile.toAbsolutePath().toString() + ", 0\"\n" +
            "oLink.WorkingDirectory = \"" + currentJar.getParent().toAbsolutePath().toString() + "\"\n" +
            "oLink.Save\n";

        Path vbsFile = appDataDir.resolve("create_shortcut.vbs");
        Files.writeString(vbsFile, vbsScript);

        ProcessBuilder pb = new ProcessBuilder("wscript.exe", vbsFile.toAbsolutePath().toString());
        Process process = pb.start();
        process.waitFor();

        Files.deleteIfExists(vbsFile);
    }
    
}