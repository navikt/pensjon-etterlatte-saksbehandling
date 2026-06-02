
update behandling set status = 'IVERKSATT'
    where status = 'ATTESTERT'
    and id in (
'67db5c28-63bc-44da-aa10-7d780bf07a41',
'7415d64d-f65f-4e3a-a74a-e45401dc684b',
'e9a3da85-8a99-4cf4-8040-0ae665199f0d',
'5393460e-b56b-4406-8a14-83e8375f0110',
'076d1819-c275-4bdc-8455-25111d374dc0',
'fac79f31-97b6-478e-bc6f-d35aeeec926a',
'1e354842-7837-4569-80a4-afe153524e8a',
'6d102e79-dfc6-4d36-aeb8-2cbaa13571e5',
'ead0f1b5-cd4b-4cf8-97ad-662f74c925a7',
'510c0015-ae3e-4879-8278-807b0eb4d8fd',
'd3b0983f-5d23-4808-aa98-dc34f1dd5905',
'e006cd3a-88e0-4f41-abc7-c639a8add364');

update vedtak
set vedtakstatus = 'IVERKSATT'
where behandlingid in (
                       '67db5c28-63bc-44da-aa10-7d780bf07a41',
                       '7415d64d-f65f-4e3a-a74a-e45401dc684b',
                       'e9a3da85-8a99-4cf4-8040-0ae665199f0d',
                       '5393460e-b56b-4406-8a14-83e8375f0110',
                       '076d1819-c275-4bdc-8455-25111d374dc0',
                       'fac79f31-97b6-478e-bc6f-d35aeeec926a',
                       '1e354842-7837-4569-80a4-afe153524e8a',
                       '6d102e79-dfc6-4d36-aeb8-2cbaa13571e5',
                       'ead0f1b5-cd4b-4cf8-97ad-662f74c925a7',
                       '510c0015-ae3e-4879-8278-807b0eb4d8fd',
                       'd3b0983f-5d23-4808-aa98-dc34f1dd5905',
                       'e006cd3a-88e0-4f41-abc7-c639a8add364');


INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46764, '67db5c28-63bc-44da-aa10-7d780bf07a41'::UUID, 6390, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46765, '7415d64d-f65f-4e3a-a74a-e45401dc684b'::UUID, 6391, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46766, 'e9a3da85-8a99-4cf4-8040-0ae665199f0d'::UUID, 6395, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46767, '5393460e-b56b-4406-8a14-83e8375f0110'::UUID, 6396, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46768, '076d1819-c275-4bdc-8455-25111d374dc0'::UUID, 6398, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46769, 'fac79f31-97b6-478e-bc6f-d35aeeec926a'::UUID, 6399, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46973, '1e354842-7837-4569-80a4-afe153524e8a'::UUID, 6625, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46974, '6d102e79-dfc6-4d36-aeb8-2cbaa13571e5'::UUID, 6626, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46975, 'ead0f1b5-cd4b-4cf8-97ad-662f74c925a7'::UUID, 6627, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46976, '510c0015-ae3e-4879-8278-807b0eb4d8fd'::UUID, 6628, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 46977, 'd3b0983f-5d23-4808-aa98-dc34f1dd5905'::UUID, 6629, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse) VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 52739, 'e006cd3a-88e0-4f41-abc7-c639a8add364'::UUID, 12995, NULL, NULL, 'Korrigerer status etter feil i G-regulering', NULL);
