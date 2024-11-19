UPDATE oppgave
set type = 'MOTTATT_INNTEKTSJUSTERING'
WHERE merknad LIKE 'Mottatt inntektsjustering%' AND type = 'GENERELL_OPPGAVE'