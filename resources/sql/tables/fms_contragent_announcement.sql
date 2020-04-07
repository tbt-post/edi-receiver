CREATE TABLE IF NOT EXISTS fms_contragent_announcement (
  sender text NOT NULL,
  ts timestamp with time zone NOT NULL,
  contragent_id uuid NOT NULL,
  contragent_reg_id text NOT NULL,
  action text NOT NULL,
  reason text NOT NULL,
  merchant_id text,
  comment text
);
