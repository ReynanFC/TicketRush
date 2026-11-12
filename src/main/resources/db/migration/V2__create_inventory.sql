-- Inventory: stock per lot and the reservations that hold it (see docs/MODELAGEM.md, section 8.2, and ADR 0003).
-- lot_stock.lot_id has no FK to lots on purpose: the inventory only knows the lot by ID,
-- and in step 6 each service gets its own database.
-- Half-price columns (#20) and the version column for locking (#16) come in later migrations.

CREATE TABLE lot_stock (
    lot_id    UUID PRIMARY KEY,
    sector_id UUID NOT NULL,
    total     INT  NOT NULL,
    available INT  NOT NULL,
    CONSTRAINT lot_stock_total_check     CHECK (total > 0),
    CONSTRAINT lot_stock_available_check CHECK (available >= 0 AND available <= total)
);

CREATE TABLE reservations (
    id         UUID        PRIMARY KEY,
    order_id   UUID        NOT NULL,
    lot_id     UUID        NOT NULL REFERENCES lot_stock (lot_id),
    quantity   INT         NOT NULL,
    status     VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT reservations_quantity_check CHECK (quantity > 0),
    CONSTRAINT reservations_status_check   CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'RELEASED')),
    -- One reservation per order: a repeated reserve request for the same order cannot hold stock twice.
    CONSTRAINT reservations_order_unique   UNIQUE (order_id)
);

-- Postgres does not index foreign keys on its own.
CREATE INDEX reservations_lot_id_idx ON reservations (lot_id);
