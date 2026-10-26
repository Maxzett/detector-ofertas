/**
 * Módulo <b>ingestion</b>: obtención y normalización de datos de las tiendas.
 *
 * <p>Es el único módulo que habla con sistemas externos de tiendas, a través del port
 * {@code port.StoreConnector}. Hay una implementación por plataforma (ADR 0002): hoy {@code vtex};
 * después SFCC, Magento (HTML), Sport 78 (HTML) y JSON-LD.</p>
 *
 * <p><b>API pública:</b> los tipos de este paquete raíz ({@link dev.marzetti.pricemonitor.ingestion.ListingImporter}
 * y sus excepciones). Los subpaquetes ({@code port}, {@code vtex}, {@code http}, {@code normalization}) son internos.</p>
 */
package dev.marzetti.pricemonitor.ingestion;
