package dev.marzetti.pricemonitor.ingestion;

import dev.marzetti.pricemonitor.catalog.ListingUpdateResult;
import dev.marzetti.pricemonitor.catalog.ObservedListing;
import dev.marzetti.pricemonitor.catalog.OfferUpdateService;
import dev.marzetti.pricemonitor.catalog.StoreDirectory;
import dev.marzetti.pricemonitor.catalog.StoreInfo;
import dev.marzetti.pricemonitor.ingestion.port.StoreConnector;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Importa un producto a partir de su URL pública: resuelve la tienda, elige el connector
 * de su plataforma, obtiene los datos y los aplica al catálogo.
 */
@Service
public class ListingImporter {

    private static final Logger log = LoggerFactory.getLogger(ListingImporter.class);

    private final StoreDirectory stores;
    private final Map<String, StoreConnector> connectorsByType;
    private final OfferUpdateService offerUpdates;

    public ListingImporter(StoreDirectory stores, List<StoreConnector> connectors, OfferUpdateService offerUpdates) {
        this.stores = stores;
        this.connectorsByType = connectors.stream()
                .collect(Collectors.toUnmodifiableMap(StoreConnector::connectorType, Function.identity()));
        this.offerUpdates = offerUpdates;
    }

    public ImportResult importByUrl(URI productUrl) {
        requireHttpUrl(productUrl);

        StoreInfo store = stores.findByUrl(productUrl)
                .orElseThrow(() -> new UnsupportedStoreException(
                        "No hay ninguna tienda configurada para " + productUrl.getHost()));

        StoreConnector connector = connectorsByType.get(store.connectorType());
        if (connector == null) {
            throw new UnsupportedStoreException("%s usa la plataforma %s, que todavía no tiene connector"
                    .formatted(store.name(), store.connectorType()));
        }

        ObservedListing listing = connector.fetchByUrl(store, productUrl)
                .orElseThrow(() -> new ListingNotFoundException(productUrl));
        ListingUpdateResult result = offerUpdates.apply(listing);

        log.info("Importado store={} externalId={} offers={} nuevas={} con_cambios={}",
                store.code(), listing.externalId(), result.offersSeen(), result.offersCreated(), result.offersChanged());

        return new ImportResult(store.code(), listing.externalId(), listing.title(),
                result.offersSeen(), result.offersCreated(), result.offersChanged());
    }

    private static void requireHttpUrl(URI url) {
        if (url == null || url.getHost() == null
                || !("http".equalsIgnoreCase(url.getScheme()) || "https".equalsIgnoreCase(url.getScheme()))) {
            throw new IllegalArgumentException("Se espera una URL http(s) absoluta: " + url);
        }
    }
}
