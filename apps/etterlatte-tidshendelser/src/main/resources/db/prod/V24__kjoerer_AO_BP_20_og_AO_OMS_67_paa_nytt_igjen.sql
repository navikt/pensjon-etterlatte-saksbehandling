-- Kjører aldersovergang BP20, BP21 og OMS67 på nytt etter at de feilet
INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_BP20', '2025-04-23', '2025-05');

INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_BP21', '2025-04-23', '2025-05');

INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('AO_OMS67', '2025-04-23', '2025-05');
