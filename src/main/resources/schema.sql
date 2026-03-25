-- Form A Clearance Card – one column per form blank
CREATE TABLE IF NOT EXISTS clearance_card (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    station         VARCHAR(100)  NOT NULL,
    issued_time     TIME          NOT NULL,
    issued_date     DATE          NOT NULL,
    train_number    VARCHAR(50)   NOT NULL,
    order_count     INT           NOT NULL,
    signal_stop_for VARCHAR(255),
    operator_name   VARCHAR(100)  NOT NULL
);

-- Normalized child rows for the repeating order-number list
CREATE TABLE IF NOT EXISTS clearance_card_order_number (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clearance_card_id BIGINT      NOT NULL REFERENCES clearance_card (id),
    sequence          INT         NOT NULL,
    order_number      VARCHAR(20) NOT NULL
);

