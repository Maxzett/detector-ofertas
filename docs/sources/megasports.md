# Mega Sports

- **URL:** https://www.megasports.com.ar
- **Plataforma:** VTEX
- **Connector:** `VTEX`
- **Verificado:** 2026-10-26

## Acceso

API de catálogo pública, sin login ni key:

```
GET /api/catalog_system/pub/products/search?ft=botines&_from=0&_to=49
```

- Hasta 50 resultados por request (`_from`/`_to`) y un tope de 2500 por búsqueda.
- También filtra por categoría. El árbol de categorías viene en `categories`, por ejemplo `/Calzado/Botines/`.

## Datos disponibles

| Dato | Campo |
| --- | --- |
| Nombre, marca | `productName`, `brand` |
| SKU por talle | `items[].itemId`, `items[].Talle` |
| Precio actual | `items[].sellers[0].commertialOffer.Price` |
| Precio de lista | `items[].sellers[0].commertialOffer.ListPrice` |
| Stock | `items[].sellers[0].commertialOffer.AvailableQuantity` |
| URL | `link` |

Escala de talle: **AR** (35, 36, ... 45).

Ejemplo real: "KAPPA BOTINES - INVICTUS BLCKROL", con `Price` 45499 y `ListPrice` 64999.

## Decisión

Fuente principal del primer corte del MVP.
