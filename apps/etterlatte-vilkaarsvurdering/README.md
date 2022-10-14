# etterlatte-vilkaarsvurdering

Tjeneste som tilbyr endepunkter for å hente ut vilkår for en behandling og sette dem til oppfylt / ikke oppfylt.

Det som støttes så langt er
- Hovedvilkår
- Unntaksvilkår

Utestående funksjonalitet er
- Grunnlag på vilkår
- Automatiserte vilkårsvurdering

## Kjøre lokalt

1. Start Kafka og Mock-OAuth2-Server lokalt ved å kjøre `docker-compose up -d`
2. Sett følgende miljøvariabler ved oppstart av applikasjon:
   `KAFKA_RAPID_TOPIC=etterlatte;KAFKA_BOOTSTRAP_SERVERS=0.0.0.0:9092;NAIS_APP_NAME=etterlatte-vilkaarsvurdering;AZURE_APP_WELL_KNOWN_URL=http://localhost:8082/azure/.well-known/openid-configuration;AZURE_APP_CLIENT_ID=clientId`

### Teste mot rapid
Kjør følgende for å poste innholdet i en fil til kafka:
`jq -c . src/test/resources/grunnlagEndret.json | docker exec -i etterlatte-vilkaarsvurdering-kafka-1 kafka-console-producer.sh --bootstrap-server localhost:9092 --topic etterlatte 'jq'`

### Teste mot REST-endepunkter
- Gjør requester mot endepunkter under `http://localhost:8080/api/vilkaarsvurdering`
- Sett header `Authorization` med verdien `Bearer eyJraWQiOiJhenVyZSIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJhenVyZS1pZCBmb3Igc2Frc2JlaGFuZGxlciIsIk5BVmlkZW50IjoiU2Frc2JlaGFuZGxlcjAxIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgyL2F6dXJlIiwibm9uY2UiOiI1Njc4IiwidGlkIjoiYXp1cmUiLCJhdWQiOiJjbGllbnRJZCIsIm5iZiI6MTY2NTc1ODE0NCwiYXpwIjoiY2xpZW50SWQiLCJuYW1lIjoiSm9obiBEb2UiLCJleHAiOjE2NjU3NjE3NDQsImlhdCI6MTY2NTc1ODE0NCwianRpIjoiOWYxOTc0NDUtZDc3ZC00YmU4LWE0OTUtN2FiZGJhMTVhYWU3In0.cnghBhSI_LTKQw1vzPss-OuH-QWH2KxbaAVYRxXSaOEGy2JHtHIL7AOUzghwAMpYaqVbHhjsOP4rGEYnHxYYGbvxrLI9hEXb3UFPX11xZFSk8WzUa5th6SwKnbBdm2xu7V11KoWFFCi6KHlCHzlCkwnAXNw-CsWGdmvdMo0NnbbJV6FdYY0FUGdJeFzV38ExVN0QakYkYqIQn5xfRh101mGv0cjEYA49RRGZjqXl8tz-SlwspdtqNpW9p0hZTQ8q2VywU_jiBlEv1uIqgU2FmE59oVLxOXIkjJa8XSVBdV-ZOShbHpLv__l9gV_kUaGu31-BkeEX2i8IF7zLJjiVMQ`
