# Solo Deportes

- **URL:** https://www.solodeportes.com.ar
- **Plataforma:** Magento 2
- **Connector:** `MAGENTO_HTML`
- **Verificado:** 2026-10-26

## Acceso

La API GraphQL está deshabilitada (404), y robots.txt prohíbe `/graphql` y `/rest/`. Se usa el HTML del servidor:

- **Listado:** `/calzado/botines.html`. Cada tarjeta trae `data-price-amount` con `data-price-type="finalPrice"`.
- **Producto:** el HTML incluye `jsonSwatchConfig` con los talles y sus SKUs.

## Datos disponibles

| Dato | Fuente |
| --- | --- |
| Precio actual | `[data-price-type=finalPrice][data-price-amount]` |
| Precio anterior | bloque `old-price` (cuando hay oferta) |
| Talles y SKU | `jsonSwatchConfig` |
| Stock por talle | a confirmar |

Escala de talle: varias en la misma etiqueta, por ejemplo `UK: 8.0 US: 8.5 AR: 40`. Se toma el valor AR.

## Decisión

Siguiente después de Dexter. Open Sports usa la misma plataforma (GraphQL → 403), así que probablemente le sirve el mismo connector.
