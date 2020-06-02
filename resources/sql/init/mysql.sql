CREATE TABLE IF NOT EXISTS migrations (
    table_name varchar(64) NOT NULL,
    model_version integer NOT NULL,
    applied_at datetime DEFAULT now(),
    CONSTRAINT migration_pkey PRIMARY KEY (table_name, model_version)
);

-- init migrations table if not inited yet
INSERT INTO migrations (table_name, model_version)
SELECT table_name, 0
FROM information_schema.tables
WHERE table_schema = database() AND table_name != 'migrations'
    AND table_name NOT IN (SELECT table_name FROM migrations WHERE model_version=0);
