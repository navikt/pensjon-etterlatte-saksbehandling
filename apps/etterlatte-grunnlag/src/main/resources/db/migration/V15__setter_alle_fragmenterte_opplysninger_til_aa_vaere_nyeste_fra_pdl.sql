-- Setter opp et midlertidig view som har det nyeste PDL-dokumentet per person per sak, til bruk i utpakking av opplysninger
CREATE TEMPORARY VIEW pdl_latest AS (select max(hendelsenummer) as siste_hendelse, sak_id, fnr, opplysning::jsonb as opplysning
                                    from grunnlagshendelse
                                    where opplysning_type in ('SOEKER_PDL_V1', 'GJENLEVENDE_FORELDER_PDL_V1', 'AVDOED_PDL_V1')
                                    group by sak_id, fnr, opplysning);

-- BOSTEDSADRESSE
UPDATE grunnlagshendelse g
SET opplysning = pdl_latest.opplysning ->> 'bostedsadresse'
FROM pdl_latest
WHERE g.opplysning_type = 'BOSTEDSADRESSE'
  and g.sak_id = pdl_latest.sak_id
  and g.fnr = pdl_latest.fnr;

-- DELTBOSTEDSADRESSE
UPDATE grunnlagshendelse g
SET opplysning = pdl_latest.opplysning ->> 'deltBostedsadresse'
FROM pdl_latest
WHERE g.opplysning_type = 'DELTBOSTEDSADRESSE'
  and g.sak_id = pdl_latest.sak_id
  and g.fnr = pdl_latest.fnr;

-- KONTAKTADRESSE
UPDATE grunnlagshendelse g
SET opplysning = pdl_latest.opplysning ->> 'kontaktadresse'
FROM pdl_latest
WHERE g.opplysning_type = 'KONTAKTADRESSE'
  and g.sak_id = pdl_latest.sak_id
  and g.fnr = pdl_latest.fnr;

-- OPPHOLDSADRESSE
UPDATE grunnlagshendelse g
SET opplysning = pdl_latest.opplysning ->> 'oppholdsadresse'
FROM pdl_latest
WHERE g.opplysning_type = 'OPPHOLDSADRESSE'
  and g.sak_id = pdl_latest.sak_id
  and g.fnr = pdl_latest.fnr;

-- VERGEMAALELLERFREMTIDSFULLMAKT
UPDATE grunnlagshendelse g
SET opplysning = pdl_latest.opplysning ->> 'vergemaalEllerFremtidsfullmakt'
FROM pdl_latest
WHERE g.opplysning_type = 'VERGEMAALELLERFREMTIDSFULLMAKT'
  and g.sak_id = pdl_latest.sak_id
  and g.fnr = pdl_latest.fnr;

-- SIVILSTAND
UPDATE grunnlagshendelse g
SET opplysning = pdl_latest.opplysning ->> 'sivilstand'
FROM pdl_latest
WHERE g.opplysning_type = 'SIVILSTAND'
  and g.sak_id = pdl_latest.sak_id
  and g.fnr = pdl_latest.fnr;
