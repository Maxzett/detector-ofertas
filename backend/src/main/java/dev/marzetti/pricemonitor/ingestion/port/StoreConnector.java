package dev.marzetti.pricemonitor.ingestion.port;

import dev.marzetti.pricemonitor.catalog.ObservedListing;
import dev.marzetti.pricemonitor.catalog.StoreInfo;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

/**
 * Port hacia una plataforma de e-commerce. Cada implementación cubre una plataforma
 * (VTEX, SFCC, ...) y sirve para todas las tiendas que la usan: la tienda es configuración.
 *
 * <p>Las implementaciones pueden lanzar {@link dev.marzetti.pricemonitor.ingestion.SourceUnavailableException}
 * cuando la tienda no responde o responde con error.</p>
 */
public interface StoreConnector {

    /** Valor de {@code store.connector_type} que atiende este connector. */
    String connectorType();

    /** Lo que esta plataforma permite obtener. El resto del sistema se adapta a esto. */
    Set<ConnectorCapability> capabilities();

    /**
     * Trae un producto a partir de su URL pública.
     *
     * @return vacío si la tienda no tiene ese producto
     */
    Optional<ObservedListing> fetchByUrl(StoreInfo store, URI productUrl);
}
