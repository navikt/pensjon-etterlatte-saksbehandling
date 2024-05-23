-- Jobben oppretter oppgave for å vurdere aktivitetsplikt etter 4 måneder for sakene der dødsdato var i desember 2023.
-- Enda en gang siden toggle var skrudd av i produksjon :(
INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
values ('OMS_DOED_4MND', '2024-05-21', '2024-04')