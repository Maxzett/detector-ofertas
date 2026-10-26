# Dexter (y Stock Center, Moov)

- **URL:** https://www.dexter.com.ar
- **Plataforma:** Salesforce Commerce Cloud (SFRA), del mismo grupo que Stock Center y Moov
- **Connector:** `SFCC`
- **Verificado:** 2026-10-26

## Acceso

- **Discovery:** páginas de categoría, por ejemplo `/calzado/botines` o `/categorias/botinesfg`. Cada producto trae `data-pid`.
- **Detalle:** endpoint JSON del storefront:

```
GET /on/demandware.store/Sites-Dexter-Site/es_AR/Product-Variation?pid=<pid>&quantity=1
Header: X-Requested-With: XMLHttpRequest
```

## Datos disponibles

| Dato | Campo |
| --- | --- |
| Nombre, marca | `product.productName`, `product.brand` |
| Precio actual | `product.price.sales.value` |
| Precio de lista | `product.price.list.value` |
| Talles | `product.variationAttributes[id=size].values[]` |
| Disponibilidad por talle | `selectable` (a confirmar con un `pid` de variante) |

Escala de talle: **AR**, a veces con rangos ("38-38.5", "42-42.5").

Ejemplo real: "Botines Fútbol Nike Tiempo Legend 10 Academy Hombre", $89.999 (lista $189.999).

## robots.txt

Prohíbe parámetros de filtro y orden (`pmin`, `pmax`, `prefn*`, `prefv*`, `srule`), además de carrito, checkout y cuenta. Las páginas de categoría y de producto están permitidas.

## Decisión

Siguiente connector después del primer corte. Vende Nike, adidas y Puma, así que cubre el hueco de nike.com.ar. El mismo connector serviría para Stock Center y Moov cambiando el `siteId`.
