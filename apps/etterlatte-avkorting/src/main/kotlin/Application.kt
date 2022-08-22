import model.AvkortingService
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
        // TODO refaktorere ut appbuilder
    }.also { env ->
        RapidApplication.create(env)
            .also { LesAvkortingsmelding(it, AvkortingService()) }
            .start()
    }
}