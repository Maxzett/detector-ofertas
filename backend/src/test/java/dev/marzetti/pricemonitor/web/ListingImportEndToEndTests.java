package dev.marzetti.pricemonitor.web;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.marzetti.pricemonitor.TestcontainersConfiguration;
import dev.marzetti.pricemonitor.ingestion.Fixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Recorrido completo: HTTP → ListingImporter → VtexConnector → tienda falsa (WireMock)
 * → OfferUpdateService → PostgreSQL. La tienda falsa responde con el fixture real de Mega Sports.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ListingImportEndToEndTests {

    private static final String LINK_TEXT = "kappa-botines---termoli-fg-blck";
    private static final String API_PATH = "/api/catalog_system/pub/products/search/" + LINK_TEXT + "/p";

    private static WireMockServer fakeStore;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcClient jdbc;

    private MockMvc mvc;

    @BeforeAll
    static void startFakeStore() {
        fakeStore = new WireMockServer(wireMockConfig().dynamicPort());
        fakeStore.start();
    }

    @AfterAll
    static void stopFakeStore() {
        fakeStore.stop();
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        fakeStore.resetAll();
        jdbc.sql("TRUNCATE price_history, alert_offer_state, notification, offer, listing RESTART IDENTITY CASCADE")
                .update();
        // Tienda VTEX de prueba apuntando a WireMock. Deshabilitada para no alterar el seed del MVP.
        jdbc.sql("""
                        INSERT INTO store (code, name, base_url, connector_type, config, enabled)
                        VALUES ('fake-vtex', 'Tienda VTEX de prueba', :baseUrl, 'VTEX', '{"sizeScale": "AR"}', false)
                        ON CONFLICT (code) DO UPDATE SET base_url = EXCLUDED.base_url
                        """)
                .param("baseUrl", fakeStore.baseUrl())
                .update();
    }

    private String importRequest(String productUrl) {
        return "{\"url\": \"" + productUrl + "\"}";
    }

    private String productUrl() {
        return fakeStore.baseUrl() + "/" + LINK_TEXT + "/p";
    }

    @Test
    void importsAProductAndThenDetectsAPriceDrop() throws Exception {
        String fixture = Fixtures.read(Fixtures.MEGASPORTS_TERMOLI);
        fakeStore.stubFor(get(urlEqualTo(API_PATH)).willReturn(okJson(fixture)));

        mvc.perform(post("/api/internal/listings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest(productUrl())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeCode").value("fake-vtex"))
                .andExpect(jsonPath("$.externalId").value("1211155"))
                .andExpect(jsonPath("$.offersSeen").value(7))
                .andExpect(jsonPath("$.offersCreated").value(7))
                .andExpect(jsonPath("$.offersChanged").value(0));

        // La tienda baja el precio de todos los talles
        fakeStore.stubFor(get(urlEqualTo(API_PATH))
                .willReturn(okJson(fixture.replace("\"Price\": 44499", "\"Price\": 39999"))));

        mvc.perform(post("/api/internal/listings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest(productUrl())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offersCreated").value(0))
                .andExpect(jsonPath("$.offersChanged").value(7));

        Integer historyRows = jdbc.sql("SELECT count(*) FROM price_history").query(Integer.class).single();
        assertThat(historyRows).isEqualTo(14);
    }

    @Test
    void unknownStoreIsUnprocessable() throws Exception {
        mvc.perform(post("/api/internal/listings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest("https://tienda-desconocida.com.ar/algo/p")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title").value("Tienda no soportada"));
    }

    @Test
    void storeWithoutConnectorIsUnprocessable() throws Exception {
        // Dexter está en el seed, pero su connector (SFCC) todavía no existe
        mvc.perform(post("/api/internal/listings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest("https://www.dexter.com.ar/botines/NIDV4337-601.html")))
                .andExpect(status().is(422));
    }

    @Test
    void missingProductIsNotFound() throws Exception {
        fakeStore.stubFor(get(urlEqualTo(API_PATH)).willReturn(okJson("[]")));

        mvc.perform(post("/api/internal/listings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest(productUrl())))
                .andExpect(status().isNotFound());
    }

    @Test
    void relativeUrlIsABadRequest() throws Exception {
        mvc.perform(post("/api/internal/listings/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest("/" + LINK_TEXT + "/p")))
                .andExpect(status().isBadRequest());
    }
}
