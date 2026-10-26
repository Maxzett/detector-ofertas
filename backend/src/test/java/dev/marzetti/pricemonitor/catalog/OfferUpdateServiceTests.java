package dev.marzetti.pricemonitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.marzetti.pricemonitor.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Criterio de cierre de la Etapa 2: la regla del historial, contra PostgreSQL real.
 * Usa la tienda "megasports" del seed (V2), que tiene mapeada la categoría /Calzado/Botines/.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
class OfferUpdateServiceTests {

    private static final Instant FIRST_CHECK = Instant.parse("2026-10-26T10:00:00Z");
    private static final Instant SECOND_CHECK = Instant.parse("2026-10-26T11:00:00Z");

    @Autowired
    private OfferUpdateService service;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void cleanCatalog() {
        jdbc.sql("TRUNCATE price_history, alert_offer_state, notification, offer, listing RESTART IDENTITY CASCADE")
                .update();
    }

    private static ObservedOffer offer(String sku, String size, String price, boolean inStock) {
        return new ObservedOffer(sku, size, size, "Negro",
                new PriceSnapshot(new BigDecimal(price), new BigDecimal("62999"), inStock), inStock ? 10 : 0);
    }

    private static ObservedListing listing(Instant observedAt, ObservedOffer... offers) {
        return new ObservedListing("megasports", "1211155", "KAPPA BOTINES - TERMOLI FG BLCK", "Kappa",
                URI.create("https://www.megasports.com.ar/kappa-botines---termoli-fg-blck/p"),
                List.of("/Calzado/Botines/", "/Calzado/"), List.of(offers), observedAt);
    }

    private int historyRows() {
        return jdbc.sql("SELECT count(*) FROM price_history").query(Integer.class).single();
    }

    private int historyRowsOf(String sku) {
        return jdbc.sql("""
                        SELECT count(*) FROM price_history h JOIN offer o ON o.id = h.offer_id
                        WHERE o.external_sku_id = :sku
                        """)
                .param("sku", sku)
                .query(Integer.class)
                .single();
    }

    @Test
    void firstObservationCreatesOffersWithTheirInitialHistory() {
        ListingUpdateResult result = service.apply(listing(FIRST_CHECK,
                offer("sku-41", "41", "44499", true),
                offer("sku-42", "42", "44499", true)));

        assertThat(result.offersSeen()).isEqualTo(2);
        assertThat(result.offersCreated()).isEqualTo(2);
        assertThat(result.offersChanged()).isZero();
        assertThat(historyRows()).isEqualTo(2);
        assertThat(events.stream(OfferChanged.class)).hasSize(2).allMatch(OfferChanged::isNewOffer);
    }

    @Test
    void listingIsLinkedToTheMappedCategory() {
        service.apply(listing(FIRST_CHECK, offer("sku-42", "42", "44499", true)));

        String category = jdbc.sql("""
                        SELECT c.code FROM listing l JOIN category c ON c.id = l.category_id
                        WHERE l.external_id = '1211155'
                        """)
                .query(String.class)
                .single();
        assertThat(category).isEqualTo("botines-futbol");
    }

    @Test
    void samePricesDoNotWriteHistory() {
        service.apply(listing(FIRST_CHECK, offer("sku-42", "42", "44499", true)));
        events.clear();

        // Mismo precio con otra escala decimal: tiene que considerarse igual
        ListingUpdateResult result = service.apply(listing(SECOND_CHECK, offer("sku-42", "42", "44499.00", true)));

        assertThat(result.offersChanged()).isZero();
        assertThat(historyRowsOf("sku-42")).isEqualTo(1);
        assertThat(events.stream(OfferChanged.class)).isEmpty();
    }

    @Test
    void uncheckedAndUnchangedAreDistinguishable() {
        service.apply(listing(FIRST_CHECK, offer("sku-42", "42", "44499", true)));
        service.apply(listing(SECOND_CHECK, offer("sku-42", "42", "44499", true)));

        record Timestamps(OffsetDateTime lastCheckedAt, OffsetDateTime lastChangedAt) {
        }
        Timestamps timestamps = jdbc.sql("SELECT last_checked_at, last_changed_at FROM offer WHERE external_sku_id = 'sku-42'")
                .query((rs, rowNum) -> new Timestamps(
                        rs.getObject("last_checked_at", OffsetDateTime.class),
                        rs.getObject("last_changed_at", OffsetDateTime.class)))
                .single();

        assertThat(timestamps.lastCheckedAt().toInstant()).isEqualTo(SECOND_CHECK);
        assertThat(timestamps.lastChangedAt().toInstant()).isEqualTo(FIRST_CHECK);
    }

    @Test
    void priceDropWritesExactlyOneHistoryRow() {
        service.apply(listing(FIRST_CHECK,
                offer("sku-41", "41", "44499", true),
                offer("sku-42", "42", "44499", true)));
        events.clear();

        ListingUpdateResult result = service.apply(listing(SECOND_CHECK,
                offer("sku-41", "41", "44499", true),
                offer("sku-42", "42", "39999", true)));

        assertThat(result.offersChanged()).isEqualTo(1);
        assertThat(historyRowsOf("sku-41")).isEqualTo(1);
        assertThat(historyRowsOf("sku-42")).isEqualTo(2);

        BigDecimal currentPrice = jdbc.sql("SELECT current_price FROM offer WHERE external_sku_id = 'sku-42'")
                .query(BigDecimal.class)
                .single();
        assertThat(currentPrice).isEqualByComparingTo("39999");

        assertThat(events.stream(OfferChanged.class)).singleElement().satisfies(event -> {
            assertThat(event.isNewOffer()).isFalse();
            assertThat(event.previous().price()).isEqualByComparingTo("44499");
            assertThat(event.current().price()).isEqualByComparingTo("39999");
            assertThat(event.observedAt()).isEqualTo(SECOND_CHECK);
        });
    }

    @Test
    void runningOutOfStockIsAChange() {
        service.apply(listing(FIRST_CHECK, offer("sku-42", "42", "44499", true)));

        ListingUpdateResult result = service.apply(listing(SECOND_CHECK, offer("sku-42", "42", "44499", false)));

        assertThat(result.offersChanged()).isEqualTo(1);
        assertThat(historyRowsOf("sku-42")).isEqualTo(2);
    }

    @Test
    void unknownStoreIsRejected() {
        ObservedListing fromUnknownStore = new ObservedListing("no-existe", "1", "X", null,
                URI.create("https://example.com/x/p"), List.of(), List.of(), FIRST_CHECK);

        assertThatThrownBy(() -> service.apply(fromUnknownStore)).isInstanceOf(IllegalArgumentException.class);
    }
}
