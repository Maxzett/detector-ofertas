package dev.marzetti.pricemonitor.catalog;

import java.time.Instant;

/**
 * Evento de dominio: cambió el precio, el precio de lista o el stock de una offer,
 * o apareció una offer nueva. Lo publica {@link OfferUpdateService} dentro de la misma
 * transacción que escribe el historial. En la Etapa 5 lo escucha el módulo {@code alerts}.
 *
 * @param previous estado anterior; {@code null} si la offer es nueva
 */
public record OfferChanged(
        long offerId,
        long listingId,
        String storeCode,
        PriceSnapshot previous,
        PriceSnapshot current,
        Instant observedAt) {

    public boolean isNewOffer() {
        return previous == null;
    }
}
