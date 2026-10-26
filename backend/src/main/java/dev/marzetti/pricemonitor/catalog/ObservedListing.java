package dev.marzetti.pricemonitor.catalog;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Una publicación de producto tal como se observó en una tienda, con todas sus variantes.
 * Es el contrato entre {@code ingestion} (que la produce) y {@code catalog} (que la persiste):
 * cada connector, sea cual sea su fuente, termina entregando este mismo tipo.
 *
 * @param storeCode      código de la tienda (tabla {@code store})
 * @param externalId     id del producto en la tienda
 * @param title          nombre del producto
 * @param brand          marca, si la tienda la informa
 * @param url            URL pública del producto
 * @param categoryPaths  categorías de la tienda; se cruzan con {@code store_category}
 * @param offers         variantes observadas
 * @param observedAt     momento de la consulta
 */
public record ObservedListing(
        String storeCode,
        String externalId,
        String title,
        String brand,
        URI url,
        List<String> categoryPaths,
        List<ObservedOffer> offers,
        Instant observedAt) {

    public ObservedListing {
        Objects.requireNonNull(storeCode, "storeCode");
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Falta el id externo del producto");
        }
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(observedAt, "observedAt");
        categoryPaths = categoryPaths == null ? List.of() : List.copyOf(categoryPaths);
        offers = offers == null ? List.of() : List.copyOf(offers);
    }
}
