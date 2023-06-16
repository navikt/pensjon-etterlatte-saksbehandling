select
    b.id as behandling_id, k.svar as svar, k.begrunnelse as begrunnelse, k.kilde as kilde
into table kommerbarnettilgode
from
    behandling b,
    lateral jsonb_to_record(kommer_barnet_tilgode::jsonb) as k(svar varchar, begrunnelse varchar, kilde varchar)
where kommer_barnet_tilgode is not null;

ALTER TABLE kommerbarnettilgode ADD CONSTRAINT kommerbarnettilgode_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id);