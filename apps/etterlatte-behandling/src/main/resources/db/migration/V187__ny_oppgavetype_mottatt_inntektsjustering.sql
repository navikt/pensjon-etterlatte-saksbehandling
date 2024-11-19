UPDATE oppgave
SET type = 'MOTTATT_INNTEKTSJUSTERING'
WHERE merknad LIKE 'Mottatt inntektsjustering%' AND type = 'GENERELL_OPPGAVE'