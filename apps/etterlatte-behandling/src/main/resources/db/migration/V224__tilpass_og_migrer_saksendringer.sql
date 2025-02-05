-- Tar backup av endringer før den endres og migreres
CREATE TABLE endringer_backup AS TABLE endringer;

ALTER TABLE endringer RENAME TO saksendring;

ALTER TABLE saksendring DROP COLUMN tabell;
ALTER TABLE saksendring RENAME saksbehandler TO ident;
ALTER TABLE saksendring
    ADD COLUMN sak_id BIGINT;
ALTER TABLE saksendring
    ADD COLUMN endringstype TEXT;
ALTER TABLE saksendring
    ADD COLUMN identtype TEXT;

UPDATE saksendring
SET endringstype =
        CASE
            WHEN kallendemetode = 'HikariProxyConnection: opprettSak' THEN 'OPPRETT_SAK'
            WHEN kallendemetode = 'HikariProxyConnection: markerSakerMedSkjerming' THEN 'ENDRE_SKJERMING'
            WHEN kallendemetode = 'HikariProxyConnection: oppdaterIdent' THEN 'ENDRE_IDENT'
            WHEN kallendemetode = 'HikariProxyConnection: oppdaterEnheterPaaSaker' THEN 'ENDRE_ENHET'
            WHEN kallendemetode = 'HikariProxyConnection: oppdaterAdresseBeskyttelse' THEN 'ENDRE_ADRESSEBESKYTTELSE'
            END;

UPDATE saksendring
SET identtype =
        CASE
            WHEN ident ~ '^[A-Z][0-9]{6}$' THEN 'SAKSBEHANDLER'
            ELSE 'GJENNY'
            END;

UPDATE saksendring
SET sak_id = CAST(etter ->> 'id' AS BIGINT);

ALTER TABLE saksendring
    ALTER COLUMN endringstype SET NOT NULL;
ALTER TABLE saksendring
    ALTER COLUMN identtype SET NOT NULL;
ALTER TABLE saksendring
    ALTER COLUMN sak_id SET NOT NULL;

ALTER TABLE saksendring
    DROP COLUMN kallendemetode;