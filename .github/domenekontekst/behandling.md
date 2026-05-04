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

## Avhengigheter

Kaller: `etterlatte-beregning`, `etterlatte-trygdetid`, `etterlatte-vilkaarsvurdering`, Pesys samordning (via `SamordningsKlient` – avgjør om OMS-ytelsen skal samordnes med TP-ordninger)  
Publiserer til: Kafka (`etterlatte.vedtakshendelser`)
