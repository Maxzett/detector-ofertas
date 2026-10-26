package dev.marzetti.pricemonitor.catalog;

import java.util.Objects;

/**
 * Una variante (SKU) tal como se observó en la tienda, ya normalizada.
 *
 * @param externalSkuId  id del SKU en la tienda; junto con la tienda identifica la offer
 * @param sizeRaw        talle como lo muestra la tienda (ej. "42", "UK: 8.0 US: 8.5 AR: 40")
 * @param sizeCanonical  talle en la escala canónica (AR); {@code null} si no se pudo convertir
 * @param color          color, si la tienda lo informa
 * @param snapshot       precio, precio de lista y stock
 * @param availableQty   unidades disponibles, si la tienda lo informa
 */
public record ObservedOffer(
        String externalSkuId,
        String sizeRaw,
        String sizeCanonical,
        String color,
        PriceSnapshot snapshot,
        Integer availableQty) {

    public ObservedOffer {
        if (externalSkuId == null || externalSkuId.isBlank()) {
            throw new IllegalArgumentException("Falta el id externo del SKU");
        }
        Objects.requireNonNull(snapshot, "snapshot");
    }
}
