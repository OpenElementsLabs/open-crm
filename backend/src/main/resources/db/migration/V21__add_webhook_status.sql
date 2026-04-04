ALTER TABLE webhooks ADD COLUMN last_status INTEGER;
ALTER TABLE webhooks ADD COLUMN last_called_at TIMESTAMP;
