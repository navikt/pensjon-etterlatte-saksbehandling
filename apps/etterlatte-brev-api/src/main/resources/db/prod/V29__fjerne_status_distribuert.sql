/*
Sletter status DISTRIBUERT. De har fått en bestilling_id og status DISTRIBUERT, men ble aldri
sendt pga. bug i dokdist. Se: EY-4955
*/
DELETE
FROM hendelse
WHERE status_id = 'DISTRIBUERT'
  AND brev_id IN (29229, 29376, 29396, 29399);
