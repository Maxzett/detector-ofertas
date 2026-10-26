-- =============================================================================
-- V2: tiendas y categoría del MVP
--
-- Surge de la Etapa 0 (ver docs/sources/). Solo se habilitan las dos fuentes del
-- primer corte del MVP: una VTEX (Mega Sports) y una HTML propia (Digital Sport).
-- nike.com.ar no se carga: Cloudflare bloquea clientes que no son navegador y no
-- se evaden protecciones. Los productos Nike se siguen a través de revendedores.
-- =============================================================================

INSERT INTO category (code, name, attribute_schema) VALUES
('botines-futbol', 'Botines de fútbol', '{
  "sizeScale": "AR",
  "attributes": {
    "surface": { "type": "enum", "values": ["FG", "AG", "MG", "SG", "TF", "IC"] },
    "tier":    { "type": "enum", "values": ["ALTA", "MEDIA_ALTA", "MEDIA", "BASICA"] }
  },
  "tierKeywords": {
    "ALTA":       ["elite", "ultimate"],
    "MEDIA_ALTA": ["pro"],
    "MEDIA":      ["academy", "league", "match"],
    "BASICA":     ["club", "play"]
  }
}');

INSERT INTO store (code, name, base_url, connector_type, config, min_poll_interval, enabled) VALUES
('megasports',   'Mega Sports',   'https://www.megasports.com.ar',   'VTEX',
    '{"sizeScale": "AR", "pageSize": 50, "requestDelayMs": 3000}', '1 hour', true),
('digitalsport', 'Digital Sport', 'https://www.digitalsport.com.ar', 'SPORT78_HTML',
    '{"sizeScale": "AR", "requestDelayMs": 5000}', '2 hours', true),
('dexter',       'Dexter',        'https://www.dexter.com.ar',       'SFCC',
    '{"sizeScale": "AR", "requestDelayMs": 3000, "siteId": "Sites-Dexter-Site", "locale": "es_AR"}', '1 hour', false),
('solodeportes', 'Solo Deportes', 'https://www.solodeportes.com.ar', 'MAGENTO_HTML',
    '{"sizeScale": "AR", "requestDelayMs": 5000}', '2 hours', false),
('adidas',       'adidas Argentina', 'https://www.adidas.com.ar',    'JSONLD',
    '{"sizeScale": "EU", "requestDelayMs": 5000, "stockPerSize": false}', '2 hours', false);

-- Puntos de entrada verificados en la Etapa 0. Digital Sport queda pendiente:
-- falta identificar la URL de su categoría de botines (Etapa 2).
INSERT INTO store_category (store_id, category_id, external_ref)
SELECT s.id, c.id, v.external_ref
FROM (VALUES
        ('megasports',   '/Calzado/Botines/'),
        ('dexter',       '/calzado/botines'),
        ('solodeportes', '/calzado/botines.html'),
        ('adidas',       '/botines')
     ) AS v (store_code, external_ref)
JOIN store s    ON s.code = v.store_code
JOIN category c ON c.code = 'botines-futbol';
