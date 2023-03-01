# Vedtaksvurdering-Kafka

Vedtaksvurdering-Kafka er en Rapid app.

Appen har ansvar for å lytte etter saker som skal reguleres. Den sjekker opp mot vedtaksappen om en sak har en løpende
ytelse. Dersom den har det, så sendes meldingen videre til Omregning

## Konsepter

### Løpende ytelse

En sak er løpende dersom den har 1 eller flere perioder med utbetalinger etter en gitt dato. En opphørt sak kan fortsatt
være løpende dersom opphøret har virkningstidspunkt etter den gitte datoen
