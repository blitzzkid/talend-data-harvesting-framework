-- Audit schema written to by Talend jobs and read by the harvester.
-- Talend context variables (customer_id, env, source/target schema) flow into
-- these rows so the harvester can replay any run for any customer.

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.job_run (
    run_id           UUID PRIMARY KEY,
    job_name         VARCHAR(255) NOT NULL,
    job_version      VARCHAR(64),
    customer_id      VARCHAR(128) NOT NULL,
    env              VARCHAR(32)  NOT NULL,
    context_params   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status           VARCHAR(32)  NOT NULL,
    started_at       TIMESTAMPTZ  NOT NULL,
    ended_at         TIMESTAMPTZ,
    error_message    TEXT
);

CREATE INDEX idx_job_run_customer       ON audit.job_run (customer_id, started_at DESC);
CREATE INDEX idx_job_run_job_name       ON audit.job_run (job_name, started_at DESC);

CREATE TABLE audit.dataset_io (
    id               BIGSERIAL PRIMARY KEY,
    run_id           UUID NOT NULL REFERENCES audit.job_run (run_id) ON DELETE CASCADE,
    role             VARCHAR(16) NOT NULL CHECK (role IN ('source', 'target')),
    connection_name  VARCHAR(255) NOT NULL,
    schema_name      VARCHAR(255) NOT NULL,
    table_name       VARCHAR(255) NOT NULL,
    row_count        BIGINT,
    captured_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_dataset_io_run ON audit.dataset_io (run_id);

CREATE TABLE audit.column_lineage (
    id                    BIGSERIAL PRIMARY KEY,
    run_id                UUID NOT NULL REFERENCES audit.job_run (run_id) ON DELETE CASCADE,
    source_schema         VARCHAR(255) NOT NULL,
    source_table          VARCHAR(255) NOT NULL,
    source_column         VARCHAR(255) NOT NULL,
    target_schema         VARCHAR(255) NOT NULL,
    target_table          VARCHAR(255) NOT NULL,
    target_column         VARCHAR(255) NOT NULL,
    transformation_expr   TEXT
);

CREATE INDEX idx_column_lineage_run    ON audit.column_lineage (run_id);
CREATE INDEX idx_column_lineage_target ON audit.column_lineage (target_schema, target_table);

CREATE TABLE audit.run_metric (
    id            BIGSERIAL PRIMARY KEY,
    run_id        UUID NOT NULL REFERENCES audit.job_run (run_id) ON DELETE CASCADE,
    metric_name   VARCHAR(128) NOT NULL,
    metric_value  NUMERIC(38, 6),
    metric_text   TEXT,
    captured_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_run_metric_run ON audit.run_metric (run_id);

-- Per-customer config the harvester reads to know where in TDC to land things.
CREATE TABLE audit.customer_config (
    customer_id          VARCHAR(128) PRIMARY KEY,
    display_name         VARCHAR(255) NOT NULL,
    tdc_folder_path      VARCHAR(512) NOT NULL,
    tdc_model_id         VARCHAR(128),
    custom_attributes    JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Watermark per customer so the scheduler can resume incrementally.
CREATE TABLE audit.harvest_watermark (
    customer_id        VARCHAR(128) PRIMARY KEY REFERENCES audit.customer_config (customer_id) ON DELETE CASCADE,
    last_run_started   TIMESTAMPTZ,
    last_run_id        UUID,
    last_harvested_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
