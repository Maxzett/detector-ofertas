# Nike Argentina

- **URL:** https://www.nike.com.ar (operada por Southbay)
- **Plataforma:** VTEX (cuenta `nikearprod`)
- **Verificado:** 2026-10-26

## Hallazgos

- Desde un navegador real, la API de catálogo VTEX responde JSON completo: precio, precio de lista y stock por talle (escala **US**). robots.txt no la prohíbe.
- Desde un cliente HTTP sin navegador (`curl`), Cloudflare responde **403**.
- Dato útil para el diseño: el mismo botín tenía precios distintos según el talle ($349.999 en los talles con stock, $499.999 en los agotados).

## Decisión

**Descartada como fuente directa** (ver ADR 0004): pasar el filtro requeriría simular un navegador para evadir la protección anti-bot. Los productos Nike se siguen a través de Dexter, Digital Sport y Mega Sports.
