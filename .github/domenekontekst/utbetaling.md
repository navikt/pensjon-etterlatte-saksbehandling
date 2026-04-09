# etterlatte-utbetaling

Oversetter iverksatte vedtak til utbetalinger i Oppdragssystemet (OS), håndterer kvittering og utfører daglig grensesnittavstemming.

## Ansvarsområder

- Lytte på Kafka etter iverksatte vedtak og opprette utbetalingsoppdrag
- Sende utbetalinger til Oppdrag via IBM MQ
- Motta og tolke kvitteringer fra Oppdrag
- Daglig grensesnittavstemming mot Oppdrag
- Ca. månedlig konsistensavstemming mot Oppdrag
- Simulere utbetalinger for forhåndsvisning

## Sentrale begreper

| Begrep                   | Forklaring                                                        |
|--------------------------|-------------------------------------------------------------------|
| `Utbetaling`             | Én utbetalingstransaksjon med status og linjer                    |
| `Utbetalingslinje`       | Enkeltlinje i oppdraget (type `UTBETALING` eller `OPPHOER`)       |
| `Utbetalingsstatus`      | `SENDT` → `MOTTATT` → `GODKJENT` / `AVVIST` / `GODKJENT_MED_FEIL` |
| `Iverksetting`           | Prosessen med å konvertere vedtak til utbetalingsoppdrag          |
| `Grensesnittsavstemming` | Daglig batch-kontroll av samsvar mellom systemer                  |
| `Konsistensavstemming`   | Ca. månedlig kontroll av alle løpende utbetalinger fra systemet   |


## Nøkkelklasser

- `VedtakMottakRiver` – Kafka-lytter som starter iverksetting ved nytt vedtak
- `UtbetalingService` – kjernetjeneste for livssyklushåndtering av utbetalinger
- `KvitteringMottaker` – poller MQ-kø for kvitteringer fra Oppdrag
- `GrensesnittsavstemmingService` – daglig avstemmingsjobb
- `SimuleringOsService` – simulerer utbetaling mot Oppdrag
- `KonsistensavstemmingServic` - månedlig avstemmingsjobb

## Avhengigheter

Lytter på: Den interne Rapids-en (`ATTESTERT`-hendelse fra vedtaksvurdering)  
Kaller: IBM MQ (Oppdrag), `etterlatte-behandling`, `etterlatte-vedtaksvurdering`  
Publiserer: utbetalingsstatus tilbake på Kafka
