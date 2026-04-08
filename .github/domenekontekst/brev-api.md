# etterlatte-brev-api

Oppretter, redigerer, ferdigstiller og distribuerer brev til brukere. Bruker Brevbakeren som malmotor og Joark for arkivering.

## Ansvarsområder

- Generere vedtaksbrev, varselbrev, tilbakekrevingsbrev, klagebrev og interne notater
- Støtte redigerbart brevinnhold (fritekst via Slate-editor)
- Arkivere ferdigstilte brev i Joark
- Distribuere brev via Brevdistribusjon (post/digital)

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `Brev` | Hoveddokument med status, mottaker, språk og innhold |
| `BrevStatus` | `OPPRETTET` → `FERDIGSTILT` → `DISTRIBUERT` (eller `UTGAAR`) |
| `Brevprosesstype` | Kategori: `VEDTAK`, `VARSELBREV`, `NOTAT`, `TILBAKEKREVING`, osv. |
| `Mottaker` | Person eller organisasjon som mottar brevet |
| `RedigerbartBrev` | Brev med fritekstinnhold som saksbehandler kan endre |

## Nøkkelklasser

- `BrevService` – livssyklus for brev (opprett, ferdigstill, distribuer)
- `Brevoppretter` – oppretter brevinnhold med databerikelse fra andre tjenester
- `VedtaksbrevService` / `VarselbrevService` – spesialisert generering per brevtype
- `JournalfoerBrevService` – arkiverer brev i Joark
- `Brevdistribuerer` – sender til Brevdistribusjon
- `PDFService` – genererer PDF fra brevinnhold via Brevbakeren

**Brevbakeren** er en ekstern tjeneste eid av pensjonsbrev-teamet i NAV. Gjenny har skrivetilgang til egne brevmaler og vedlikeholder innholdet i disse selv.

## Avhengigheter

Kaller: `etterlatte-behandling` (behandlingskontekst, tilgangskontroll og grunnlag inkl. adresse og persondata), `Brevbakeren` (malrendering), Joark/SAF (arkiv), Brevdistribusjon (utsending), PDL (adresseoppslag)

## Gammel vs. ny brevflyt

Det eksisterer to parallelle flyter for å bygge opp brevdata:

**Gammel flyt:** `etterlatte-brev-api` bygger brevdata-DTO-er selv via `BrevApiKlient`. Denne flyten er kompleks og har utilsiktede endringsmuligheter. Unngå endringer her med mindre det er strengt nødvendig.

**Ny flyt (strukturert brev):** Brevdata bygges opp i `etterlatte-behandling` og sendes strukturert til `etterlatte-brev-api` via `StrukturertBrevService` (route: `brev/strukturert/`). Brukes i dag for etteroppgjør, tilbakekreving og OMS innvilgelse. Alle nye brevtyper skal bruke denne flyten.

For å se kontrasten: sammenlign OMS innvilgelse (ny flyt via `VedtaksbrevService` i behandling) med OMS avslag (gammel flyt via `BrevApiKlient` → `brev-api`).
