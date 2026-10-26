-- =============================================================================
-- V1: schema inicial
--
-- Dos mundos separados (principio §27 del documento de requerimientos):
--   * Catálogo  -> store, category, product, listing, offer, price_history
--   * Alertas   -> app_user, alert, alert_store, alert_offer_state, notification
-- Se conectan solo a través de alert_offer_state. Borrar o editar una alerta
-- nunca toca el catálogo ni el historial.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Identidad
-- -----------------------------------------------------------------------------
CREATE TABLE app_user (
    id               BIGSERIAL PRIMARY KEY,
    email            TEXT        NOT NULL UNIQUE,
    password_hash    TEXT        NOT NULL,
    telegram_chat_id TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- Catálogo
-- -----------------------------------------------------------------------------
CREATE TABLE store (
    id                BIGSERIAL PRIMARY KEY,
    code              TEXT        NOT NULL UNIQUE,
    name              TEXT        NOT NULL,
    base_url          TEXT        NOT NULL,
    connector_type    TEXT        NOT NULL,
    config            JSONB       NOT NULL DEFAULT '{}',
    min_poll_interval INTERVAL    NOT NULL DEFAULT '1 hour',
    currency          CHAR(3)     NOT NULL DEFAULT 'ARS',
    enabled           BOOLEAN     NOT NULL DEFAULT false,
    CONSTRAINT ck_store_connector_type
        CHECK (connector_type IN ('VTEX', 'SFCC', 'MAGENTO_HTML', 'SPORT78_HTML', 'JSONLD'))
);

CREATE TABLE category (
    id               BIGSERIAL PRIMARY KEY,
    code             TEXT  NOT NULL UNIQUE,
    name             TEXT  NOT NULL,
    attribute_schema JSONB NOT NULL DEFAULT '{}'
);

-- Puntos de entrada para el discovery cuando la tienda no tiene búsqueda por API
-- (o su robots.txt la prohíbe): páginas de categoría o ids de categoría externos.
CREATE TABLE store_category (
    id          BIGSERIAL PRIMARY KEY,
    store_id    BIGINT  NOT NULL REFERENCES store (id),
    category_id BIGINT  NOT NULL REFERENCES category (id),
    external_ref TEXT   NOT NULL,     -- URL relativa o id de categoría de la tienda
    enabled     BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (store_id, category_id, external_ref)
);

-- Producto canónico, independiente de la tienda. Opcional en el MVP
-- (listing.product_id puede ser NULL hasta que exista el matching).
CREATE TABLE product (
    id             BIGSERIAL PRIMARY KEY,
    category_id    BIGINT NOT NULL REFERENCES category (id),
    brand          TEXT   NOT NULL,
    model          TEXT   NOT NULL,
    normalized_key TEXT   NOT NULL UNIQUE,
    attributes     JSONB  NOT NULL DEFAULT '{}'
);

-- Una publicación de producto en una tienda.
CREATE TABLE listing (
    id                 BIGSERIAL PRIMARY KEY,
    store_id           BIGINT      NOT NULL REFERENCES store (id),
    external_id        TEXT        NOT NULL,
    product_id         BIGINT      REFERENCES product (id),
    category_id        BIGINT      REFERENCES category (id),
    title              TEXT        NOT NULL,
    brand              TEXT,
    url                TEXT        NOT NULL,
    raw_attributes     JSONB       NOT NULL DEFAULT '{}',
    derived_attributes JSONB       NOT NULL DEFAULT '{}',  -- gama, superficie...
    first_seen_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (store_id, external_id)
);

-- Una variante concreta (SKU: talle/color) de un listing, con su precio actual.
-- El precio vive acá y no en el listing: en la Etapa 0 se vio que el mismo botín
-- tiene precios distintos según el talle.
CREATE TABLE offer (
    id                 BIGSERIAL PRIMARY KEY,
    listing_id         BIGINT        NOT NULL REFERENCES listing (id),
    store_id           BIGINT        NOT NULL REFERENCES store (id),
    external_sku_id    TEXT          NOT NULL,
    size_raw           TEXT,
    size_canonical     TEXT,
    color              TEXT,
    variant_attributes JSONB         NOT NULL DEFAULT '{}',
    current_price      NUMERIC(14, 2),
    list_price         NUMERIC(14, 2),
    in_stock           BOOLEAN       NOT NULL DEFAULT false,
    available_qty      INT,
    tracking_status    TEXT          NOT NULL DEFAULT 'TRACKED',
    next_check_at      TIMESTAMPTZ,
    last_checked_at    TIMESTAMPTZ,
    last_seen_at       TIMESTAMPTZ,
    last_changed_at    TIMESTAMPTZ,
    UNIQUE (store_id, external_sku_id),
    CONSTRAINT ck_offer_tracking_status CHECK (tracking_status IN ('TRACKED', 'DORMANT', 'GONE')),
    CONSTRAINT ck_offer_prices CHECK (
        (current_price IS NULL OR current_price >= 0) AND (list_price IS NULL OR list_price >= 0)
    )
);

-- Solo se inserta una fila cuando cambia (price, list_price, in_stock).
-- "No consultado" se distingue de "sin cambios" con offer.last_checked_at.
CREATE TABLE price_history (
    offer_id   BIGINT         NOT NULL REFERENCES offer (id),
    valid_from TIMESTAMPTZ    NOT NULL,
    price      NUMERIC(14, 2),
    list_price NUMERIC(14, 2),
    in_stock   BOOLEAN        NOT NULL,
    PRIMARY KEY (offer_id, valid_from)
);

-- -----------------------------------------------------------------------------
-- Alertas y notificaciones
-- -----------------------------------------------------------------------------
CREATE TABLE alert (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES app_user (id),
    name             TEXT          NOT NULL,
    category_id      BIGINT        REFERENCES category (id),
    keywords         TEXT,
    brands           TEXT[]        NOT NULL DEFAULT '{}',
    size_canonical   TEXT,
    extra_criteria   JSONB         NOT NULL DEFAULT '{}',  -- gama, color, superficie...
    min_price        NUMERIC(14, 2),
    max_price        NUMERIC(14, 2),
    min_discount_pct NUMERIC(5, 2),
    discount_basis   TEXT          NOT NULL DEFAULT 'STORE_LIST_PRICE',
    poll_interval    INTERVAL      NOT NULL DEFAULT '6 hours',
    status           TEXT          NOT NULL DEFAULT 'ACTIVE',
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version          INT           NOT NULL DEFAULT 0,      -- optimistic locking
    CONSTRAINT ck_alert_status CHECK (status IN ('ACTIVE', 'PAUSED')),
    CONSTRAINT ck_alert_discount_basis CHECK (discount_basis IN ('STORE_LIST_PRICE', 'LAST_PRICE', 'MIN_30D')),
    CONSTRAINT ck_alert_price_range CHECK (min_price IS NULL OR max_price IS NULL OR min_price <= max_price),
    CONSTRAINT ck_alert_discount CHECK (min_discount_pct IS NULL OR (min_discount_pct >= 0 AND min_discount_pct <= 100))
);

CREATE TABLE alert_store (
    alert_id BIGINT NOT NULL REFERENCES alert (id),
    store_id BIGINT NOT NULL REFERENCES store (id),
    PRIMARY KEY (alert_id, store_id)
);

CREATE TABLE alert_offer_state (
    alert_id            BIGINT         NOT NULL REFERENCES alert (id),
    offer_id            BIGINT         NOT NULL REFERENCES offer (id),
    matching            BOOLEAN        NOT NULL,
    last_matched_at     TIMESTAMPTZ,
    last_notified_price NUMERIC(14, 2),
    last_notified_at    TIMESTAMPTZ,
    PRIMARY KEY (alert_id, offer_id)
);

CREATE TABLE notification (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id),
    alert_id   BIGINT      NOT NULL REFERENCES alert (id),
    offer_id   BIGINT      NOT NULL REFERENCES offer (id),
    channel    TEXT        NOT NULL,
    payload    JSONB       NOT NULL,
    status     TEXT        NOT NULL DEFAULT 'PENDING',
    attempts   INT         NOT NULL DEFAULT 0,
    dedup_key  TEXT        NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ,
    CONSTRAINT ck_notification_channel CHECK (channel IN ('TELEGRAM')),
    CONSTRAINT ck_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- -----------------------------------------------------------------------------
-- Observabilidad de jobs
-- -----------------------------------------------------------------------------
CREATE TABLE job_run (
    id            BIGSERIAL PRIMARY KEY,
    job_type      TEXT        NOT NULL,
    store_id      BIGINT      REFERENCES store (id),
    started_at    TIMESTAMPTZ NOT NULL,
    finished_at   TIMESTAMPTZ,
    status        TEXT        NOT NULL,
    items_fetched INT,
    items_changed INT,
    error         TEXT,
    CONSTRAINT ck_job_run_status CHECK (status IN ('RUNNING', 'OK', 'PARTIAL', 'FAILED'))
);

-- -----------------------------------------------------------------------------
-- Índices para las consultas frecuentes (RNF-ESC-02)
-- -----------------------------------------------------------------------------
-- Scheduler: qué offers toca consultar ahora, por tienda.
CREATE INDEX ix_offer_due ON offer (store_id, next_check_at) WHERE tracking_status = 'TRACKED';
CREATE INDEX ix_offer_listing ON offer (listing_id);
CREATE INDEX ix_listing_store_category ON listing (store_id, category_id);
CREATE INDEX ix_listing_product ON listing (product_id) WHERE product_id IS NOT NULL;
-- El historial de una offer lo cubre la PK (offer_id, valid_from).
-- El "último precio" no lee el historial: está desnormalizado en offer.current_price.
CREATE INDEX ix_alert_active ON alert (user_id) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX ix_alert_offer_state_offer ON alert_offer_state (offer_id);
CREATE INDEX ix_notification_user ON notification (user_id, created_at DESC);
CREATE INDEX ix_notification_pending ON notification (created_at) WHERE status = 'PENDING';
CREATE INDEX ix_job_run_recent ON job_run (started_at DESC);
