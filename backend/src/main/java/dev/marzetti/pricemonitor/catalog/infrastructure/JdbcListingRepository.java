package dev.marzetti.pricemonitor.catalog.infrastructure;

import dev.marzetti.pricemonitor.catalog.ObservedListing;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcListingRepository {

    private final JdbcClient jdbc;

    public JdbcListingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Inserta el listing o actualiza sus datos descriptivos si ya existe.
     * Un único statement con ON CONFLICT: no hay ventana entre "buscar" e "insertar".
     *
     * @return id del listing
     */
    public long upsert(long storeId, Long categoryId, ObservedListing observed) {
        return jdbc.sql("""
                        INSERT INTO listing (store_id, external_id, category_id, title, brand, url,
                                             first_seen_at, last_seen_at)
                        VALUES (:storeId, :externalId, :categoryId, :title, :brand, :url, :seenAt, :seenAt)
                        ON CONFLICT (store_id, external_id) DO UPDATE SET
                            title        = EXCLUDED.title,
                            brand        = EXCLUDED.brand,
                            url          = EXCLUDED.url,
                            category_id  = COALESCE(EXCLUDED.category_id, listing.category_id),
                            last_seen_at = EXCLUDED.last_seen_at
                        RETURNING id
                        """)
                .param("storeId", storeId)
                .param("externalId", observed.externalId())
                .param("categoryId", categoryId, Types.BIGINT)
                .param("title", observed.title())
                .param("brand", observed.brand(), Types.VARCHAR)
                .param("url", observed.url().toString())
                .param("seenAt", OffsetDateTime.ofInstant(observed.observedAt(), ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }
}
