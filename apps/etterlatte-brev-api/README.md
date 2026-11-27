# etterlatte-brev-api

Ktor og Rapid app for å håndtere generering av brev, brevmaler og sende videre til distribusjon.

Brev genereres ved hjelp av [Pensjonbrev (Brevbakeren)](https://github.com/navikt/pensjonsbrev).

## Kom i gang

### Hvordan kjøre lokalt

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
BREV_API_URL=http://host.docker.internal:8084
```

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-brev-api/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Sakstilknyttede batchbrev

Behandling bygger brevdata og eier batchens fremdrift. Brev-api eier brevet, PDF, journalføring og distribusjon.
Se [batch-API og driftsrutiner](../etterlatte-behandling/README.md#masseutsending-av-informasjonsbrev).

| Metode og sti | Resultat |
| --- | --- |
| `POST /api/brev/strukturert/sak/{sakId}/utsending/{utsendingsId}` | Opprett eller hent samme brev fra uforanderlige strukturerte data |
| `GET /api/brev/strukturert/sak/{sakId}/utsending/{utsendingsId}` | Utsendingsstatus, eller `204 No Content` hvis utsendingen ikke finnes |
| `POST /api/brev/strukturert/sak/{sakId}/utsending/{utsendingsId}/send` | Generer PDF og utfør eller gjenoppta trygge utsendingssteg |

Routene krever en eksplisitt tillatt systembruker fra behandling.
Sett `BREV_BATCH_TILLATT_BEHANDLING` til behandling-klientens validerte `azp_name`.
Tom verdi avviser alle klienter. `BREV_BATCH_AKTIVERT=false` er standard og hindrer opprettelse og sending.
Statusoppslag for en tillatt klient er fortsatt tilgjengelig ved stopp.

Batchbrevene har ingen behandling-ID. Utsendings-ID-en er en separat nøkkel.
Samme utsendings-ID og data gir samme brev; endrede data gir `409 Conflict`.
Eksisterende redigerings- og utsendingsruter kan ikke omgå batchflytens innhold og steg.

Den generiske malen mottar tittel og Slate via `ManueltBrevMedTittelData`, valgt språk og automatisk Nav-avsender (`EY`).
Bekreft faktisk PDF-rendering og mottakerregler i dev før produksjonsaktivering.
Påbegynte eksterne steg lagres før journalføring og distribusjon. Et avbrudd kan gi `AVVENTER_AVKLARING`.
Nye eller samtidige `send`-kall gjentar ikke et uavklart eksternt steg. Operatøren må undersøke Joark og Brevdistribusjon før retting.
