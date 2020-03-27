CREATE TABLE IF NOT EXISTS wms_stocktaking_message (
  serial uuid NOT NULL,
  reference uuid NOT NULL,
  message_type text NOT NULL,
  label text,
  item json,
  version text,
  origin uuid,
  owner uuid
);
