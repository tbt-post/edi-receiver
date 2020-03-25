CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS "document";
DROP TABLE IF EXISTS order_return;
DROP TABLE IF EXISTS change_state;
DROP TABLE IF EXISTS order_payment;
DROP TABLE IF EXISTS refill_payment;
DROP TABLE IF EXISTS wms_event;
DROP TABLE IF EXISTS wms_item_announcement;
DROP TABLE IF EXISTS wms_registry_announcement;
DROP TABLE IF EXISTS wms_stocktaking_message;


DROP TYPE IF EXISTS t_sender;
DROP TYPE IF EXISTS t_doctype;
DROP TYPE IF EXISTS t_msgtype;
DROP TYPE IF EXISTS t_return_reason;
DROP TYPE IF EXISTS t_money_dest;
DROP TYPE IF EXISTS t_parcel_source;
DROP TYPE IF EXISTS t_delivery_service;
DROP TYPE IF EXISTS t_posop;
DROP TYPE IF EXISTS t_wms_event_condition;
DROP TYPE IF EXISTS t_wms_event_type;
DROP TYPE IF EXISTS t_flow;
DROP TYPE IF EXISTS t_direction;
DROP TYPE IF EXISTS t_wms_registry_state;
DROP TYPE IF EXISTS t_wms_stocktaking_message_type;


CREATE TYPE t_sender AS ENUM ('tabata', 'tabata_wms', 'tbt', 'tbt_bms', 'tbt_wms', 'erp_api', 'wms_exchange', 'external_api');
CREATE TYPE t_doctype AS ENUM ('DocReturnOrders');
CREATE TYPE t_msgtype AS ENUM ('OrderReturn', 'OrderCancel', 'ChangeState');
CREATE TYPE t_return_reason AS ENUM ('other', 'properties', 'defects', 'manufact_defects', 'difference', 'size', 'size_plus', 'size_minus', 'color', 'wrong');
CREATE TYPE t_money_dest AS ENUM ('card', 'wallet', 'account', 'ukrpost');
CREATE TYPE t_parcel_source AS ENUM ('external');
CREATE TYPE t_delivery_service AS ENUM ('tabata', 'ukrposhta', 'novaposhta', 'meest', 'intime', 'kastapost', 'justin');
CREATE TYPE t_posop AS ENUM ('fcs_term');
CREATE TYPE t_wms_event_condition AS ENUM ('exception', 'transition', 'exchange', 'action');
CREATE TYPE t_wms_event_type AS ENUM ('creating', 'scanning', 'marking', 'placing', 'processing', 'sorting', 'searching', 'classifying', 'packing', 'inventing', 'unloading', 'correcting');
CREATE TYPE t_flow AS ENUM ('empty', 'auto', 'unidirectional', 'bidirectional');
CREATE TYPE t_direction AS ENUM ('direct', 'reverse');
CREATE TYPE t_wms_registry_state AS ENUM ('created', 'updated', 'closed');
CREATE TYPE t_wms_stocktaking_message_type AS ENUM ('start', 'evaluation', 'error', 'finish');


CREATE TABLE "document" (
  sender t_sender NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  doctype t_doctype NOT NULL,
  id uuid NOT NULL,
  "body" json NOT NULL,
  checksum text
);


CREATE TABLE order_return (
  sender t_sender NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  msgtype t_msgtype NOT NULL,
  id uuid NOT NULL,
  order_id text NOT NULL,
  reason t_return_reason NOT NULL,
  items json NOT NULL,
  total_price numeric(10,2) NOT NULL,
  money_back boolean NOT NULL,
  money_dest t_money_dest NOT NULL,
  document_id text
);


CREATE TABLE change_state (
  sender t_sender NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  msgtype t_msgtype NOT NULL,
  id uuid NOT NULL,
  state text NOT NULL,
  pentity uuid,
  weight integer,
  dimensions json,
  parcel_source t_parcel_source,
  delivery_service t_delivery_service,
  is_quasi boolean
);


CREATE TABLE order_payment (
  sender t_sender NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  id uuid NOT NULL,
  parcel_id uuid NOT NULL,
  contragent_id uuid NOT NULL,
  posop t_posop NOT NULL,
  posorder_id text NOT NULL,
  merchant_id text NOT NULL,
  amount numeric(10,2) NOT NULL,
  payer_phone text,
  comment text
);


CREATE TABLE refill_payment (
  sender t_sender NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  id uuid NOT NULL,
  payer_phone text NOT NULL,
  user_id text,
  dest t_money_dest NOT NULL,
  posop t_posop NOT NULL,
  posorder_id text NOT NULL,
  merchant_id text NOT NULL,
  amount numeric(10,2) NOT NULL,
  comment text
);


CREATE TABLE wms_event (
  serial uuid NOT NULL,
  items json NOT NULL,
  condition t_wms_event_condition NOT NULL,
  ev_type t_wms_event_type,
  ev_task text,
  ev_stage text,
  coordinates json,
  flow t_flow,
  direction t_direction,
  reason text,
  version text,
  origin uuid,
  owner uuid
);


CREATE TABLE wms_item_announcement (
  serial uuid NOT NULL,
  item json NOT NULL,
  zone_from text,
  zone_to text,
  reference uuid NOT NULL,
  version text,
  origin uuid,
  owner uuid
);


CREATE TABLE wms_registry_announcement (
  source t_sender NOT NULL,
  "timestamp" timestamp with time zone NOT NULL,
  generated integer NOT NULL,
  serial integer NOT NULL,
  uid uuid NOT NULL,
  userial uuid NOT NULL,
  state t_wms_registry_state NOT NULL
);


CREATE TABLE wms_stocktaking_message (
  serial uuid NOT NULL,
  reference uuid NOT NULL,
  message_type t_wms_stocktaking_message_type NOT NULL,
  label text,
  item json,
  version text,
  origin uuid,
  owner uuid
);
