# ADR 0002: Un connector por plataforma de e-commerce, no por tienda

- **Estado:** aceptada
- **Fecha:** 2026-10-26

## Contexto

La idea inicial era arrancar solo con tiendas VTEX, que exponen una API de catálogo pública. La Etapa 0 (ver `docs/sources/`) mostró que las tiendas deportivas relevantes de Argentina usan plataformas distintas:

| Plataforma | Tiendas |
| --- | --- |
| VTEX | Mega Sports, Nike Argentina |
| Salesforce Commerce Cloud | Dexter, Stock Center, Moov, adidas |
| Magento 2 | Solo Deportes, Open Sports |
| Propia (Sport 78) | Digital Sport |

## Decisión

- Cada `StoreConnector` implementa una **plataforma**: `VTEX`, `SFCC`, `MAGENTO_HTML`, `SPORT78_HTML` y `JSONLD`. La tienda es configuración (`store.config`), no código. Agregar una tienda de una plataforma conocida es una fila en la tabla `store`.
- Cada connector declara sus `capabilities` (búsqueda, consulta en lote, stock por variante). El resto del sistema se adapta a lo que la fuente puede dar.
- Cuando la tienda no tiene búsqueda por API, o su robots.txt la prohíbe, el discovery recorre páginas de categoría configuradas en `store_category`.
- Primer corte del MVP: **VTEX (Mega Sports) + Sport 78 HTML (Digital Sport)**. Es el connector más simple más el más frágil, y juntos prueban que el diseño soporta los dos extremos.

## Consecuencias

- Los connectors HTML (Sport 78, Magento) dependen del markup de la tienda. Llevan tests de contrato con fixtures reales, para detectar cuando el sitio cambia.
- El talle viene en escalas distintas según la tienda (AR, US, EU, o varias en la misma etiqueta). Se normaliza a una escala canónica en el connector.
