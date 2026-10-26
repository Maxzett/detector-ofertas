# adidas Argentina

- **URL:** https://www.adidas.com.ar
- **Plataforma:** Next.js sobre Salesforce Commerce Cloud
- **Connector:** `JSONLD`
- **Verificado:** 2026-10-26

## Acceso

La página de categoría (`/botines`) incluye JSON-LD (`ItemList`) con 48 productos, cada uno con `offers.price`, `priceCurrency` y `availability`.

## Limitación

Existe `/api/products/<id>/availability`, que devuelve el stock por talle, pero **robots.txt lo prohíbe explícitamente** (`Disallow: /api/products/*/availability`). No se usa.

Tampoco hay precio de lista en el JSON-LD.

## Decisión

Fuente parcial: sirve para alertas por precio, no por talle. Para seguir modelos de adidas por talle, usar Dexter o Solo Deportes.
