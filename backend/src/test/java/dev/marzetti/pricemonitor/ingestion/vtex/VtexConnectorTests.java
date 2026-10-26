package dev.marzetti.pricemonitor.ingestion.vtex;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.marzetti.pricemonitor.catalog.ObservedListing;
import dev.marzetti.pricemonitor.catalog.StoreInfo;
import dev.marzetti.pricemonitor.ingestion.Fixtures;
import dev.marzetti.pricemonitor.ingestion.SourceUnavailableException;
import dev.marzetti.pricemonitor.ingestion.http.StoreHttpConfiguration;
import dev.marzetti.pricemonitor.ingestion.http.StoreHttpProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba el connector contra un servidor HTTP falso (WireMock) que responde como Mega Sports.
 * Verifica lo que el test del normalizer no cubre: la URL que se arma, los headers y el manejo
 * de errores HTTP.
 */
class VtexConnectorTests {

    private static final String API_PATH = "/api/catalog_system/pub/products/search/kappa-botines---termoli-fg-blck/p";
    private static final String USER_AGENT = "DetectorOfertas/test";
    private static final Instant NOW = Instant.parse("2026-10-26T20:00:00Z");

    private static WireMockServer store;

    private VtexConnector connector;
    private StoreInfo storeInfo;

    @BeforeAll
    static void startStore() {
        store = new WireMockServer(wireMockConfig().dynamicPort());
        store.start();
    }

    @AfterAll
    static void stopStore() {
        store.stop();
    }

    @BeforeEach
    void setUp() {
        store.resetAll();
        StoreHttpProperties properties = new StoreHttpProperties(USER_AGENT, Duration.ofSeconds(2), Duration.ofSeconds(2));
        connector = new VtexConnector(
                StoreHttpConfiguration.createRestClient(properties),
                JsonMapper.builder().build(),
                new VtexNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        storeInfo = Fixtures.vtexStore("megasports", store.baseUrl());
    }

    private URI productUrl() {
        return URI.create(store.baseUrl() + "/kappa-botines---termoli-fg-blck/p");
    }

    @Test
    void fetchesProductThroughCatalogApi() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(okJson(Fixtures.read(Fixtures.MEGASPORTS_TERMOLI))));

        Optional<ObservedListing> listing = connector.fetchByUrl(storeInfo, productUrl());

        assertThat(listing).hasValueSatisfying(l -> {
            assertThat(l.externalId()).isEqualTo("1211155");
            assertThat(l.offers()).hasSize(7);
            assertThat(l.observedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void identifiesItselfWithItsOwnUserAgent() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(okJson(Fixtures.read(Fixtures.MEGASPORTS_TERMOLI))));

        connector.fetchByUrl(storeInfo, productUrl());

        store.verify(getRequestedFor(urlEqualTo(API_PATH)).withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void emptyArrayMeansProductNotFound() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(okJson("[]")));

        assertThat(connector.fetchByUrl(storeInfo, productUrl())).isEmpty();
    }

    @Test
    void http404MeansProductNotFound() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(aResponse().withStatus(404)));

        assertThat(connector.fetchByUrl(storeInfo, productUrl())).isEmpty();
    }

    @Test
    void serverErrorIsReportedAsSourceUnavailable() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> connector.fetchByUrl(storeInfo, productUrl()))
                .isInstanceOf(SourceUnavailableException.class)
                .hasMessageContaining("503");
    }

    @Test
    void invalidJsonIsReportedAsSourceUnavailable() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html>mantenimiento</html>")));

        assertThatThrownBy(() -> connector.fetchByUrl(storeInfo, productUrl()))
                .isInstanceOf(SourceUnavailableException.class);
    }

    @Test
    void timeoutIsReportedAsSourceUnavailable() {
        store.stubFor(get(urlEqualTo(API_PATH)).willReturn(okJson("[]").withFixedDelay(3_000)));

        assertThatThrownBy(() -> connector.fetchByUrl(storeInfo, productUrl()))
                .isInstanceOf(SourceUnavailableException.class);
    }

    @Test
    void rejectsUrlsThatAreNotVtexProductPages() {
        URI categoryPage = URI.create(store.baseUrl() + "/calzado/botines");

        assertThatThrownBy(() -> connector.fetchByUrl(storeInfo, categoryPage))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
