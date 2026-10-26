package dev.marzetti.pricemonitor.catalog;

import dev.marzetti.pricemonitor.catalog.infrastructure.JdbcListingRepository;
import dev.marzetti.pricemonitor.catalog.infrastructure.JdbcOfferRepository;
import dev.marzetti.pricemonitor.catalog.infrastructure.JdbcOfferRepository.CurrentOffer;
import dev.marzetti.pricemonitor.catalog.infrastructure.JdbcStoreRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica una observación de la tienda al catálogo. Implementa la regla del historial (ADR 0003):
 *
 * <ol>
 *   <li>Offer nueva: se crea, se escribe su primera fila de historial y se publica {@link OfferChanged}.</li>
 *   <li>Offer existente con cambios de precio, precio de lista o stock: se actualiza, se agrega
 *       una fila de historial y se publica {@link OfferChanged}.</li>
 *   <li>Offer existente sin cambios: solo se actualizan {@code last_checked_at} y {@code last_seen_at}.
 *       No se escribe historial.</li>
 * </ol>
 *
 * Todo ocurre en una transacción: o se aplica la observación completa, o nada.
 */
@Service
public class OfferUpdateService {

    private final JdbcStoreRepository stores;
    private final JdbcListingRepository listings;
    private final JdbcOfferRepository offers;
    private final ApplicationEventPublisher events;

    public OfferUpdateService(JdbcStoreRepository stores, JdbcListingRepository listings,
                              JdbcOfferRepository offers, ApplicationEventPublisher events) {
        this.stores = stores;
        this.listings = listings;
        this.offers = offers;
        this.events = events;
    }

    @Transactional
    public ListingUpdateResult apply(ObservedListing observed) {
        StoreInfo store = stores.findByCode(observed.storeCode())
                .orElseThrow(() -> new IllegalArgumentException("Tienda desconocida: " + observed.storeCode()));

        Long categoryId = stores.findCategoryId(store.id(), observed.categoryPaths()).orElse(null);
        long listingId = listings.upsert(store.id(), categoryId, observed);

        Instant at = observed.observedAt();
        Instant nextCheckAt = at.plus(store.minPollInterval());
        int created = 0;
        int changed = 0;

        for (ObservedOffer offer : observed.offers()) {
            Optional<CurrentOffer> existing = offers.findForUpdate(store.id(), offer.externalSkuId());

            if (existing.isEmpty()) {
                long offerId = offers.insert(listingId, store.id(), offer, at, nextCheckAt);
                offers.appendHistory(offerId, at, offer.snapshot());
                events.publishEvent(new OfferChanged(offerId, listingId, store.code(), null, offer.snapshot(), at));
                created++;
                continue;
            }

            CurrentOffer current = existing.get();
            boolean isChange = offer.snapshot().differsFrom(current.snapshot());
            Instant lastChangedAt = isChange ? at : current.lastChangedAt();
            offers.update(current.id(), listingId, offer, at, nextCheckAt, lastChangedAt);

            if (isChange) {
                offers.appendHistory(current.id(), at, offer.snapshot());
                events.publishEvent(new OfferChanged(
                        current.id(), listingId, store.code(), current.snapshot(), offer.snapshot(), at));
                changed++;
            }
        }

        return new ListingUpdateResult(listingId, observed.offers().size(), created, changed);
    }
}
