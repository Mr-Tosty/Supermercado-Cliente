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
 * @since   11 jul 2026
 * ----------------------------------------------------------------------------
 */
public class ProductoFX {
    private final String codigoBarras;
    private final String nombre;
    private final String description;
    private final double precio;
    private final int stockActual;
    
    public ProductoFX(String codigoBarras, String nombre, String description, double precio, int stockActual){
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.description = description;
        this.precio = precio;
        this.stockActual = stockActual;
    }
    
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return description; }
    public double getPrecio() { return precio; }
    public int getStockActual() { return stockActual; }
    
}