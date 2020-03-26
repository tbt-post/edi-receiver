CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE IF NOT EXISTS "document" (
  sender text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  doctype text NOT NULL,
  id uuid NOT NULL,
  "body" json NOT NULL,
  checksum text
);


CREATE TABLE IF NOT EXISTS order_return (
  sender text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  msgtype text NOT NULL,
  id uuid NOT NULL,
  order_id text NOT NULL,
  reason text NOT NULL,
  items json NOT NULL,
  total_price numeric(10,2) NOT NULL,
  money_back boolean NOT NULL,
  money_dest text NOT NULL,
  document_id text
);


CREATE TABLE IF NOT EXISTS change_state (
  sender text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  msgtype text NOT NULL,
  id uuid NOT NULL,
  state text NOT NULL,
  pentity uuid,
  weight integer,
  dimensions json,
  parcel_source text,
  delivery_service text,
  is_quasi boolean
);


CREATE TABLE IF NOT EXISTS order_payment (
  sender text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  id uuid NOT NULL,
  parcel_id uuid NOT NULL,
  contragent_id uuid NOT NULL,
  posop text NOT NULL,
  posorder_id text NOT NULL,
  merchant_id text NOT NULL,
  amount numeric(10,2) NOT NULL,
  payer_phone text,
  comment text
);


CREATE TABLE IF NOT EXISTS refill_payment (
  sender text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  id uuid NOT NULL,
  payer_phone text NOT NULL,
  user_id text,
  dest text NOT NULL,
  posop text NOT NULL,
  posorder_id text NOT NULL,
  merchant_id text NOT NULL,
  amount numeric(10,2) NOT NULL,
  comment text
);


CREATE TABLE IF NOT EXISTS wms_event (
  serial uuid NOT NULL,
  items json NOT NULL,
  condition text NOT NULL,
  ev_type text,
  ev_task text,
  ev_stage text,
  coordinates json,
  flow text,
  direction text,
  reason text,
  version text,
  origin uuid,
  owner uuid
);


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


CREATE TABLE IF NOT EXISTS wms_registry_announcement (
  source text NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  generated integer NOT NULL,
  serial integer NOT NULL,
  uid uuid NOT NULL,
  userial uuid NOT NULL,
  state text NOT NULL
);


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
