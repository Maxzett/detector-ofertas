package dev.marzetti.pricemonitor;

import org.springframework.boot.SpringApplication;

/**
 * Levanta la aplicación en local con un PostgreSQL de Testcontainers.
 * Útil para desarrollar sin docker compose: correr este main desde el IDE.
 */
public class TestPriceMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.from(PriceMonitorApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
