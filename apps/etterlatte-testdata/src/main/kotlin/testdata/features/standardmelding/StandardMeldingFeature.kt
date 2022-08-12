package testdata.features.standardmelding

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.nav.etterlatte.*
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.batch.payload
import no.nav.etterlatte.kafka.KafkaProdusent
import java.time.OffsetDateTime
import java.util.*

val aremark_person = "12101376212"

object StandardMeldingFeature: TestDataFeature {
    override val beskrivelse: String
        get() = "Post standardmelding"
    override val path: String
        get() = "standardmelding"
    override val routes: Route.() -> Unit
        get() = {
            get {
                sendMelding(
                    payload(aremark_person),
                    producer
                )

                call.respondHtml {
                    this.head {
                        title { +"Post melding til Kafka" }
                    }
                    body {
                        h3 {
                            +"Standardmelding postet!"
                        }
                        br {}
                        ul {
                            li {
                                a {
                                    href = "/"
                                    +"Tilbake til hovedmeny"
                                }
                            }
                        }
                    }
                }
            }
        }

}
internal fun sendMelding(
    melding: String,
    producer: KafkaProdusent<String, String>,
) {
    val startMillis = System.currentTimeMillis()
    logger.info("Publiserer melding")

    createRecord(melding).also { producer.publiser(it.first, it.second) }

    logger.info("melding publisert på ${(System.currentTimeMillis() - startMillis) / 1000}s ")
}

private fun createRecord(input: String): Pair<String, String> {
    val message = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "soeknad_innsendt",
            "@skjema_info" to objectMapper.readValue<ObjectNode>(input),
            "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
            "@template" to "soeknad",
            "@fnr_soeker" to aremark_person,
            "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L).toString()
        )
    )
    return "0" to message.toJson()
}