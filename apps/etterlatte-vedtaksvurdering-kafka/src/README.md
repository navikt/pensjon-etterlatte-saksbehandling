# Vedtaksvurdering-Kafka

Vedtaksvurdering-Kafka er en [Rapid app](https://github.com/navikt/rapids-and-rivers).

Appen har ansvar for å lytte etter:

1. Vedtak som er blitt oversendt _Oppdrag/Utbetaling_ og skal iverksettes i Gjenny
2. Omregningsvedtak som er attestert, og skal vurderes for samordning
3. Saker som skal reguleres. Den sjekker opp mot vedtaksappen om en sak har en løpende
ytelse. Dersom den har det, så sendes meldingen videre til Omregning

## Konsepter

### Løpende ytelse

En sak er løpende dersom den har 1 eller flere perioder med utbetalinger etter en gitt dato. En opphørt sak kan fortsatt
være løpende dersom opphøret har virkningstidspunkt etter den gitte datoen
