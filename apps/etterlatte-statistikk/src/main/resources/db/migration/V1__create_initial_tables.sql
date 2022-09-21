CREATE TABLE stoenad
(
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
    sakNummer BIGINT,
    tekniskTid TIMESTAMP,
    sakYtelse TEXT,
    versjon TEXT,
    saksbehandler TEXT,
    attestant TEXT,
    vedtakLoependeFom DATE,
    vedtakLoependeTom DATE
);