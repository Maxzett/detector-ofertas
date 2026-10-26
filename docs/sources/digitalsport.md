# Digital Sport

- **URL:** https://www.digitalsport.com.ar
- **Plataforma:** propia (Grupo Sport 78)
- **Connector:** `SPORT78_HTML`
- **Verificado:** 2026-10-26

## Acceso

No tiene API pública ni JSON-LD. Los datos vienen en el **HTML renderizado del servidor**, así que alcanza con Jsoup y no hace falta un navegador.

Página de producto: `/sport78/prod/<slug>-<id>/`, por ejemplo `/sport78/prod/botines-zm-superfly-11-pro-fg-nike-719173/`.

## Datos disponibles

| Dato | Selector / fuente |
| --- | --- |
| Precio actual | `span.price`, y también `meta[property=og:price:amount]` |
| Precio anterior | `span.price_strikeout` |
| Talles | `label.sizeLbl`, con atributo `variant` (ej. `719173_7`) |
| Sin stock por talle | clase `nostock` en el `label` |
| Nombre | `h1` |

Escala de talle: **AR** (38, 39, 39.5, ...).

Ejemplo real: "BOTINES ZM SUPERFLY 11 PRO FG nike", $228.999 (antes $329.999).

## robots.txt

Prohíbe `/search/` y los parámetros de filtro (`variants=`, `discipline=`, `price_regular=`, `color1=`). Por eso el discovery va por páginas de categoría y no por búsqueda.

## Riesgos

- Es la fuente más frágil: depende de clases CSS. Necesita tests de contrato con fixtures reales.
- **Pendiente para la Etapa 2:** identificar la URL de la categoría de botines y guardarla en `store_category`.

## Decisión

Parte del primer corte del MVP (junto con Mega Sports).
