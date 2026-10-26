package dev.marzetti.pricemonitor.ingestion.http;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP compartido por todos los connectors. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StoreHttpProperties.class)
public class StoreHttpConfiguration {

    @Bean
    public RestClient storeRestClient(StoreHttpProperties properties) {
        return createRestClient(properties);
    }

    /** Público y estático para que los tests de connectors usen exactamente el mismo cliente. */
    public static RestClient createRestClient(StoreHttpProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }
}
