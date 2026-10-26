package dev.marzetti.pricemonitor;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifica los límites entre módulos (catalog, ingestion, alerts, notifications,
 * identity, web, shared): sin ciclos y sin acceso a paquetes internos de otro módulo.
 * Si alguien importa una clase interna de otro módulo, este test falla.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(PriceMonitorApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }
}
