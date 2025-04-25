-- Tilgjengelig for nye registreringer, null for eksisterende
alter table sak add column sak_utland_enhet TEXT;

-- Oppdater eksisterende
update sak set sak_utland_enhet = case ansvarlig_enhet
    when '0001' then 'BOSATT_UTLAND'
    when '4862' then 'UTLANDSTILSNITT'
    else 'NASJONAL'
end where sak_utland_enhet is null;

-- refresher view på tabellen for å få med ny kolonne
CREATE OR REPLACE VIEW sak_statistikk AS
SELECT * FROM sak
WHERE tidspunkt_registrert > NOW() - interval '2 days';
