-- Konvertere behandlingsId til UUID, som resten av vedtaksløsningen
alter table brev
    alter column behandling_id type uuid using behandling_id::uuid;
