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

**2026-04-24 — Refaktorering**
- Observation: Jeg committet uten å kompilere, og feilen ble oppdaget av bruker i neste melding.
- Action: Kompiler alltid før commit – ikke anta at koden er riktig fordi den ser logisk ut.

**2026-04-30 — Testskriving**
- Observation: Jeg antok hvilken metode og hvilken app endringen gjaldt uten å spørre. Begge var feil.
- Action: Når brukeren sier "linje X", sjekk linje X i riktig app. Spør om usikkerheten er der. 
