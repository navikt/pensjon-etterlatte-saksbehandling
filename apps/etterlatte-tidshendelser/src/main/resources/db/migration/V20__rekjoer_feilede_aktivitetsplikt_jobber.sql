-- Kjører de som ikke ble kjørt på grunn av feil 2025-01-01 på nytt 2025-01-02
UPDATE jobb
SET kjoeredato = '2025-01-02'
WHERE status = 'NY'
  AND kjoeredato = '2025-01-01';

-- Legger inn kjøringen for OMS doed 10 måneder også for januar
INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
VALUES ('OMS_DOED_10MND', '2025-01-02', '2024-12');