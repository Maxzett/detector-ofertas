package dev.marzetti.pricemonitor.catalog;

/**
 * Resumen de aplicar un {@link ObservedListing}.
 *
 * @param offersSeen     variantes que vinieron en la observación
 * @param offersCreated  variantes nuevas (primera vez que se ven)
 * @param offersChanged  variantes existentes cuyo precio o stock cambió
 */
public record ListingUpdateResult(long listingId, int offersSeen, int offersCreated, int offersChanged) {
}
