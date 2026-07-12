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
package com.vortexaronix.supermercado.Frontend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

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
public class SecurityConfigLoader {
    private static final String CLAVE_ESTATICA = "VortexLanCrypt12"; 
    
    private static SecurityConfigLoader instance;
    private String serverIp;
    private int serverPort;
    private String serverToken;

    public static SecurityConfigLoader getInstance() {
        if (instance == null) {
            instance = new SecurityConfigLoader();
        }
        return instance;
    }

    /**
     * Valida el Token Maestro de TI para levantar la pantalla perimetral de bloqueo.
     */
    public static boolean validarTokenMaestroTI(String token) {
        return "TI-MASTER-VORTEX-2026".equals(token != null ? token.trim() : "");
    }

    /**
     * Ejecuta el flujo dinámico de verificación, cifrado y autodestrucción en el disco duro.
     */
    public void initializeConfiguration() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        Path secureDir = os.contains("win") ? 
                Paths.get(System.getenv("APPDATA"), "VortexAronix") : 
                Paths.get(System.getProperty("user.home"), ".config", "vortexaronix");
        
        Path secureFile = secureDir.resolve("network.enc");

        if (Files.exists(secureFile)) {
            String contenidoBase64 = Files.readString(secureFile, StandardCharsets.UTF_8).trim();
            byte[] bytesCifrados = Base64.getDecoder().decode(contenidoBase64);

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(CLAVE_ESTATICA.getBytes(StandardCharsets.UTF_8), "AES"));
            
            String datosDescifrados = new String(cipher.doFinal(bytesCifrados), StandardCharsets.UTF_8);
            String[] tokens = datosDescifrados.split("\\|");
            
            this.serverIp = tokens[0];
            this.serverPort = Integer.parseInt(tokens[1]);
            this.serverToken = tokens[2];
            
            System.out.println("[CLIENTE-SECURITY] Configuración segura cargada exitosamente desde la ruta profunda del S.O.");
        } else {
            Path rawConfig = Paths.get("server.vaconect");
            if (!Files.exists(rawConfig)) {
                throw new FileNotFoundException("Infraestructura ausente. No se encontró 'network.enc' ni 'server.vaconect'.");
            }

            Properties props = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(rawConfig, StandardCharsets.UTF_8)) {
                props.load(reader);
            }

            this.serverIp = props.getProperty("server.ip");
            String puertoStr = props.getProperty("server.port");
            this.serverToken = props.getProperty("server.token");

            if (this.serverIp == null || puertoStr == null || this.serverToken == null) {
                throw new IllegalStateException("El archivo 'server.vaconect' existe pero sus propiedades internas están corruptas.");
            }
            this.serverPort = Integer.parseInt(puertoStr.trim());

            String rawPayload = this.serverIp + "|" + this.serverPort + "|" + this.serverToken;
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(CLAVE_ESTATICA.getBytes(StandardCharsets.UTF_8), "AES"));
            
            byte[] encryptedBytes = cipher.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String base64Payload = Base64.getEncoder().encodeToString(encryptedBytes);

            Files.createDirectories(secureDir);
            Files.writeString(secureFile, base64Payload, StandardCharsets.UTF_8);
            System.out.println("[CLIENTE-SECURITY] Archivo criptográfico 'network.enc' generado en el entorno profundo.");

            try {
                Files.delete(rawConfig);
                System.out.println("[CLIENTE-SECURITY] CUMPLIMIENTO: Archivo plano temporal 'server.vaconect' ELIMINADO Y BORRADO de la raíz para evitar fugas.");
            } catch (IOException e) {
                System.err.println("[ALERTA SEGURIDAD] No se pudo ejecutar el borrado físico del plano: " + e.getMessage());
            }
        }
    }

    public String getServerIp() { return serverIp; }
    public int getServerPort() { return serverPort; }
    public String getApiToken() { return serverToken; }
}