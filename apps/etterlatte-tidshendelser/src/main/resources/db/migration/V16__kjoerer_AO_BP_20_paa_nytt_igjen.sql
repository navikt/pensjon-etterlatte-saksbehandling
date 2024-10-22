-- Kjører aldersovergang BP20 på nytt etter at de feilet grunnet bug
-- se V173__avbryte_oppgaver_for_feilet_aldersovergang (behandling)
INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_BP20', '2024-10-22', '2024-10');

