UPDATE behandling
SET revurdering_aarsak = 'SLUTTBEHANDLING'
WHERE revurdering_aarsak = 'SLUTTBEHANDLING_UTLAND';

ALTER TABLE behandling_info
RENAME COLUMN omgjoering_sluttbehandling_utland TO omgjoering_sluttbehandling;