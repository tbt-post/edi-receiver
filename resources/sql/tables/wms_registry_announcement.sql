CREATE TABLE IF NOT EXISTS wms_registry_announcement (
  source text NOT NULL,
  ts timestamp with time zone NOT NULL,
  generated integer NOT NULL,
  serial integer NOT NULL,
  uid uuid NOT NULL,
  userial uuid NOT NULL,
  state text NOT NULL
);
