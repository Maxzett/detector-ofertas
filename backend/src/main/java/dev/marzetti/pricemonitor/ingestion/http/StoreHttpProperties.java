package dev.marzetti.pricemonitor.ingestion.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cómo se presenta y cuánto espera el sistema al consultar tiendas (ADR 0004).
 *
 * @param userAgent      identifica al proyecto ante la tienda; no se hace pasar por un navegador
 * @param connectTimeout tiempo máximo para conectar
 * @param readTimeout    tiempo máximo de respuesta
 */
@ConfigurationProperties("detector.http")
public record StoreHttpProperties(
        @DefaultValue("DetectorOfertas/0.1 (+https://github.com/Maxzett/detector-ofertas)") String userAgent,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("15s") Duration readTimeout) {
}
