# Samordning-vedtak

Omstillingsstønad er en samordningspliktig ytelse. Applikasjonen tilbyr tjenester for å understøtte informasjonsbehovet til tjenestepensjonsleverandørene (KLP, SPK, m.fl.), slik at de kan gjøre sine vurderinger.

## Autentisering

Tjenesten krever token utstedt av Maskinporten med scope _nav:etterlatteytelser:vedtaksinformasjon.read_

### Autorisasjon

Det må foreligge et tjenestepensjonsforhold i Tjenestepensjonsregisteret som gjelder kallende leverandør, personen vedtaket gjelder og på vedtakets virkningsdato.

## API

### /api/vedtak/{nav-vedtak-id}

#### Informasjonsmodell

##### Samordningsvedtak

| Felt             | Type        | Beskrivelse                |
|:-----------------|:------------|:---------------------------|
| vedtakId         | int         | samme som forespurt id     |
| sakstype         | string      | "OMSTILLINGSSTOENAD"       |
| virkningsdato    | yyyy-MM-dd  | Dato vedtaket gjelder fra  |
| opphoersdato     | yyyy-MM-dd  | Eventuell sluttdato        |
| resultatkode*    | string      | Start / Endring / Stopp    |
| stoppaarsak*     | string      | Inntekt / Opphør / Annet   |
| anvendtTrygdetid | int         | Avdødes trygdetid          |
| perioder         | Periode[]   | Periodiserte beløp         |

##### Periode

| Felt                     | Type       | Beskrivelse                               |
|:-------------------------|:-----------|:------------------------------------------|
| fom                      | yyyy-MM-dd | Periodens start                           |
| tom                      | yyyy-MM-dd | Periodens slutt (ikke påkrevd)            |
| omstillingsstoenadBrutto | int        | Omstillingsstønad før inntektsavkorting   |
| omstillingsstoenadNetto  | int        | Omstillingsstønad etter inntektsavkorting |

## Integrasjon

| Miljø | Ingress                                                  |
|:------|:---------------------------------------------------------|
| dev   | https://etterlatte-samordning-vedtak.ekstern.dev.nav.no  |
| prod  | tbd                                                      |    

## Kom i gang

### Hvordan kjøre lokalt mot dev-gcp

Les [README](../../README.md) på rot i prosjektet.


## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-samordning-vedtak/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
