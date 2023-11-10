# etterlatte-hendelser-samordning

Applikasjonen lytter på hendelser fra SAM sin Kafka-topic `pensjonsamhandling.sam-vedtak-samhandlersvar-<p|q2>`.
Denne inneholder hendelser for vedtak som er til samordning, men nå har mottatt svar fra aktuelle TP-ordninger, 
eller har gått over ventetiden, og dermed er ferdig samordnet.

Topic'en inneholder hendelser for alle vedtak som samordnes via SAM, dvs Gjenny OG Pesys. 
Gjeldende app skal kun håndtere hendelser med fagsystemkode **EYO**.

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-hendelser-samordning/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
