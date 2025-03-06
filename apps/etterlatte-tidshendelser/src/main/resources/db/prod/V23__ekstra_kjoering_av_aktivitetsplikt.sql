-- Legger inn en kjøring av infobrevoppgavene som burde ha blitt kjørt tidligere men ble uheldigvis hoppet over
-- (med behandlingsmaaned 2025-01 så treffer denne de med dødsdato avdød 2024-03, så kravet løper fra neste
-- måned allerede)
insert into jobb (type, kjoeredato, behandlingsmaaned)
VALUES ('OMS_DOED_10MND', '2025-03-06', '2025-01');