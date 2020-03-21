CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS "document";
DROP TABLE IF EXISTS order_return;
DROP TABLE IF EXISTS change_state;

DROP TYPE IF EXISTS t_sender;
DROP TYPE IF EXISTS t_doctype;
DROP TYPE IF EXISTS t_msgtype;
DROP TYPE IF EXISTS t_return_reason;
DROP TYPE IF EXISTS t_money_dest;
DROP TYPE IF EXISTS t_parcel_source;
DROP TYPE IF EXISTS t_delivery_service;


CREATE TYPE t_sender AS ENUM ('tabata', 'tabata_wms', 'tbt', 'tbt_bms', 'tbt_wms');
CREATE TYPE t_doctype AS ENUM ('DocReturnOrders');
CREATE TYPE t_msgtype AS ENUM ('OrderReturn', 'OrderCancel', 'ChangeState');
CREATE TYPE t_return_reason AS ENUM ('other', 'properties', 'defects', 'manufact_defects', 'difference', 'size', 'size_plus', 'size_minus', 'color', 'wrong');
CREATE TYPE t_money_dest AS ENUM ('card', 'wallet', 'account', 'ukrpost');
CREATE TYPE t_parcel_source AS ENUM ('external');
CREATE TYPE t_delivery_service AS ENUM ('tabata', 'ukrposhta', 'novaposhta', 'meest', 'intime', 'kastapost', 'justin');


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
