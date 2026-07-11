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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
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

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String WIN_PATH = "C:\\Users\\Public\\Vortex Aronix\\Data Base\\SuperMarket\\";
    private static final String LINUX_PATH = "/var/tmp/Vortex_Aronix/Data_Base/SuperMarket/";
    private static final String FILE_NAME = "network.enc";
    private static final String RAW_FILE = "server.vaconect";
    
    private static SecurityConfigLoader instance;

    static boolean validarTokenMaestroTI(String token) {
        return "TI-MASTER-VORTEX-2026".equals(token.trim());
    }
    private String serverIp;
    private int serverPort;
    private String apiToken;
    
    public static SecurityConfigLoader getInstance() {
        if (instance == null) instance = new SecurityConfigLoader();
        return instance;
    }

    // Llave estática de 16 bytes para AES-128
    private static final byte[] AES_KEY = {
        0x56, 0x6f, 0x72, 0x74, 0x65, 0x78, 0x41, 0x72,
        0x6f, 0x6e, 0x69, 0x78, 0x4e, 0x65, 0x74, 0x5f
    };

    public void initializeConfiguration() throws Exception {
        Path secureDir = Paths.get(OS.contains("win") ? WIN_PATH : LINUX_PATH);
        Path secureFile = secureDir.resolve(FILE_NAME);

        if (Files.exists(secureFile)) {
            loadEncryptedConfig(secureFile);
        } else {
            Path jarDir = Paths.get(SecurityConfigLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            Path rawConfig = jarDir.resolve(RAW_FILE);

            if (!Files.exists(rawConfig)) {
                throw new FileNotFoundException("No se encontró archivo de configuración inicial: " + RAW_FILE);
            }

            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(rawConfig)) {
                props.load(in);
            }

            this.serverIp = props.getProperty("server.ip");
            this.serverPort = Integer.parseInt(props.getProperty("server.port"));
            this.apiToken = props.getProperty("server.token");

            Files.createDirectories(secureDir);
            saveEncryptedConfig(secureFile);
            applySystemAttributes(secureFile);
            Files.delete(rawConfig); // Autodestrucción
        }
    }

    private void loadEncryptedConfig(Path path) throws Exception {
        byte[] ciphertext = Files.readAllBytes(path);
        byte[] iv = new byte[16];
        byte[] encryptedData = new byte[ciphertext.length - 16];
        System.arraycopy(ciphertext, 0, iv, 0, 16);
        System.arraycopy(ciphertext, 16, encryptedData, 0, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(encryptedData);
        String dataStr = new String(decrypted, StandardCharsets.UTF_8);
        String[] tokens = dataStr.split(";");

        this.serverIp = tokens[0];
        this.serverPort = Integer.parseInt(tokens[1]);
        this.apiToken = tokens[2];
    }

    private void saveEncryptedConfig(Path path) throws Exception {
        String dataStr = this.serverIp + ";" + this.serverPort + ";" + this.apiToken;
        byte[] plainBytes = dataStr.getBytes(StandardCharsets.UTF_8);

        byte[] iv = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encryptedData = cipher.doFinal(plainBytes);
        byte[] finalPayload = new byte[16 + encryptedData.length];
        System.arraycopy(iv, 0, finalPayload, 0, 16);
        System.arraycopy(encryptedData, 0, finalPayload, 16, encryptedData.length);

        Files.write(path, finalPayload, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void applySystemAttributes(Path path) {
        if (OS.contains("win")) {
            try {
                // Aplica atributos de oculto y sistema en Windows
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "attrib +h +s \"" + path.toAbsolutePath() + "\"");
                pb.start().waitFor();
            } catch (Exception ignored) {
                // Log de seguridad silencioso
            }
        }
    }

    public String getServerIp() { return serverIp; }
    public int getServerPort() { return serverPort; }
    public String getApiToken() { return apiToken; }
}