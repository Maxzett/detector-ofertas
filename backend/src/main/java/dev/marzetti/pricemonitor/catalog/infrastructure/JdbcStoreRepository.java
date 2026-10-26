package dev.marzetti.pricemonitor.catalog.infrastructure;

import dev.marzetti.pricemonitor.catalog.StoreInfo;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStoreRepository {

    // Los valores de config (JSONB) se extraen en SQL para no acoplar el resto del código al JSON.
    private static final String SELECT_STORE = """
            SELECT id, code, name, base_url, connector_type, enabled,
                   config ->> 'sizeScale'                                  AS size_scale,
                   COALESCE((config ->> 'requestDelayMs')::bigint, 3000)   AS request_delay_ms,
                   EXTRACT(EPOCH FROM min_poll_interval)::bigint           AS min_poll_seconds
            FROM store
            """;

    private final JdbcClient jdbc;

    public JdbcStoreRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StoreInfo> findByCode(String code) {
        return jdbc.sql(SELECT_STORE + " WHERE code = :code")
                .param("code", code)
                .query(JdbcStoreRepository::mapStore)
                .optional();
    }

    public List<StoreInfo> findAll() {
        return jdbc.sql(SELECT_STORE + " ORDER BY code")
                .query(JdbcStoreRepository::mapStore)
                .list();
    }

    /**
     * Primera categoría propia que coincide con alguna categoría de la tienda.
     * Las categorías vienen de {@code store_category.external_ref} (seed V2).
     */
    public Optional<Long> findCategoryId(long storeId, Collection<String> externalRefs) {
        if (externalRefs.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.sql("""
                        SELECT category_id FROM store_category
                        WHERE store_id = :storeId AND enabled AND external_ref IN (:refs)
                        ORDER BY id
                        LIMIT 1
                        """)
                .param("storeId", storeId)
                .param("refs", externalRefs)
                .query(Long.class)
                .optional();
    }

    private static StoreInfo mapStore(ResultSet rs, int rowNum) throws SQLException {
        return new StoreInfo(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                URI.create(rs.getString("base_url")),
                rs.getString("connector_type"),
                rs.getBoolean("enabled"),
                rs.getString("size_scale"),
                Duration.ofMillis(rs.getLong("request_delay_ms")),
                Duration.ofSeconds(rs.getLong("min_poll_seconds")));
    }
}
