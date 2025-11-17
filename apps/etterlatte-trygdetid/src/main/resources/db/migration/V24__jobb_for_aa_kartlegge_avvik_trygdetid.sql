create table trygdetid_avvik (
    trygdetid_id uuid,
    behandling_id uuid,
    sak_id bigint,
    status text default 'IKKE_SJEKKET',
    avvik jsonb default null
);

insert into trygdetid_avvik (trygdetid_id, behandling_id, sak_id)
select id, behandling_id, sak_id from trygdetid;