CREATE TABLE IF NOT EXISTS order_payment (
  sender text NOT NULL,
  ts timestamp with time zone NOT NULL,
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
