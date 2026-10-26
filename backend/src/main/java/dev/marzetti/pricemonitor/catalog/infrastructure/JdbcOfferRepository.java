package dev.marzetti.pricemonitor.catalog.infrastructure;

import dev.marzetti.pricemonitor.catalog.ObservedOffer;
import dev.marzetti.pricemonitor.catalog.PriceSnapshot;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOfferRepository {

    /** Estado actual de una offer, lo necesario para decidir si cambió. */
    public record CurrentOffer(long id, PriceSnapshot snapshot, Instant lastChangedAt) {
    }

    private final JdbcClient jdbc;

    public JdbcOfferRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lee el estado actual y bloquea la fila hasta el fin de la transacción ({@code FOR UPDATE}),
     * para que dos procesos no evalúen el mismo cambio a la vez.
     */
    public Optional<CurrentOffer> findForUpdate(long storeId, String externalSkuId) {
        return jdbc.sql("""
                        SELECT id, current_price, list_price, in_stock, last_changed_at
                        FROM offer
                        WHERE store_id = :storeId AND external_sku_id = :sku
                        FOR UPDATE
                        """)
                .param("storeId", storeId)
                .param("sku", externalSkuId)
                .query((rs, rowNum) -> {
                    OffsetDateTime lastChanged = rs.getObject("last_changed_at", OffsetDateTime.class);
                    return new CurrentOffer(
                            rs.getLong("id"),
                            new PriceSnapshot(
                                    rs.getBigDecimal("current_price"),
                                    rs.getBigDecimal("list_price"),
                                    rs.getBoolean("in_stock")),
                            lastChanged == null ? null : lastChanged.toInstant());
                })
                .optional();
    }

    /** @return id de la offer creada */
    public long insert(long listingId, long storeId, ObservedOffer offer, Instant observedAt, Instant nextCheckAt) {
        OffsetDateTime at = utc(observedAt);
        return jdbc.sql("""
                        INSERT INTO offer (listing_id, store_id, external_sku_id, size_raw, size_canonical, color,
                                           current_price, list_price, in_stock, available_qty,
                                           next_check_at, last_checked_at, last_seen_at, last_changed_at)
                        VALUES (:listingId, :storeId, :sku, :sizeRaw, :sizeCanonical, :color,
                                :price, :listPrice, :inStock, :qty,
                                :nextCheckAt, :at, :at, :at)
                        RETURNING id
                        """)
                .param("listingId", listingId)
                .param("storeId", storeId)
                .param("sku", offer.externalSkuId())
                .param("sizeRaw", offer.sizeRaw(), Types.VARCHAR)
                .param("sizeCanonical", offer.sizeCanonical(), Types.VARCHAR)
                .param("color", offer.color(), Types.VARCHAR)
                .param("price", offer.snapshot().price(), Types.NUMERIC)
                .param("listPrice", offer.snapshot().listPrice(), Types.NUMERIC)
                .param("inStock", offer.snapshot().inStock())
                .param("qty", offer.availableQty(), Types.INTEGER)
                .param("nextCheckAt", utc(nextCheckAt))
                .param("at", at)
                .query(Long.class)
                .single();
    }

    /**
     * Actualiza el estado actual. Se llama siempre, haya cambio o no: {@code last_checked_at}
     * y {@code last_seen_at} registran que la offer se consultó y seguía publicada.
     */
    public void update(long offerId, long listingId, ObservedOffer offer, Instant observedAt,
                       Instant nextCheckAt, Instant lastChangedAt) {
        OffsetDateTime at = utc(observedAt);
        jdbc.sql("""
                        UPDATE offer SET
                            listing_id      = :listingId,
                            size_raw        = :sizeRaw,
                            size_canonical  = :sizeCanonical,
                            color           = :color,
                            current_price   = :price,
                            list_price      = :listPrice,
                            in_stock        = :inStock,
                            available_qty   = :qty,
                            next_check_at   = :nextCheckAt,
                            last_checked_at = :at,
                            last_seen_at    = :at,
                            last_changed_at = :lastChangedAt
                        WHERE id = :id
                        """)
                .param("id", offerId)
                .param("listingId", listingId)
                .param("sizeRaw", offer.sizeRaw(), Types.VARCHAR)
                .param("sizeCanonical", offer.sizeCanonical(), Types.VARCHAR)
                .param("color", offer.color(), Types.VARCHAR)
                .param("price", offer.snapshot().price(), Types.NUMERIC)
                .param("listPrice", offer.snapshot().listPrice(), Types.NUMERIC)
                .param("inStock", offer.snapshot().inStock())
                .param("qty", offer.availableQty(), Types.INTEGER)
                .param("nextCheckAt", utc(nextCheckAt))
                .param("at", at)
                .param("lastChangedAt", lastChangedAt == null ? null : utc(lastChangedAt),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
    }

    public void appendHistory(long offerId, Instant validFrom, PriceSnapshot snapshot) {
        jdbc.sql("""
                        INSERT INTO price_history (offer_id, valid_from, price, list_price, in_stock)
                        VALUES (:offerId, :validFrom, :price, :listPrice, :inStock)
                        """)
                .param("offerId", offerId)
                .param("validFrom", utc(validFrom))
                .param("price", snapshot.price(), Types.NUMERIC)
                .param("listPrice", snapshot.listPrice(), Types.NUMERIC)
                .param("inStock", snapshot.inStock())
                .update();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
