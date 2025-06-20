-- Oppdaget i forbindelse med regulering: 2 behandlinger er iverksatt i vedtaksvurdering og har kvittering i utbetaling,
-- men står fremdeles som attestert i behandling. Oppdaterer disse to behandlingene slik at de kan bli regulert riktig
UPDATE behandling
SET status = 'IVERKSATT'
WHERE id IN ('721b80a0-7c16-47dc-92bd-2aa85f712519', 'e9ed2b4d-e821-4dfa-9a1a-3bbacb20f0c9')
  AND status = 'ATTESTERT';