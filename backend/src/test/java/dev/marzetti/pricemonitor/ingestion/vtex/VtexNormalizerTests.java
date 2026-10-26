package dev.marzetti.pricemonitor.ingestion.vtex;

import static org.assertj.core.api.Assertions.assertThat;

import dev.marzetti.pricemonitor.catalog.ObservedListing;
import dev.marzetti.pricemonitor.catalog.ObservedOffer;
import dev.marzetti.pricemonitor.catalog.StoreInfo;
import dev.marzetti.pricemonitor.ingestion.Fixtures;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class VtexNormalizerTests {

    private static final Instant OBSERVED_AT = Instant.parse("2026-10-26T20:00:00Z");

    private final JsonMapper json = JsonMapper.builder().build();
    private final VtexNormalizer normalizer = new VtexNormalizer();
    private final StoreInfo megasports = Fixtures.vtexStore("megasports", "https://www.megasports.com.ar");

    private ObservedListing normalizeFixture() {
        JsonNode product = json.readTree(Fixtures.read(Fixtures.MEGASPORTS_TERMOLI)).get(0);
        return normalizer.normalize(megasports, product, OBSERVED_AT);
    }

    @Test
    void mapsProductData() {
        ObservedListing listing = normalizeFixture();

        assertThat(listing.storeCode()).isEqualTo("megasports");
        assertThat(listing.externalId()).isEqualTo("1211155");
        assertThat(listing.title()).isEqualTo("KAPPA BOTINES - TERMOLI FG BLCK");
        assertThat(listing.brand()).isEqualTo("Kappa");
        assertThat(listing.url()).isEqualTo(URI.create("https://www.megasports.com.ar/kappa-botines---termoli-fg-blck/p"));
        assertThat(listing.categoryPaths()).containsExactly("/Calzado/Botines/", "/Calzado/");
        assertThat(listing.observedAt()).isEqualTo(OBSERVED_AT);
    }

    @Test
    void createsOneOfferPerSkuWithSizeAndPrices() {
        ObservedListing listing = normalizeFixture();

        assertThat(listing.offers()).hasSize(7);
        ObservedOffer size42 = listing.offers().stream()
                .filter(offer -> "42".equals(offer.sizeCanonical()))
                .findFirst()
                .orElseThrow();
        assertThat(size42.externalSkuId()).isEqualTo("1199914807");
        assertThat(size42.sizeRaw()).isEqualTo("42");
        assertThat(size42.color()).isEqualTo("Negro");
        assertThat(size42.snapshot().price()).isEqualByComparingTo(new BigDecimal("44499"));
        assertThat(size42.snapshot().listPrice()).isEqualByComparingTo(new BigDecimal("62999"));
        assertThat(size42.snapshot().inStock()).isTrue();
        assertThat(size42.availableQty()).isEqualTo(100);
    }

    @Test
    void skuWithoutUnitsIsOutOfStock() {
        ObservedListing listing = normalizeFixture();

        assertThat(listing.offers())
                .filteredOn(offer -> !offer.snapshot().inStock())
                .extracting(ObservedOffer::sizeCanonical)
                .containsExactlyInAnyOrder("39", "40");
    }

    @Test
    void zeroPriceMeansNoPrice() {
        String withoutPrice = Fixtures.read(Fixtures.MEGASPORTS_TERMOLI)
                .replace("\"Price\": 44499", "\"Price\": 0");
        JsonNode product = json.readTree(withoutPrice).get(0);

        ObservedListing listing = normalizer.normalize(megasports, product, OBSERVED_AT);

        assertThat(listing.offers()).allSatisfy(offer -> assertThat(offer.snapshot().price()).isNull());
    }

    @Test
    void sizesFromNonArgentineScaleAreNotCanonicalized() {
        StoreInfo usStore = new StoreInfo(2L, "us", "us", URI.create("https://us.example"), "VTEX", true, "US",
                megasports.requestDelay(), megasports.minPollInterval());
        JsonNode product = json.readTree(Fixtures.read(Fixtures.MEGASPORTS_TERMOLI)).get(0);

        ObservedListing listing = normalizer.normalize(usStore, product, OBSERVED_AT);

        assertThat(listing.offers()).allSatisfy(offer -> {
            assertThat(offer.sizeRaw()).isNotNull();
            assertThat(offer.sizeCanonical()).isNull();
        });
    }
}
