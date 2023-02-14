-- Konvertere behandlingsId til UUID, som resten av vedtaksløsningen
alter table brev
    alter column behandling_id type uuid using behandling_id::uuid;


-- Slå sammen fornavn og etternavn til ett felt
alter table mottaker
    rename column fornavn to navn;
alter table mottaker
    drop column etternavn;


-- Legge til søker sitt fødselsnummer for å gjøre oppslag enklere
alter table brev
    add soeker_fnr text not null default '';
alter table brev
    alter column soeker_fnr drop default;
