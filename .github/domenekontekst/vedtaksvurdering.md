# etterlatte-vedtaksvurdering

Oppretter og forvalter vedtak (beslutninger) for pensjonssaker. Fungerer som orkestrator mellom beregning, vilkårsvurdering og nedstrøms systemer (utbetaling, brev).

## Ansvarsområder

- Opprette vedtak basert på behandling, beregning og vilkårsvurdering
- Håndtere vedtaksflyt: opprettelse → fatte → attestere → iverksette
- Publisere vedtakshendelser på Kafka for utbetaling og brev
- Håndtere vedtak for tilbakekreving, klage og etteroppgjør
- Automatisk behandling for kvalifiserte saker

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `Vedtak` | Kjerneentitet – én beslutning med type, status og innhold |
| `VedtakStatus` | `OPPRETTET` → `FATTET_VEDTAK` → `ATTESTERT` → `IVERKSATT` |
| `VedtakType` | `INNVILGELSE`, `AVSLAG`, `ENDRING`, `OPPHOER`, `REGULERING`, `ETTEROPPGJOER` |
| `Utbetalingsperiode` | Periode med beløp og type som inngår i vedtaket |
| `Vedtakstidslinje` | Temporær struktur for å kontrollere aktive vedtak på et gitt tidspunkt |

## Nøkkelklasser

- `VedtaksvurderingService` – overordnet orkestrering av vedtaksflyt
- `VedtakBehandlingService` – kjernelogikk for opprettelse og oppdatering
- `VedtaksvurderingRepository` – persistering til Postgres
- `AutomatiskBehandlingService` – automatisk prosessering av egnede saker
- `OutboxJob` – sikrer pålitelig publisering av vedtakshendelser til Kafka (outbox-mønster)

## To flyter for vedtaksopprettelse

**Manuell flyt** (saksbehandler i frontend): Saksbehandler arbeider gjennom behandling, beregning og avkorting, og trigger deretter oppretting av vedtak via et direkte REST-kall mot `etterlatte-vedtaksvurdering`.

**Automatisk flyt** (f.eks. G-regulering): En hendelse publiseres på Rapids-en og konsumeres av `etterlatte-vedtaksvurdering-kafka`, som kaller `etterlatte-vedtaksvurdering` for å opprette vedtaket.

## Kafka-publisering

Publiserer til topic `etterlatte.vedtakshendelser` (deles med andre Nav-systemer) ved iverksetting, med bl.a. `ident`, `sakstype`, `vedtakType`, `vedtakId` og `virkningFom`. Dette trigger utbetaling og brevgenerering. Meldinger internt i Gjenny går på Rapids-en (eget internt topic).

Outbox-mønsteret (database-tabell + `OutboxJob`) brukes for å sikre pålitelig publisering. Dette er spesifikt for `vedtaksvurdering`.

## Avhengigheter

Kaller: `etterlatte-behandling` (behandlingskontekst, tilgangskontroll og grunnlag), `etterlatte-beregning`, `etterlatte-trygdetid`, vilkårsvurdering (via behandling), Pesys samordning (via `SamordningsKlient` – avgjør om OMS-ytelsen skal samordnes med TP-ordninger)  
Publiserer til: Kafka (`etterlatte.vedtakshendelser`)
