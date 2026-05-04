# Eksterne repos

Team Etterlatte har tre repoer. Dette er dokumentasjon om de to andre repoene og hvordan de henger sammen med `pensjon-etterlatte-saksbehandling` (Gjenny).

---

## pensjon-etterlatte

**Rolle:** Søknadsdialog og selvbetjening for sluttbrukere (BP og OMS).

### Apper

| App | Beskrivelse |
|-----|-------------|
| `barnepensjon-ui` | React-søknadsdialog for barnepensjon |
| `omstillingsstoenad-ui` | React-søknadsdialog for omstillingsstønad |
| `selvbetjening-ui` | Felles selvbetjeningsgrensesnitt (React) |
| `innsendt-soeknad` | Backend for søknadsdialogene – lagrer søknader, henter data fra PDL og kodeverk, publiserer til Kafka |
| `selvbetjening-backend` | Backend for selvbetjening – lar brukere sjekke sak og melde inn endringer (OMS) |
| `etterlatte-node-server` | Felles Node/Express BFF for søknadsdialogene |

### Integrasjon mot Gjenny

#### Søknadsflyt (innsendt-soeknad → Gjenny)

`innsendt-soeknad` publiserer to events på Kafka-rapidsen. Topic heter `etterlatte.etterlatteytelser` (prod-navn) – dev-topic heter fortsatt `etterlatte.dodsmelding` av historiske årsaker, men det er samme rapid i begge miljøer:

| Event | Konstant | Håndteres av |
|-------|----------|--------------|
| `soeknad_innsendt` | `SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT` | `NySoeknadRiver` i `etterlatte-behandling-kafka` |
| `trenger_behandling` | `SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV` | `OpprettBehandlingRiver` i `etterlatte-behandling-kafka` |

Gjenny-siden er definert i `libs/saksbehandling-common/.../event/SoeknadInnsendt.kt`.

Meldingsfelter på Kafka-pakken:

```
@event_name, @skjema_info, @lagret_soeknad_id, @template, @fnr_soeker, @hendelse_gyldig_til
```

#### Selvbetjening (selvbetjening-backend → Gjenny)

`selvbetjening-backend` kaller `etterlatte-api` (ikke saksbehandlings-BFF-en) for å sjekke sak:

- `POST /api/sak/oms/har_sak` – om bruker har OMS-sak
- `POST /api/sak/oms/har-loepende-sak` – om bruker har løpende OMS-sak

#### OMS – meld inn endring

`selvbetjening-backend` lar OMS-brukere melde inn endringer (inntekt, aktivitet, etteroppgjør). Endringene lagres lokalt og publiseres til Gjennys rapid via en periodisk jobb (`PubliserOmsMeldtInnEndringJobb`). Modellen `OmsMeldtInnEndring` er definert i `pensjon-etterlatte-felles/common`.

Når `OmsMeldtInnEndringRiver` i `etterlatte-behandling-kafka` mottar hendelsen, skjer følgende:
1. Finner eller oppretter OMS-sak i Gjenny
2. Journalfører endringen i dokarkiv (via `JournalfoerOmsMeldtInnEndringService`)
3. Oppretter en oppgave av type `MELDT_INN_ENDRING` med lesbar merknad (f.eks. "Inntekt") som saksbehandler må behandle manuelt
4. Publiserer `oms_meldt_inn_endring_mottak_fullført` på rapiden, som `selvbetjening-backend` lytter på for å markere endringen som ferdigstilt

Det opprettes **ikke** automatisk en revurdering – det er saksbehandleroppgaven som trigger videre behandling.

---

## pensjon-etterlatte-felles

**Rolle:** Delt infrastruktur, felles Kafka-topics, tjenestespesifikasjoner og verktøy for Team Etterlatte.

### Apper

| App | Beskrivelse |
|-----|-------------|
| `etterlatte-proxy` | GCP→On-Prem-proxy for SOAP-tjenester (tilbakekreving og oppdragssimulering) |
| `etterlatte-notifikasjoner` | Sender SMS/e-post/Min side-varsler til sluttbrukere ved mottatt søknad |
| `ey-pdfgen` | PDF-generator basert på [pdfgen](https://github.com/navikt/pdfgen), brukes til søknads-PDF-er, notater og klageblankett i Gjenny |
| `etterlatte-kafkamanager` | Intern Kafka-oversikt for å følge flyten til en søknad |
| `ey-slackbot` | Slack-botconfig for teamet |

### common-biblioteket

`common/` er et publisert Kotlin-bibliotek som inneholder modeller som deles mellom søknadsdialogene og Gjenny:

- `InnsendtSoeknad`, `Barnepensjon`, `Omstillingsstoenad` – søknadsskjema-modeller
- `OmsMeldtInnEndring`, `OmsEndring` – OMS-selvbetjeningsmodeller
- `Foedselsnummer`, `FoedselsnummerValidator` – felles person-modeller

Gjenny importerer `common` som Gradle-avhengighet. Endringer i `common` påvirker begge repoer.

### Tjenestespesifikasjoner

WSDL/XSD-filer for on-prem SOAP-tjenester som Gjenny bruker via `etterlatte-proxy`:
- Tilbakekreving
- Oppdragssimulering (SimulerFP)
- Avstemming

### Kafka-topics

`etterlatte-proxy` eier topic-definisjonene for Gjennys Kafka:

| Topic (dev) | Topic (prod) | Formål |
|-------------|--------------|--------|
| `etterlatte.dodsmelding` | `etterlatte.etterlatteytelser` | Samme rapid i to miljøer – prod-navn er mer dekkende, dev-navnet er aldri korrigert |

ACL-lista i `.deploy/topic.yaml` viser alle apper som har tilgang – nyttig ved feilsøking av Kafka-tilganger.

### etterlatte-proxy – integrasjon mot Gjenny

`etterlatte-tilbakekreving` i Gjenny kaller `etterlatte-proxy` for å nå tilbakekrevingstjenesten on-prem:
- `POST /tilbakekreving/tilbakekrevingsvedtak`
- `POST /tilbakekreving/kravgrunnlag`

Proxy bruker STS (on-prem) for autentisering og er deployert i FSS.

### ey-pdfgen – integrasjon mot Gjenny

`ey-pdfgen` brukes av Gjenny for PDF-er som **ikke** er brev (brev genereres via Brevbaker). Kalles via `PdfGeneratorKlient`:

- **`etterlatte-behandling-kafka`**: PDF av innsendt søknad og OMS meld-inn-endring ved journalføring
- **`etterlatte-brev-api`**: Notater – `KLAGE_OVERSENDELSE_BLANKETT`, `NORDISK_VEDLEGG`, `MANUELL_SAMORDNING` og tom mal

Lytter på Kafka-rapiden (topic `dodsmelding`/`etterlatteytelser`) og sender Min side-varsler (via `tms-varsel`) når søknad er mottatt. Ingen direkte REST-kall mot Gjenny.
