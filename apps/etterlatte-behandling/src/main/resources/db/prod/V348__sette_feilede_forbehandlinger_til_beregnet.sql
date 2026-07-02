
UPDATE etteroppgjoer_behandling
SET status = 'BEREGNET'
WHERE id::text IN (SELECT relatert_behandling
                   FROM behandling
                   WHERE id IN (
                                'd95be9fc-3acb-462b-96a6-91bac22d2fea',
                                '145cb975-c6b9-4138-9fd9-b59a359a6065'));
