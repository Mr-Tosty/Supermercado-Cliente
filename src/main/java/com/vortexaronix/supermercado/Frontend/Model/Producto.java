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
package com.vortexaronix.supermercado.Frontend.Model;

import java.math.BigDecimal;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
public class Producto {

    private final StringProperty codigoBarras = new SimpleStringProperty();
    private final StringProperty nombre = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> precio = new SimpleObjectProperty<>();
    private final IntegerProperty stockActual = new SimpleIntegerProperty();

    /**
     * Constructor por Defecto.
     * Requerido obligatoriamente por los motores de red para la deserialización
     * de respuestas JSON entrantes desde la API de Spring Boot.
     */
    public Producto() {
    }

    /**
     * Constructor Completo de Inicialización Comercial.
     */
    public Producto(String codigoBarras, String nombre, String descripcion, BigDecimal precio, int stockActual) {
        this.codigoBarras.set(codigoBarras);
        this.nombre.set(nombre);
        this.descripcion.set(descripcion);
        this.precio.set(precio);
        this.stockActual.set(stockActual);
    }

    public StringProperty codigoBarrasProperty() { return codigoBarras; }
    public StringProperty nombreProperty() { return nombre; }
    public StringProperty descripcionProperty() { return descripcion; }
    public ObjectProperty<BigDecimal> precioProperty() { return precio; }
    public IntegerProperty stockActualProperty() { return stockActual; }

    public String getCodigoBarras() { return codigoBarras.get(); }
    public void setCodigoBarras(String value) { this.codigoBarras.set(value); }

    public String getNombre() { return nombre.get(); }
    public void setNombre(String value) { this.nombre.set(value); }

    public String getDescripcion() { return descripcion.get(); }
    public void setDescripcion(String value) { this.descripcion.set(value); }

    public BigDecimal getPrecio() { return precio.get(); }
    public void setPrecio(BigDecimal value) { this.precio.set(value); }

    public int getStockActual() { return stockActual.get(); }
    public void setStockActual(int value) { this.stockActual.set(value); }
}