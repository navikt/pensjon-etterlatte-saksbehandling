import no.nav.etterlatte.app

fun main() {
    /*
    Starter appen uten validering av token
    Forutsetter at servicene definert i [app-root]/docker-compose.yml er startet
    */

    val env = System.getenv().toMutableMap()
    env["LOCAL_DEV"] = "true"
    env["DB_JDBC_URL"] = "jdbc:postgresql://localhost:5432/postgres"
    env["DB_PASSWORD"] = "postgres"
    env["DB_USERNAME"] = "postgres"
    env["HTTP_PORT"] = "8085"
    env["KAFKA_BOOTSTRAP_SERVERS"] = "0.0.0.0:9092"
    env["KAFKA_RAPID_TOPIC"] = "etterlatte.dodsmelding"
    env["NAIS_APP_NAME"] = "etterlatte-vedtakvurdering"
    app(env)
}