# etterlatte-vedtaksvurdering

Tjeneste som tilbyr forslag til vedtak som kan fattes og attesteres


## Vedtakshendelser for eksterne systemer

Kafka-topic'en **etterlatte.vedtakshendelser** benyttes til dette. Kontakt teamet for tilgang.

Oppdateringer av ACL er ikke automatisk, og må kjøres inn manuelt med `kubectl apply -f <manifest>`.

| context  | manifest                                |
|:---------|:----------------------------------------|
| dev-gcp  | .nais/topic-vedtakshendelser-dev.yaml   |
| prod-gcp | .nais/topic-vedtakshendelser-prod.yaml  |

## Kom i gang

### Hvordan kjøre lokalt mot dev-gcp

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
VEDTAK_API_URL=http://host.docker.internal:8085
```
