# etterlatte-testdata


### Lokal utvikling


Kjør opp kafka i docker med: \
`docker-compose up -d` 

Deretter kan du kjøre `ApplicationKt` med følgende environment variables:

```
DEV=true;KAFKA_BROKERS=0.0.0.0:9092;KAFKA_TARGET_TOPIC=etterlatte.dodsmelding
```
