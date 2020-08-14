CREATE TABLE IF NOT EXISTS migrations (
    table_name varchar(64) NOT NULL,
    model_version integer NOT NULL,
    applied_at datetime DEFAULT now(),  -- Note: seconds precision
    CONSTRAINT migration_pkey PRIMARY KEY (table_name, model_version)
);

-- init migrations table if not inited yet
INSERT INTO migrations (table_name, model_version)
SELECT table_name, 0
FROM information_schema.tables
WHERE table_schema = database() AND table_name != 'migrations'
    AND table_name NOT IN (SELECT table_name FROM migrations WHERE model_version=0);

CREATE TABLE IF NOT EXISTS ringbuffer (
	id serial,
    instance_id integer NOT NULL,
    created_at datetime NOT NULL,
    planned_at datetime NOT NULL,
    tries integer NOT NULL DEFAULT 0,
    payload text NOT NULL,
    error text NOT NULL
);

SET @sql := (
    SELECT
        IF (
            COUNT(*)>0,
            'SELECT null',
            'CREATE INDEX idx_ringbuffer_tries_created_at USING btree ON ringbuffer (tries, created_at)'
        )
    FROM information_schema.statistics
    WHERE table_schema = database() AND table_name = 'ringbuffer' and index_name = 'idx_ringbuffer_tries_created_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;

SET @sql := (
    SELECT
        IF (
            COUNT(*)>0,
            'SELECT null',
            'CREATE INDEX idx_ringbuffer_instance_id USING hash ON ringbuffer (instance_id)'
        )
    FROM information_schema.statistics
    WHERE table_schema = database() AND table_name = 'ringbuffer' and index_name = 'idx_ringbuffer_instance_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
