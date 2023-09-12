# Samordning-vedtak

Omstillingsstønad er en samordningspliktig ytelse. Applikasjonen tilbyr tjenester for å understøtte informasjonsbehovet til tjenestepensjonsleverandørene (KLP, SPK, m.fl.), slik at de kan gjøre sine vurderinger.

## Autentisering

Tjenesten krever token utstedt av Maskinporten med scope _nav:etterlatteytelser:vedtaksinformasjon.read_

### Autorisasjon

Det må foreligge et tjenestepensjonsforhold i Tjenestepensjonsregisteret som gjelder kallende leverandør, personen vedtaket gjelder og på vedtakets virkningsdato.

## API

| Endepunkt                      | Responstype         | Beskrivelse                                                                                                                           |
|:-------------------------------|---------------------|:--------------------------------------------------------------------------------------------------------------------------------------|
| /api/vedtak?virkFom=YYYY-MM-DD | Samordningsvedtak[] | Henter ut vedtaksinformasjon for gitt person fra og med gitt dato. <br/> Fødselsnummeret angis i en `fnr`-header                      |
| /api/vedtak/{nav-vedtak-id}    | Samordningsvedtak   | Henter ut informasjon om et spesifikt vedtak. VedtaksIDen kommer fra samordningskøen hvor det varsles løpende om vedtak som gjøres.   |


#### Informasjonsmodell

##### Samordningsvedtak

| Felt             | Type        | Beskrivelse               |
|:-----------------|:------------|:--------------------------|
| vedtakId         | int         | Vedtakets unike id        |
| sakstype         | string      | OMS                       |
| virkningsdato    | YYYY-MM-DD  | Dato vedtaket gjelder fra |
| opphoersdato     | YYYY-MM-DD  | Eventuell sluttdato       |
| type             | string      | START/ENDRING/OPPHOER     |
| arsak*           | string      | INNTEKT/ANNET             |
| anvendtTrygdetid | int         | Avdødes trygdetid         |
| perioder         | Periode[]   | Periodiserte beløp        |

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
