package dev.marzetti.pricemonitor.catalog;

import dev.marzetti.pricemonitor.catalog.infrastructure.JdbcStoreRepository;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Consulta de tiendas configuradas. Las tiendas se cargan por migración, no por API. */
@Service
public class StoreDirectory {

    private final JdbcStoreRepository stores;

    public StoreDirectory(JdbcStoreRepository stores) {
        this.stores = stores;
    }

    public Optional<StoreInfo> findByCode(String code) {
        return stores.findByCode(code);
    }

    public List<StoreInfo> findAll() {
        return stores.findAll();
    }

    /**
     * Tienda a la que pertenece una URL de producto. Compara host (sin "www.") y puerto,
     * así {@code https://megasports.com.ar/x/p} y {@code https://www.megasports.com.ar/x/p}
     * resuelven a la misma tienda.
     */
    public Optional<StoreInfo> findByUrl(URI url) {
        if (url == null || url.getHost() == null) {
            return Optional.empty();
        }
        String host = normalizeHost(url.getHost());
        int port = effectivePort(url);
        return stores.findAll().stream()
                .filter(store -> store.baseUrl().getHost() != null)
                .filter(store -> normalizeHost(store.baseUrl().getHost()).equals(host))
                .filter(store -> effectivePort(store.baseUrl()) == port)
                .findFirst();
    }

    private static String normalizeHost(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.startsWith("www.") ? lower.substring(4) : lower;
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
    }
}
