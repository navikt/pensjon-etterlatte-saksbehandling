package no.nav.etterlatte.testdata.features.prosessering

import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.logger
import no.nav.etterlatte.testdata.kunEtterlatte
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProsesseringFeature(
    private val klient: ProsesseringKlient,
) : TestDataFeature {
    override val beskrivelse: String
        get() = "Prosessering – se og styre tasks"
    override val path: String
        get() = "prosessering"
    override val kunEtterlatte: Boolean
        get() = true
    override val routes: Route.() -> Unit
        get() = {
            get {
                kunEtterlatte {
                    val status = call.request.queryParameters["status"]?.takeIf { it.isNotBlank() }
                    val tasks = klient.hentTasks(brukerTokenInfo, status)

                    call.respond(
                        MustacheContent(
                            "prosessering/tasks.hbs",
                            mapOf(
                                "path" to path,
                                "beskrivelse" to beskrivelse,
                                "statuser" to STATUSER.map { mapOf("navn" to it, "valgt" to (it == status)) },
                                "harTasks" to tasks.isNotEmpty(),
                                "tasks" to tasks.map { it.tilVisning() },
                            ),
                        ),
                    )
                }
            }

            post("{id}/rekjor") {
                kunEtterlatte {
                    val id =
                        call.parameters["id"]?.toLongOrNull()
                            ?: return@kunEtterlatte call.respondRedirect("/$path")
                    logger.info("Rekjører prosessering-task $id fra testdata-GUI")
                    klient.rekjor(brukerTokenInfo, id)
                    call.respondRedirect("/$path")
                }
            }
        }
}

private val STATUSER = listOf("KLAR", "KJØRER", "FULLFØRT", "STOPPET", "AVBRUTT")

private val tidsformat: DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm:ss")
        .withZone(ZoneId.of("Europe/Oslo"))

private fun ProsesseringTaskDto.tilVisning(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "type" to type,
        "status" to status,
        "antallFeil" to antallFeil,
        "stoppaarsak" to (stoppaarsak ?: ""),
        "triggerTid" to tidsformat.format(triggerTid),
        "opprettetTid" to tidsformat.format(opprettetTid),
        "payload" to (payload ?: ""),
        "kanRekjores" to (status == "STOPPET" || status == "AVBRUTT"),
    )
