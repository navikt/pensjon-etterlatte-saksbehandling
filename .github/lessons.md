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
- Observation: Edits fjerner viktig innhold – enten ved å ikke se nøye på `old_str`, eller ved å bruke et for lite ankerpunkt slik at resten av signaturen forsvinner.
- Action: Ved edits: ta med nok kontekst i `old_str` til å være unik, og les den nøye for å sikre at alt som ikke skal bort er med i `new_str`.

**2026-04-10 — Dokumentasjon med usikkerhet**
- Observation: Jeg la inn "ikke referert i kodebasen" som plassholder i stedet for å bare spørre. Det er mer respektfullt og presist å stille spørsmålet direkte.
- Action: Når en verdi ikke kan verifiseres, spør – ikke dokumenter usikkerheten som et faktum.

**2026-04-24 — Refaktorering**
- Observation: Jeg committet uten å kompilere, og feilen ble oppdaget av bruker i neste melding.
- Action: Kompiler alltid før commit – ikke anta at koden er riktig fordi den ser logisk ut.

**2026-04-30 — OTel / bibliotekbruk**
- Observation: Jeg antok at en API-verdi kunne settes og tilbakestilles symmetrisk, uten å sjekke hvilke tilstandsoverganger som faktisk er lovlige.
- Action: Sjekk tillatte tilstandsoverganger i et bibliotek før du bruker det – ikke anta at alle verdier er likestilte.

**2026-04-30 — Testskriving**
- Observation: Jeg antok hvilken metode og hvilken app endringen gjaldt uten å spørre. Begge var feil.
- Action: Når brukeren sier "linje X", sjekk linje X i riktig app. Spør om usikkerheten er der.

**2026-05-04 — Testskriving**
- Observation: Jeg la inn mock for `SaksbehandlerInfoDao` uten å vurdere om real-DB var mulig og bedre. Brukeren måtte eksplisitt be om det.
- Action: Når en DAO er enkel og DatabaseExtension finnes i modulen, vurder real-DB som default – ikke mock.

**2026-05-07 — Regelendring / beregning**
- Observation: Før jeg endret reglene sjekket jeg OMS-koden for å se hvordan de løste det samme problemet – det gav meg korrekt mønster direkte (`multiply(grunnbeloep).divide(12)`), og samsvarte med planens antagelser.
- Action: Ved beregningsregelendringer, alltid finn den tilsvarende regelen i det andre regelsettet (BP/OMS) som referanseimplementasjon.

**2026-05-29 — Estimering**
- Observation: Jeg antok at en mock var "bare bekvemmelighet" og estimerte fjerning som trivielt – uten å verifisere det.
- Action: Anta at en mock er en del av testkontrakten til det motsatte er bevist. Verifiser antagelsen med en rask sjekk før du estimerer.

**2026-06-03 — Statussmaskin / fikse logikk**
- Observation: Jeg byttet til en status-basert løsning fordi DAO-tilnærmingen hadde en RETURNERT-bug, men den status-baserte løsningen fikset ikke den faktiske buggen (TRYGDETID_OPPDATERT rutes til tilFattetVedtakUtvidet som tillater TRYGDETID_OPPDATERT). Brukeren måtte si ifra.
- Action: Når en bug handler om at en status _urettmessig_ passerer en sperre, sjekk at den nye løsningen faktisk _sperrer_ den statusen i det problematiske tilfellet — ikke bare at koden ser logisk ut.
