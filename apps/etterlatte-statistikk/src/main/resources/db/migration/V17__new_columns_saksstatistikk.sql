alter table sak add column sak_ytelsesgruppe TEXT;
update sak set sak_ytelsesgruppe = 'EN_AVDOED_FORELDER' where sak_ytelse = 'BARNEPENSJON';

alter table sak add column avdoede_foreldre JSONB;
update sak sak
    set avdoede_foreldre = sto.fnrForeldre
FROM (SELECT fnrForeldre, behandlingId FROM stoenad) as sto
WHERE sto.behandlingId = sak.behandling_id;

alter table sak add column revurdering_aarsak TEXT;
-- dette stemmer ikke for alle saker i test, men stemmer for data i prod
update sak set revurdering_aarsak = 'REGULERING' where behandling_type = 'REVURDERING';

-- fiksing av kodeverk
update sak set behandling_resultat = 'INNVILGELSE' where behandling_resultat = 'VEDTAK';
update sak set behandling_metode = 'AUTOMATISK_REGULERING' where behandling_type = 'REVURDERING' and behandling_metode = 'AUTOMATISK';
update sak set behandling_metode = 'TOTRINN' where behandling_metode = 'MANUELL';

-- refresher view på tabellen for å få med nye kolonner
CREATE OR REPLACE VIEW sak_statistikk AS
SELECT * FROM sak
WHERE tidspunkt_registrert > NOW() - interval '2 days';