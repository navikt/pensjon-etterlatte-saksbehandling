package no.nav.etterlatte.no.nav.etterlatte.testdata.features.opprettFamilie

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.brukerIdFraToken
import no.nav.etterlatte.getDollyAccessToken
import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.DollyInterface
import no.nav.etterlatte.testdata.features.dolly.generererBestilling

class OpprettFamilie(
    private val dollyService: DollyInterface,
    private val dev: Boolean = false,
) : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett Familie"
    override val path: String
        get() = "opprettFamilie"
    override val kunEtterlatte: Boolean
        get() = true

    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "opprettFamilie/opprettFamilie.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                        ),
                    ),
                )
            }

            get("hent-familier") {
                try {
                    val (brukerId, accessToken) =
                        when (dev) {
                            true -> Pair("", "")
                            false -> Pair(brukerIdFraToken()!!, getDollyAccessToken())
                        }
                    val gruppeId =
                        dollyService.hentTestGruppeId(brukerId, accessToken) ?: throw Exception("Mangler grupperId")

                    val familier = dollyService.hentFamilier(gruppeId, accessToken)

                    call.respond(
                        MustacheContent(
                            "opprettFamilie/familie.hbs",
                            mapOf("familier" to familier),
                        ),
                    )
                } catch (e: Exception) {
                    val melding = e.message ?: "Noe gikk galt"
                    call.respond(HttpStatusCode.BadRequest, melding)
                }
            }

            post("opprett") {
                try {
                    val params = call.receiveParameters()

                    val gjenlevendeAlder =
                        params["gjenlevendeAlder"]?.toInt() ?: throw Exception("Må ha gjenlevendeAlder")
                    val barnOver18 = params["barnOver18"]?.toBoolean() ?: false
                    val halvSoesken = params["halvsoeskenAvdoed"]?.toInt() ?: throw Exception("Må ha halvsoeskenAvdoed")
                    val helSoesken = params["helsoesken"]?.toInt() ?: throw Exception("Må ha helsoesken")

                    val (brukerId, accessToken) =
                        when (dev) {
                            true -> Pair("", "")
                            false -> Pair(brukerIdFraToken()!!, getDollyAccessToken())
                        }
                    val gruppeId =
                        dollyService.hentTestGruppeId(brukerId, accessToken) ?: throw Exception("Mangler grupperId")

                    val dollyReq =
                        BestillingRequest(
                            gjenlevendeAlder = gjenlevendeAlder,
                            erOver18 = barnOver18,
                            helsoesken = helSoesken,
                            halvsoeskenAvdoed = halvSoesken,
                            gruppeId = gruppeId,
                            antall = 1,
                        )

                    val bestilling =
                        dollyService.opprettBestilling(generererBestilling(dollyReq), gruppeId, accessToken)

                    if (bestilling.ferdig) {
                        call.respond("Bestilling er fullført. Refresh side.")
                    } else {
                        call.respond("Bestilling er Sendt. Refresh side om noen sekunder.")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Noe gikk galt")
                }
            }
        }
}
