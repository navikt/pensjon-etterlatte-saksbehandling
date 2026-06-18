
update behandling set status = 'IVERKSATT'
    where status = 'ATTESTERT'
    and id in (
'2e600eaa-7c5e-45ec-b225-6ee773d69462',
'd5183d02-4145-462f-8e47-9f595aa33c34',
'88f3283f-f97d-4f04-b81b-3d1f99d7ec0d',
'017d8a40-243b-4d96-ae85-dd29aae35f73');

update vedtak
set vedtakstatus = 'IVERKSATT',
    datoiverksatt = datoattestert::timestamptz + interval '1 second'
where behandlingid in (
    '2e600eaa-7c5e-45ec-b225-6ee773d69462',
    'd5183d02-4145-462f-8e47-9f595aa33c34',
    '88f3283f-f97d-4f04-b81b-3d1f99d7ec0d',
    '017d8a40-243b-4d96-ae85-dd29aae35f73');

