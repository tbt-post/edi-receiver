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
