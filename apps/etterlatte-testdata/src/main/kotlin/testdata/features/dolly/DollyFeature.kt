package testdata.features.dolly


import dolly.DollyService
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.getClientAccessToken
import no.nav.etterlatte.logger
import no.nav.etterlatte.usernameFraToken

class DollyFeature(private val dollyService: DollyService) : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad"
    override val path: String
        get() = "dolly"

    override val routes: Route.() -> Unit
        get() = {
            get {
                val gruppeId = dollyService.hentTestGruppe(usernameFraToken()!!, getClientAccessToken())

                logger.info(gruppeId.toString())

                call.respond(
                    MustacheContent(
                        "soeknad/dolly.hbs", mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId
                        )
                    )
                )
            }

            post {
                try {
                    call.respondRedirect("/$path/sendt")
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
        }
}
