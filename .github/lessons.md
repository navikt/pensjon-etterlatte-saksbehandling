**2026-04-16 — Refactoring**
- Observation: Jeg refaktorerte rundt en nullable parameter (`hendelse: EtteroppgjoerHendelser?`) uten å spørre om selve parametersignaturen var riktig. Nullable i funksjonsparameter er et direkte signal om at null-sjekken tilhører kallstedet – det burde vært åpenbart.
- Action: Når jeg sender nullable inn i en funksjon som et resultat av min egen logikk, stopp og vurder om funksjonen i stedet skal ta en ikke-nullable parameter og la kallstedet håndtere guards.

**2026-04-09 — Kommunikasjon**
- Observation: Lange svar uten oppsummering gjør at essensen drukner.
- Action: Avslutt alltid lange svar med en TL;DR – 2–4 linjer, cave-man stil.

**2026-04-10 — Metaoppgaver**
- Observation: Lessons.md ble ikke oppdatert underveis fordi planmodus absorberte all oppmerksomhet, og meta-oppgaver ble behandlet som cleanup, ikke inline-forpliktelse.
- Action: Lessons-oppdatering er ikke en post-task jobb – den skal skje i samme svar som kursjusteringen, også under planlegging.

**2026-04-10 — Datamodellendringer**
- Observation: Feil plassering av data og feil scope-reduksjon skyldes begge at lag ble vurdert isolert, ikke som del av en sammenhengende pipeline.
- Action: Før enhver datamodellendring – forstå den semantiske kontrakten til hvert lag (hva representerer det, hvem leser det, hvordan brukes det), og spor hele flyten fra kilde til konsument.

**2026-04-10 — Sparring / løsningsdesign**
- Observation: Løsninger ble foreslått før eksisterende sperrer/constraints var kartlagt, og scope vokste uten eksplisitt avklaring.
- Action: Før første løsningsskisse – les koden, kartlegg constraints, avklar om systemet eller saksbehandler skal eie løsningen, og spør om scope.

**2026-04-10 — Redigering**
- Observation: Jeg fjerner viktig innhold når jeg gjør målrettede edits – ser ikke nøye nok på hva som forsvinner.
- Action: Ved edits: les old_str nøye, sjekk at ny tekst bevarer alt som ikke eksplisitt skal bort.

**2026-04-10 — Dokumentasjon med usikkerhet**
- Observation: Jeg la inn "ikke referert i kodebasen" som plassholder i stedet for å bare spørre. Det er mer respektfullt og presist å stille spørsmålet direkte.
- Action: Når en verdi ikke kan verifiseres, spør – ikke dokumenter usikkerheten som et faktum.

**2026-04-15 — Refactoring**
- Observation: `runCatching { }.getOrNull()` og unødvendig `runBlocking`-wrapping gjentok seg i begge job-service-filer. Første gang fikset jeg det via ny nullable-metode på service, men oppdaget ikke at det samme mønsteret fantes i den andre filen.
- Action: Etter en mønsterbasert fix – søk aktivt etter samme anti-mønster i relaterte filer i samme pakke.
