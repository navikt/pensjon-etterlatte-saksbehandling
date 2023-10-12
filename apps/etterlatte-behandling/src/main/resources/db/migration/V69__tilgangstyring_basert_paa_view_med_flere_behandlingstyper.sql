CREATE VIEW alle_behandlinger AS
select id, sak_id
from behandling
UNION
select id, sak_id
from tilbakekreving;