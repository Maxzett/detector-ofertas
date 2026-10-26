package dev.marzetti.pricemonitor.ingestion.vtex;

import dev.marzetti.pricemonitor.catalog.ObservedListing;
import dev.marzetti.pricemonitor.catalog.StoreInfo;
import dev.marzetti.pricemonitor.ingestion.SourceUnavailableException;
import dev.marzetti.pricemonitor.ingestion.port.ConnectorCapability;
import dev.marzetti.pricemonitor.ingestion.port.StoreConnector;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Connector para tiendas sobre VTEX. Usa la API pública de catálogo, que es la misma que
 * consume el frontend de la tienda: no requiere login ni key, y no se rompe cuando la tienda
 * cambia el diseño del sitio.
 */
@Component
public class VtexConnector implements StoreConnector {

    /** URL de producto VTEX: {@code https://tienda.com/<linkText>/p}. */
    private static final Pattern PRODUCT_PATH = Pattern.compile("^/([^/]+)/p/?$");
    private static final String PRODUCT_BY_LINK_TEXT = "/api/catalog_system/pub/products/search/{linkText}/p";

    private final RestClient http;
    private final JsonMapper json;
    private final VtexNormalizer normalizer;
    private final Clock clock;

    public VtexConnector(RestClient storeRestClient, JsonMapper json, VtexNormalizer normalizer, Clock clock) {
        this.http = storeRestClient;
        this.json = json;
        this.normalizer = normalizer;
        this.clock = clock;
    }

    @Override
    public String connectorType() {
        return "VTEX";
    }

    @Override
    public Set<ConnectorCapability> capabilities() {
        return EnumSet.of(ConnectorCapability.SEARCH, ConnectorCapability.FETCH_BY_URL,
                ConnectorCapability.LIST_PRICE, ConnectorCapability.STOCK_PER_VARIANT);
    }

    @Override
    public Optional<ObservedListing> fetchByUrl(StoreInfo store, URI productUrl) {
        String linkText = linkText(productUrl);
        URI apiUrl = UriComponentsBuilder.fromUri(store.baseUrl())
                .path(PRODUCT_BY_LINK_TEXT)
                .buildAndExpand(linkText)
                .encode()
                .toUri();

        String body = get(store, apiUrl);
        if (body == null) {
            return Optional.empty();
        }

        JsonNode products = parse(store, body);
        if (!products.isArray() || products.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(normalizer.normalize(store, products.get(0), clock.instant()));
    }

    /** @return el body, o {@code null} si la tienda responde 404 */
    private String get(StoreInfo store, URI apiUrl) {
        try {
            return http.get()
                    .uri(apiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                            return null;
                        }
                        if (response.getStatusCode().isError()) {
                            throw new SourceUnavailableException("%s respondió HTTP %d en %s"
                                    .formatted(store.code(), response.getStatusCode().value(), apiUrl));
                        }
                        return readBody(response.getBody().readAllBytes());
                    });
        } catch (RestClientException e) {
            throw new SourceUnavailableException("No se pudo consultar %s: %s".formatted(store.code(), e.getMessage()), e);
        }
    }

    private static String readBody(byte[] bytes) throws IOException {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private JsonNode parse(StoreInfo store, String body) {
        try {
            return json.readTree(body);
        } catch (JacksonException e) {
            throw new SourceUnavailableException("%s devolvió un JSON inválido".formatted(store.code()), e);
        }
    }

    static String linkText(URI productUrl) {
        String path = productUrl.getPath();
        Matcher matcher = PRODUCT_PATH.matcher(path == null ? "" : path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "No es una URL de producto VTEX (se espera /<producto>/p): " + productUrl);
        }
        return matcher.group(1);
    }
}
