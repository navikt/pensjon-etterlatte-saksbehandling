DELETE
FROM hendelse
WHERE status_id = 'FERDIGSTILT' AND brev_id = 31975;

DELETE
FROM pdf
WHERE brev_id = 31975;