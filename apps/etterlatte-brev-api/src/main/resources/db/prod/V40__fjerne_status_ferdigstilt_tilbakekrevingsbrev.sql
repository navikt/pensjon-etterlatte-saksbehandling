-- nullstiller et vedtaksbrev som er feil låst til å være ferdigstilt
DELETE
FROM hendelse
WHERE status_id = 'FERDIGSTILT' AND brev_id = 40163 AND (select brevkoder from brev where brev_id = 40163) = 'TILBAKEKREVING';
DELETE
FROM hendelse
WHERE status_id = 'FERDIGSTILT' AND brev_id = 45642 AND (select brevkoder from brev where brev_id = 45642) = 'TILBAKEKREVING';

DELETE
FROM pdf
WHERE brev_id = 40163;
DELETE
FROM pdf
WHERE brev_id = 45642;
