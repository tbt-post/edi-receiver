CREATE TABLE IF NOT EXISTS event_parcel_order_return (
  sender text NOT NULL,
  ts timestamp with time zone NOT NULL,
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

CREATE TABLE IF NOT EXISTS event_parcel_change_state (
  sender text NOT NULL,
  ts timestamp with time zone NOT NULL,
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
