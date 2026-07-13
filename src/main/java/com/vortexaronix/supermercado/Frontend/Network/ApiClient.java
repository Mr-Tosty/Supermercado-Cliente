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
package com.vortexaronix.supermercado.Frontend.Network;

import com.vortexaronix.supermercado.Frontend.SecurityConfigLoader;
import com.vortexaronix.supermercado.Inicio.ContextoConfiguracion;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
public class ApiClient {

    private final HttpClient client;

    public ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Construye la URL base de forma dinámica en cada petición.
     * Evita IPs cableadas (hardcoded) y apunta a la interfaz real descubierta en la LAN.
     */
    private String obtenerUrlBase() {
        String ip = ContextoConfiguracion.ip;
        int puerto = ContextoConfiguracion.puerto;

        if (ip == null || puerto <= 0) {
            ip = SecurityConfigLoader.getInstance().getServerIp();
            puerto = SecurityConfigLoader.getInstance().getServerPort();
        }

        if (ip == null) {
            ip = "127.0.0.1";
            puerto = 8080;
        }

        return String.format("http://%s:%d/api/productos", ip, puerto);
    }

    /**
     * Extrae el token descifrado de la memoria RAM de manera dinámica.
     */
    private String obtenerTokenSeguridad() {
        String token = ContextoConfiguracion.token;
        if (token == null) {
            token = SecurityConfigLoader.getInstance().getApiToken();
        }
        return token != null ? token.trim() : "TOKEN-AUSENTE-PROD";
    }

    /**
     * Envía una petición POST blindada al Backend con cabeceras de autorización dinámicas.
     */
    public HttpResponse<String> post(String endpoint, String jsonBody) throws Exception {
        String urlDestino = obtenerUrlBase() + endpoint;
        String tokenActivo = obtenerTokenSeguridad();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlDestino))
                .timeout(Duration.ofSeconds(4))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenActivo) // Acoplamiento estricto con FiltroSeguridadLan
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Envía una petición GET blindada al Backend con cabeceras de autorización dinámicas.
     */
    public HttpResponse<String> get(String endpoint) throws Exception {
        String urlDestino = obtenerUrlBase() + endpoint;
        String tokenActivo = obtenerTokenSeguridad();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlDestino))
                .timeout(Duration.ofSeconds(4))
                .header("Authorization", "Bearer " + tokenActivo) // Acoplamiento estricto con FiltroSeguridadLan
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}