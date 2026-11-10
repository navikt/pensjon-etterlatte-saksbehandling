# etterlatte-behandling

Oppretter og forvalter vedtak (beslutninger) for barnepensjon og omstillingsstønad. Fungerer som orkestrator mellom beregning, vilkårsvurdering og nedstrøms systemer (utbetaling, brev).

## Ansvarsområder

- Opprette og behandle førstegangsbehandlinger og revurderinger
- Opprette vedtak basert på behandling, beregning, trygdetid og vilkårsvurdering
- Håndtere vedtaksflyt: opprettelse → fatte → attestere → iverksette
- Publisere vedtakshendelser på Kafka for utbetaling og brev
- Håndtere vedtak for tilbakekreving, klage og etteroppgjør
- Automatisk behandling for kvalifiserte saker

## Sentrale begreper

| Begrep               | Forklaring                                                                   |
|----------------------|------------------------------------------------------------------------------|
| `Behandling`         | Prosessen frem til å kunne fatte vedtak som endrer ytelsen til en søker      |
| `Vedtak`             | Kjerneentitet – én beslutning med type, status og innhold                    |
| `VedtakStatus`       | `OPPRETTET` → `FATTET_VEDTAK` → `ATTESTERT` → `IVERKSATT`                    |
| `VedtakType`         | `INNVILGELSE`, `AVSLAG`, `ENDRING`, `OPPHOER`, `REGULERING`, `ETTEROPPGJOER` |
| `Utbetalingsperiode` | Periode med beløp og type som inngår i vedtaket                              |
| `Vedtakstidslinje`   | Temporær struktur for å kontrollere aktive vedtak på et gitt tidspunkt       |

## Nøkkelklasser

- `Behandling` - superklasse for førstegangsbehandlinger og revurderinger i en sak
- `VedtaksvurderingService` – overordnet orkestrering av vedtaksflyt
- `VedtakBehandlingService` – kjernelogikk for opprettelse og oppdatering
- `VedtaksvurderingRepository` – persistering til Postgres
- `AutomatiskBehandlingService` – automatisk prosessering av egnede saker
- `OutboxJob` – sikrer pålitelig publisering av vedtakshendelser til Kafka (outbox-mønster)

## To flyter for vedtaksopprettelse

**Manuell flyt** (saksbehandler i frontend): Saksbehandler arbeider gjennom behandling, beregning og avkorting, og trigger deretter oppretting av vedtak via et direkte REST-kall mot `etterlatte-behandling`.

**Automatisk flyt** (f.eks. G-regulering): En hendelse publiseres på Rapids-en og konsumeres av `etterlatte-vedtaksvurdering-kafka`, som kaller `etterlatte-behandling` for å opprette vedtaket.

## Kafka-publisering

Publiserer til topic `etterlatte.vedtakshendelser` (deles med andre Nav-systemer) ved iverksetting, med bl.a. `ident`, `sakstype`, `vedtakType`, `vedtakId` og `virkningFom`. Dette trigger utbetaling og brevgenerering. Meldinger internt i Gjenny går på Rapids-en (eget internt topic).

Outbox-mønsteret (database-tabell + `OutboxJob`) brukes for å sikre pålitelig publisering. Dette er spesifikt for vedtak.

## Falsk identitet i personoppslag

Flagget `pensjon-etterlatte.haandter-falsk-identitet` styrer den nye håndteringen i `etterlatte-pdltjenester` og `etterlatte-brev-api`. Standardverdien er `false`. Aktiver flagget i dev og la det være avslått i prod i Unleash.

I pdltjenester gjelder sjekken bare `/person/navn-foedsel`, som brukes av statuslinjen og journalføringen. En falsk identitet med tilgjengelige opplysninger gir `erFalskIdentitet = true` og et varsel i statuslinjen og ved endring av journalpostens bruker. Mangler navn, fødselsopplysninger eller folkeregisteridentifikator, gir oppslaget 409 med kode `FALSK_IDENTITET`. Dette er ikke en kontroll av hele persongalleriet: familieoppslag og innhenting av behandlingsgrunnlag bruker andre oppslag og mapping uten denne sjekken. PR-en legger heller ikke til noen sperre mot å opprette sak eller fatte vedtak.

I brev-api gjelder flagget adresseoppslag for brevmottakere, som også kan være gjenlevende forelder eller innsender. Se [brev-api](brev-api.md).

## Avhengigheter

Kaller: `etterlatte-beregning`, Pesys samordning (via `SamordningsKlient` – avgjør om OMS-ytelsen skal samordnes med TP-ordninger)  
Publiserer til: Kafka (`etterlatte.vedtakshendelser`)

## Saksbehandlere i oppgavebenken

Saksbehandlervelgeren for tildeling bruker `saksbehandler_info`, filtrert på enhetene til innlogget bruker. `SaksbehandlerJob` oppdaterer enhetene fra Entra-proxy hvert 20. minutt innenfor jobbens åpningstid, for identer som finnes på oppgaver.

`EntraProxyKlient` oversetter HTTP 404 til en tom enhetsliste. Jobben lagrer også tomme lister, slik at saksbehandleren forsvinner fra velgeren hvis ingen enheter matcher. Et senere oppslag med riktige enheter gjør saksbehandleren tilgjengelig igjen når frontend henter listen på nytt. Andre oppslagsfeil hopper over oppdateringen for identen og bevarer lagrede enheter. Denne synkroniseringen endrer ikke oppgavenes tildeling.

Avklart med teamet bak Entra-proxy 2. november 2026: Tjenesten filtrerer bevisst bort ansatte i permisjon for å hindre tildeling av oppgaver i Oppgave/Gosys. Team IdM arbeider med håndtering av tilganger ved permisjon; fremtidig oppførsel i dette endpointet er ikke avklart. En tom enhetsliste er derfor ikke i seg selv tegn på en oppslagsfeil.

Gjenny skiller derfor mellom to lister:
- **Tildeling** (`GET /api/saksbehandlere?enheter=...`): saksbehandlere med enheter fra Entra-proxy. Ansatte i permisjon er bevisst utelatt.
- **Saksbehandlerfilteret i Oppgavelista** bruker i tillegg `GET /api/saksbehandlere/med-aapne-oppgaver`: saksbehandlere med åpne oppgaver i enhetene til innlogget bruker, med samme avgrensning som oppgavesøket. Slik kan kolleger finne og overta oppgaver hos noen i permisjon, uten at personen kan få nye oppgaver.
