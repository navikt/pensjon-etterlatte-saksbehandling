UPDATE oppgave
SET type = 'MANGLER_SOEKNAD', kilde = 'DOEDSHENDELSE'
WHERE type = 'VURDER_KONSEKVENS'
  AND merknad LIKE '%ikke søkt%';

