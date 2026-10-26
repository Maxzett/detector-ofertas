package dev.marzetti.pricemonitor.ingestion.vtex;

import dev.marzetti.pricemonitor.catalog.ObservedListing;
import dev.marzetti.pricemonitor.catalog.ObservedOffer;
import dev.marzetti.pricemonitor.catalog.PriceSnapshot;
import dev.marzetti.pricemonitor.catalog.StoreInfo;
import dev.marzetti.pricemonitor.ingestion.normalization.SizeNormalizer;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Convierte un producto de la API de catálogo de VTEX
 * ({@code /api/catalog_system/pub/products/search}) al modelo común {@link ObservedListing}.
 *
 * <p>Estructura relevante del JSON (ver {@code fixtures/vtex/}):</p>
 * <pre>
 * productId, productName, brand, link, categories[]
 * items[]                          ← un item por SKU (talle/color)
 *   itemId, variations ["Color", "Talle"], Talle ["42"], Color ["Negro"]
 *   sellers[].commertialOffer      ← sí, "commertial" con t: así lo escribe VTEX
 *     Price, ListPrice, AvailableQuantity, IsAvailable
 * </pre>
 */
@Component
public class VtexNormalizer {

    public ObservedListing normalize(StoreInfo store, JsonNode product, Instant observedAt) {
        List<ObservedOffer> offers = new ArrayList<>();
        JsonNode items = product.path("items");
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            JsonNode offer = defaultSellerOffer(item);
            if (offer == null || text(item.path("itemId")) == null) {
                continue;
            }
            String sizeRaw = variation(item, "talle");
            int availableQty = offer.path("AvailableQuantity").asInt(0);
            offers.add(new ObservedOffer(
                    text(item.path("itemId")),
                    sizeRaw,
                    SizeNormalizer.toCanonical(sizeRaw, store.sizeScale()),
                    variation(item, "color"),
                    new PriceSnapshot(
                            positiveAmount(offer.path("Price")),
                            positiveAmount(offer.path("ListPrice")),
                            availableQty > 0),
                    availableQty));
        }

        return new ObservedListing(
                store.code(),
                text(product.path("productId")),
                text(product.path("productName")),
                text(product.path("brand")),
                URI.create(text(product.path("link"))),
                strings(product.path("categories")),
                offers,
                observedAt);
    }

    /** Oferta del seller principal ({@code sellerDefault}); si no está marcado, el primero. */
    private static JsonNode defaultSellerOffer(JsonNode item) {
        JsonNode sellers = item.path("sellers");
        if (sellers.size() == 0) {
            return null;
        }
        for (int i = 0; i < sellers.size(); i++) {
            if (sellers.get(i).path("sellerDefault").asBoolean(false)) {
                return sellers.get(i).path("commertialOffer");
            }
        }
        return sellers.get(0).path("commertialOffer");
    }

    /**
     * Valor de una variación del SKU. VTEX lista los nombres en {@code variations} y pone
     * cada valor en una propiedad con ese nombre. Mega Sports usa "Talle"; otras tiendas, "talle".
     */
    private static String variation(JsonNode item, String name) {
        JsonNode variations = item.path("variations");
        for (int i = 0; i < variations.size(); i++) {
            String variationName = text(variations.get(i));
            if (variationName != null && variationName.equalsIgnoreCase(name)) {
                return text(item.path(variationName).path(0));
            }
        }
        return null;
    }

    /**
     * VTEX informa precio 0 para SKUs sin precio cargado. Un precio 0 no es una oferta:
     * se guarda como "sin precio" ({@code null}).
     */
    private static BigDecimal positiveAmount(JsonNode node) {
        if (!node.isNumber()) {
            return null;
        }
        BigDecimal value = node.decimalValue();
        return value.signum() > 0 ? value : null;
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String value = text(array.get(i));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asString();
        return value == null || value.isBlank() ? null : value.trim();
    }
}
