CREATE TABLE IF NOT EXISTS documents (
  sender text NOT NULL,
  ts timestamp with time zone NOT NULL,
  doctype text NOT NULL,
  id uuid NOT NULL,
  body json NOT NULL,
  checksum text
);
