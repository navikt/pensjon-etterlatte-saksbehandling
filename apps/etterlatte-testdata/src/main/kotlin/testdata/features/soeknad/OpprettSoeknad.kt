package testdata.features.soeknad

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.logger
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.producer
import java.time.OffsetDateTime
import java.util.UUID

object OpprettSoeknadFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad"
    override val path: String
        get() = "soeknad"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "soeknad/ny-soeknad.hbs", mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path
                        )
                    )
                )
            }

            post {
                try {
                    val (partisjon, offset) = call.receiveParameters().let {
                        producer.publiser(
                            requireNotNull(it["key"]),
                            opprettSoeknadJson(
                                gjenlevendeFnr = it["fnrGjenlevende"]!!,
                                avdoedFnr = it["fnrAvdoed"]!!,
                                barnFnr = it["fnrBarn"]!!
                            ),
                            mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                        )
                    }
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    call.respondRedirect("/$path/sendt?partisjon=$partisjon&offset=$offset")
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)

                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString())
                        )
                    )
                }
            }

            get("sendt") {
                val partisjon = call.request.queryParameters["partisjon"]!!
                val offset = call.request.queryParameters["offset"]!!

                call.respond(
                    MustacheContent(
                        "soeknad/soeknad-sendt.hbs", mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                            "partisjon" to partisjon,
                            "offset" to offset
                        )
                    )
                )
            }
        }

}

private fun opprettSoeknadJson(gjenlevendeFnr: String, avdoedFnr: String, barnFnr: String): String {
    val skjemaInfo = """
        {
            "soeker": {
              "foedselsnummer": {
                "svar": "$barnFnr"
              },
              "type": "BARN"
            },
            "foreldre": [
              {
                "foedselsnummer": {
                  "svar": "$gjenlevendeFnr"
                },
                "type": "GJENLEVENDE_FORELDER"
              },
              {
                "foedselsnummer": {
                  "svar": "$avdoedFnr"
                },
                "type": "AVDOED"
              }
            ],
            "type": "BARNEPENSJON",
            "versjon": "2"
          }
    """.trimIndent()

    return JsonMessage.newMessage(
        mapOf(
            "@event_name" to "soeknad_innsendt",
            "@skjema_info" to objectMapper.readValue<ObjectNode>(skjemaInfo),
            "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
            "@template" to "soeknad",
            "@fnr_soeker" to barnFnr,
            "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L),
            "@adressebeskyttelse" to "UGRADERT"
        )
    ).toJson()
}
