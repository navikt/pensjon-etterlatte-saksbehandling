# Etterlatte-hendelser-ufoere

Applikasjonen lytter på hendelser fra Uføre sitt
Kafka-topic `mangler` 
. Hendelser av bestemte typer blir håndtert.

Bygget med Kotlin, Rapids & Rivers og [Ktor](https://ktor.io/).

## Flyt

### Flyt ikke bestemt

## Kom i gang

### Kjøre lokalt

- Kjør opp kafka med `docker-compose up`
- Hent ned secret med `get-secret`
- Start appen som normalt (spesifiser eventuelt om den skal gå mot lokal behandling)
- For å sende en payload til kafka-topicen kjør ` jq -c . test.json | docker exec -i etterlatte-hendelser-ufoere-kafka-1 kafka-console-producer.sh --bootstrap-server localhost:9092 --topic etterlatte`. Merk at jq må være installert.

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-hendelser-ufoere/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
