INSERT INTO utbetalingshendelse(id, utbetaling_id, tidspunkt, status)
VALUES ('59c2163b-22ae-464b-bd63-40e9afa7dabf', '1d7d791b-fce8-4d8c-ae49-34904813ee56', now(), 'GODKJENT')

UPDATE utbetaling
SET kvittering = replace(oppdrag, '<oppdrag-110>', '<mmel>
        <systemId>231-OPPD</systemId>
        <alvorlighetsgrad>00</alvorlighetsgrad>
    </mmel>
    <oppdrag-110>'),
    kvittering_beskrivelse      = NULL,
    kvittering_alvorlighetsgrad = '00',
    kvittering_kode             = NULL,
    endret                      = now()
WHERE sak_id = 12995
  AND id = '1d7d791b-fce8-4d8c-ae49-34904813ee56';
