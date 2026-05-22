-- Connection NAMES are what TDC later "stitches" to harvested data stores,
-- so align them with how the real Postgres DB / file store are cataloged.
CREATE CONNECTION "FileSystem"  TYPE "File System";
CREATE CONNECTION "PostgreSQL"  TYPE "Relational Database";

-- Stage 1 — PROCESSING_FILE / FILE_INGESTED: landing file -> bronze table
SELECT customer_id  AS customer_id,
       full_name    AS full_name,
       email        AS email,
       signup_date  AS signup_date
INTO   bronze.customers@"PostgreSQL"
FROM   landing/customers.csv@"FileSystem";

-- Stage 2 — BRONZE_TO_SILVER (multi-source): two bronze tables -> one silver table
SELECT o.order_id     AS order_id,
       o.amount       AS amount,
       c.customer_id  AS customer_id,
       c.full_name    AS customer_name
INTO   silver.orders_enriched@"PostgreSQL"
FROM   bronze.orders@"PostgreSQL" o
JOIN   bronze.customers@"PostgreSQL" c ON o.customer_id = c.customer_id;

-- Stage 3 — SILVER_TO_GOLD: silver table -> gold dimension
SELECT customer_id  AS customer_key,
       full_name     AS customer_name,
       email         AS email_address,
       signup_date   AS first_seen_date
INTO   gold.dim_customer@"PostgreSQL"
FROM   silver.customers@"PostgreSQL";
