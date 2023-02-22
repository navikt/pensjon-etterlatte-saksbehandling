ALTER TABLE stoenad ADD COLUMN vedtakType TEXT;

CREATE TABLE maaned_stoenad(
   id BIGSERIAL PRIMARY KEY,
   fnrSoeker TEXT,
   fnrForeldre JSONB,
   fnrSoesken JSONB,
   anvendtTrygdetid TEXT,
   nettoYtelse TEXT,
   beregningType TEXT,
   anvendtSats TEXT,
   behandlingId UUID,
   sakId BIGINT,
   tekniskTid TIMESTAMP,
   sakYtelse TEXT,
   versjon TEXT,
   saksbehandler TEXT,
   attestant TEXT,
   vedtakLoependeFom DATE,
   vedtakLoependeTom DATE,
   statistikkMaaned TEXT,
   registrertTimestamp TIMESTAMP DEFAULT NOW()
);

-- View for forrige maanedes statistikk for import (vi vil kun produsere etter maaneden er ferdig)
CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS SELECT * FROM maaned_stoenad
     WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());
