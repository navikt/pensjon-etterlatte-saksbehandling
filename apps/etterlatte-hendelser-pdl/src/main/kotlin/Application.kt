package no.nav.etterlatte

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.etterlatte.hendelserpdl.DevConfig
import no.nav.etterlatte.hendelserpdl.DodsmeldingerRapid
import no.nav.etterlatte.hendelserpdl.FinnDodsmeldinger
import no.nav.etterlatte.hendelserpdl.leesah.LivetErEnStroemAvHendelser
import no.nav.helse.rapids_rivers.RapidApplication

var stream: FinnDodsmeldinger? = null


fun main() {
    val env = System.getenv().toMutableMap()
    env["KAFKA_BOOTSTRAP_SERVERS"] = env["KAFKA_BROKERS"]
    env["NAV_TRUSTSTORE_PATH"] = env["KAFKA_TRUSTSTORE_PATH"]
    env["NAV_TRUSTSTORE_PASSWORD"] = env["KAFKA_CREDSTORE_PASSWORD"]
    env["KAFKA_KEYSTORE_PASSWORD"] = env["KAFKA_CREDSTORE_PASSWORD"]

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule(Application::module)
        .build()
        .apply {
            GlobalScope.launch {
                stream = FinnDodsmeldinger(LivetErEnStroemAvHendelser(DevConfig().env), DodsmeldingerRapid(this@apply))
                while (true) {
                    if (stream?.stopped == true) {
                        delay(200)
                    } else {
                        stream?.stream()
                    }
                }
            }
        }.start()

}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    routing {
        get("/") {
            call.respondText(
                "Environment: " + System.getenv().keys.joinToString(","),
                contentType = ContentType.Text.Plain
            )
        }
        get("/start") {
            stream?.start()
            call.respondText("Starting leesah stream", contentType = ContentType.Text.Plain)
        }
        get("/status") {
            call.respondText(
                "Iterasjoner: ${stream?.iterasjoner}, Dødsmeldinger ${stream?.dodsmeldinger} av ${stream?.meldinger}",
                contentType = ContentType.Text.Plain
            )
        }
        get("/stop") {
            stream?.stop()
            call.respondText("Stopped reading messages", contentType = ContentType.Text.Plain)
        }

        get("/fromstart") {
            stream?.fraStart()
            call.respondText("partition has been set to start", contentType = ContentType.Text.Plain)
        }

        get("/isAlive") {
            call.respondText("JADDA!", contentType = ContentType.Text.Plain)
        }
        get("/isReady") {
            call.respondText("JADDA!", contentType = ContentType.Text.Plain)
        }
    }
}