package dev.marzetti.pricemonitor.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación. Inyectarlo, en vez de llamar a {@code Instant.now()},
 * permite fijar el tiempo en los tests.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
