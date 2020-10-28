CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS migrations (
    table_name varchar(64) NOT NULL,
    model_version integer NOT NULL,
    applied_at timestamp with time zone DEFAULT now(),
    CONSTRAINT migration_pkey PRIMARY KEY (table_name, model_version)
);

-- init migrations table if not inited yet
INSERT INTO migrations (table_name, model_version)
SELECT table_name, 0
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name != 'migrations'
    AND table_name NOT IN (SELECT table_name FROM migrations WHERE model_version=0);

CREATE TABLE IF NOT EXISTS ringbuffer (
	id serial,
    instance_id integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    planned_at timestamp with time zone NOT NULL,
    tries integer NOT NULL DEFAULT 0,
    payload text NOT NULL,
    error text NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ringbuffer_tries_created_at ON ringbuffer USING btree (tries, created_at);
CREATE INDEX IF NOT EXISTS idx_ringbuffer_instance_id ON ringbuffer USING hash (instance_id);


CREATE TABLE IF NOT EXISTS log (
	created_at timestamp with time zone NOT NULL,
	context jsonb not null,
	reference jsonb NOT NULL,
	content jsonb NOT NULL,
	raw bytea
);
