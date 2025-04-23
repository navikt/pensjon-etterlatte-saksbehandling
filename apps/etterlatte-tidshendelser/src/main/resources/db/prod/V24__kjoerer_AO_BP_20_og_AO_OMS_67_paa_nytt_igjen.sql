-- Kjører aldersovergang BP20 på nytt etter at de feilet grunnet bug
-- se V173__avbryte_oppgaver_for_feilet_aldersovergang (behandling)
INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_BP20', '2025-04-23', '2025-05');

INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_BP21', '2025-04-23', '2025-05');

INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_OMS67', '2025-04-23', '2025-05');
