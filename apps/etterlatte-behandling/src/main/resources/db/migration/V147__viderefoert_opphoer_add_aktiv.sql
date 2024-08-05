ALTER TABLE viderefoert_opphoer
ADD COLUMN aktiv BOOL NOT NULL DEFAULT true;

ALTER TABLE viderefoert_opphoer DROP CONSTRAINT viderefoert_opphoer_behandling_id_key;

CREATE UNIQUE INDEX viderefoert_opphoer_en_aktiv ON viderefoert_opphoer (behandling_id) where aktiv = true;
