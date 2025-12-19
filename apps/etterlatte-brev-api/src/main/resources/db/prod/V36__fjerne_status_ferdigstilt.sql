-- nullstiller et vedtaksbrev som er feil låst til å være ferdigstilt
DELETE
FROM hendelse
WHERE status_id = 'FERDIGSTILT' AND brev_id = 40320;

DELETE
FROM pdf
WHERE brev_id = 40320;