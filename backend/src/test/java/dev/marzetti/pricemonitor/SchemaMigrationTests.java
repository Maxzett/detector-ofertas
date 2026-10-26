package dev.marzetti.pricemonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Corre las migraciones de Flyway contra un PostgreSQL real y verifica el schema
 * y el seed. Si una migración tiene un error de SQL, el contexto no levanta.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaMigrationTests {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void appliesAllMigrations() {
        Integer applied = jdbc.sql("SELECT count(*) FROM flyway_schema_history WHERE success")
                .query(Integer.class)
                .single();

        assertThat(applied).isEqualTo(2);
    }

    @Test
    void enablesOnlyTheFirstMvpStores() {
        List<String> enabled = jdbc.sql("SELECT code FROM store WHERE enabled ORDER BY code")
                .query(String.class)
                .list();

        assertThat(enabled).containsExactly("digitalsport", "megasports");
    }

    @Test
    void rejectsUnknownConnectorType() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO store (code, name, base_url, connector_type)
                        VALUES ('test', 'Test', 'https://example.com', 'SELENIUM')
                        """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void schedulerIndexOnlyCoversTrackedOffers() {
        String definition = jdbc.sql("SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_offer_due'")
                .query(String.class)
                .single();

        assertThat(definition).contains("WHERE", "TRACKED");
    }
}
