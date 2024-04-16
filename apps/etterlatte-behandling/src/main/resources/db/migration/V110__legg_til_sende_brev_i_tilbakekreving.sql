ALTER TABLE tilbakekreving ADD COLUMN sende_brev bool NOT NULL DEFAULT TRUE;

UPDATE tilbakekreving SET sende_brev = true;