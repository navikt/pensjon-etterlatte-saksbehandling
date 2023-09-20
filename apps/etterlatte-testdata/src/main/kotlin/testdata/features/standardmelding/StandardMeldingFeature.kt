package no.nav.etterlatte.testdata.features.standardmelding

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.batch.payload
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.logger
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.producer
import no.nav.etterlatte.testdata.JsonMessage
import java.time.OffsetDateTime
import java.util.UUID

const val AREMARK_PERSON = "12101376212"

object StandardMeldingFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Post standardmelding"
    override val path: String
        get() = "standardmelding"
    override val routes: Route.() -> Unit
        get() = {
            get {
                sendMelding(
                    payload(AREMARK_PERSON, Tidspunkt.now().toLocalDatetimeUTC()),
                    producer,
                )

                call.respond(
                    MustacheContent(
                        "ny-standardmelding.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                        ),
                    ),
                )
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
    val message =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "trenger_behandling",
                "@skjema_info" to objectMapper.readValue<ObjectNode>(input),
                "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
                "@template" to "soeknad",
                "@fnr_soeker" to AREMARK_PERSON,
                "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L).toString(),
            ),
        )
    return "0" to message.toJson()
}
