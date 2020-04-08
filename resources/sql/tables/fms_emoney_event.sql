CREATE TABLE IF NOT EXISTS fms_emoney_event (
  sender text NOT NULL,
  ts timestamp with time zone NOT NULL,
  serial uuid NOT NULL,
  money_type text NOT NULL,
  account text NOT NULL,
  emoney_transaction text NOT NULL,
  transaction_id uuid NOT NULL,
  emoney_amount numeric(10,2) NOT NULL,
  direction text NOT NULL,
  action text NOT NULL,
  reason text NOT NULL,
  contragent_id uuid,
  contragent_reg_id text,
  merchant_id text,
  note text,
  source_wallet text,
  target_wallet text
);
