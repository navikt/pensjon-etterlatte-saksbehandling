-- Fikser feil merknad på oppgaver om aktivitetsplikt 6 måneder (ble opprettet med 12 måneder som merknad)
-- sak_id'ene er hentet fra tidshendelser for kjøringen av OMS_DOED_6_MND-jobben i januar.
update oppgave
set merknad = 'Vurdering av aktivitetsplikt ved 6 måneder'
where sak_id in
      (18350, 18404, 18419, 18429, 18438, 18335, 18922, 18497, 18519, 18564, 18561, 18590, 18910, 18596, 18049, 18666,
       18212, 18688, 18262, 18271, 18281, 18961, 18984, 19166, 19343, 19396, 20223, 20275)
  and merknad = 'Vurdering av aktivitetsplikt ved 12 måneder';
