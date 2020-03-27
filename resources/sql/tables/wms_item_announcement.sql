CREATE TABLE IF NOT EXISTS wms_item_announcement (
  serial uuid NOT NULL,
  item json NOT NULL,
  zone_from text,
  zone_to text,
  reference uuid NOT NULL,
  version text,
  origin uuid,
  owner uuid
);
