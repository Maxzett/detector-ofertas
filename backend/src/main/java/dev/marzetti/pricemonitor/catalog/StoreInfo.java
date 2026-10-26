package dev.marzetti.pricemonitor.catalog;

import java.net.URI;
import java.time.Duration;

/**
 * Configuración de una tienda, de solo lectura para los demás módulos.
 *
 * @param connectorType   plataforma de la tienda: VTEX, SFCC, MAGENTO_HTML, SPORT78_HTML o JSONLD
 * @param sizeScale       escala de talles que usa la tienda (AR, US, EU)
 * @param requestDelay    pausa mínima entre requests a esta tienda (ADR 0004)
 * @param minPollInterval frecuencia máxima de consulta de una misma offer
 */
public record StoreInfo(
        long id,
        String code,
        String name,
        URI baseUrl,
        String connectorType,
        boolean enabled,
        String sizeScale,
        Duration requestDelay,
        Duration minPollInterval) {
}
