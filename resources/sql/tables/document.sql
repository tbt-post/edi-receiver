CREATE TABLE IF NOT EXISTS "document" (
  sender text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  doctype text NOT NULL,
  id uuid NOT NULL,
  "body" json NOT NULL,
  checksum text
);
