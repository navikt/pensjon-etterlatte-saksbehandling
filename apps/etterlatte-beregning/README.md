# etterlatte-beregning

Tjeneste som tilbyr endepunkter for å opprette/oppdatere og hente ut en beregning tilknyttet en behandling

Det som støttes
- Opprette / Oppdatere en beregning
- Hente en beregning
- Slette beregninger tilknyttet en sak ved kafka meldinger fra vedlikeholdsriveren

## Interne repository-operasjoner

`/api/beregning/intern` eksponerer de eksisterende repository-operasjonene mens behandling
kjører forretningslogikken og dataene fortsatt ligger i beregning sin database.
Rutene kaller bare repositories, ikke tjenester eller regelmotoren. Bare systemtoken fra
`etterlatte-behandling` har tilgang. Inbound for behandling finnes allerede i både dev og prod.

Alle stiene under er relative til `/api/beregning/intern`.

| Metode | Sti | Operasjon |
| --- | --- | --- |
| GET | `/beregning/{behandlingId}` | Hent beregning |
| PUT | `/beregning` | Lagre eller oppdater `Beregning` |
| DELETE | `/beregning/{behandlingId}/overstyrt-grunnlag` | Slett overstyrt beregningsgrunnlag |
| GET | `/beregning/overstyring/{sakId}` | Hent aktiv overstyring |
| PUT | `/beregning/overstyring` | Lagre `OverstyrBeregning` |
| DELETE | `/beregning/overstyring/{sakId}` | Deaktiver overstyring |
| GET | `/beregningsgrunnlag/{behandlingId}` | Hent beregningsgrunnlag |
| PUT | `/beregningsgrunnlag` | Lagre `BeregningsGrunnlag`, returner lagringsresultat som boolean |
| GET | `/beregningsgrunnlag/{behandlingId}/overstyrt` | Hent overstyrte grunnlag |
| PUT | `/beregningsgrunnlag/{behandlingId}/overstyrt` | Erstatt overstyrte grunnlag med en liste av `OverstyrBeregningGrunnlagDao` |
| POST | `/anvendt-trygdetid/{behandlingId}` | Lagre anvendt trygdetid, returner antall lagrede rader |
| GET | `/avkorting/{behandlingId}` | Hent full avkorting med historikk |
| PUT | `/avkorting/{behandlingId}` | Lagre avkorting |
| DELETE | `/avkorting/{behandlingId}` | Slett avkorting |
| GET | `/avkorting/faktisk-inntekt/{aarsoppgjoerId}` | Hent faktisk inntekt |
| GET | `/avkorting/sak/{sakId}/aarsoppgjoer` | Hent årsoppgjør for sak |
| GET | `/avkorting/sak/{sakId}/behandlinger` | Hent behandlings-ID-er med årsoppgjør |
| POST | `/avkorting/aarsoppgjoer` | Hent årsoppgjør for en liste av behandlings-ID-er |
| POST | `/avkorting/inntekt-for-aar` | Sjekk inntekt med `InntektsjusteringAvkortingInfoRequest` |
| GET | `/etteroppgjoer/resultat/{forbehandlingId}/{aar}/{sisteIverksatteBehandlingId}` | Hent beregnet etteroppgjørsresultat |
| PUT | `/etteroppgjoer/resultat` | Lagre `BeregnetEtteroppgjoerResultat` |
| GET | `/sanksjon/behandling/{behandlingId}` | Hent sanksjoner for behandling |
| POST | `/sanksjon/behandling/{behandlingId}` | Opprett sanksjon |
| POST | `/sanksjon/behandling/{behandlingId}/kopi` | Opprett sanksjon fra kopi |
| PUT | `/sanksjon` | Oppdater sanksjon |
| GET | `/sanksjon/{sanksjonId}` | Hent sanksjon |
| DELETE | `/sanksjon/{sanksjonId}` | Slett sanksjon, returner antall slettede rader |

Forespørsler med flere argumenter bruker dataklassene i `intern/BeregningCrudRequests.kt`.
Anvendt trygdetid bruker konkrete typer for `anvendt`, perioder og regelversjon, med samme
JSON-struktur som `AnvendtTrygdetidPeriodeUtrekning`. Bare regeltrærne i `utrekning[].resultat`
bruker `JsonNode` og lagres uendret uten å rekonstruere kjørbare regler.
Avkorting bruker hele repository-modellen, ikke den flate `AvkortingDto`.

Manglende enkeltressurser gir 404. Lister kan være tomme. Oppretting gir 201, skriving uten
returverdi gir 204, og øvrige vellykkede kall gir 200. Lagring av en inaktiv overstyring gir 204.

## Kom i gang
Applikasjonen tilbyr et REST-api. I bakkant ligger en PostgreSQL-database som lager, oppdaterer, henter og sletter 
beregninger tilknyttet en behandling.

### Hvordan kjøre lokalt mot dev-gcp 

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
BEREGNING_API_URL=http://host.docker.internal:8089
```

### Kjøre lokalt med mock auth

1. Start Mock-OAuth2-Server og Postgres lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
```
AZURE_APP_WELL_KNOWN_URL=http://localhost:8082/azure/.well-known/openid-configuration;
AZURE_APP_CLIENT_ID=clientId;
DB_JDBC_URL=jdbc:postgresql://localhost:5432/postgres;
DB_USERNAME=postgres;
DB_PASSWORD=postgres;
HTTP_PORT=8087;
```

### Teste mot REST-endepunkter

#### Hente token
```
curl --location --request POST 'http://localhost:8082/azure/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_id=clientId' \
--data-urlencode 'client_secret=not_so_secret' \
--data-urlencode 'scope=clientId'
```

#### Kjøre request
- Url: `http://localhost:8080/api/beregning`
- Header: `Authorization: Bearer $token`
