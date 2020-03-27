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
