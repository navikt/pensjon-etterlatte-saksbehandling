package no.nav.etterlatte.testdata.features.opprettFamilie

import io.ktor.http.HttpStatusCode
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.brukerIdFraToken
import no.nav.etterlatte.getDollyAccessToken
import no.nav.etterlatte.libs.common.feilhaandtering.sjekk
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.DollyInterface
import no.nav.etterlatte.testdata.dolly.ForenkletFamilieModell
import no.nav.etterlatte.testdata.features.dolly.NySoeknadRequest
import no.nav.etterlatte.testdata.features.dolly.alderVoksenRandom
import no.nav.etterlatte.testdata.features.dolly.defaultDagerSidenDoedsfall
import no.nav.etterlatte.testdata.features.dolly.generererBestilling

class OpprettFamilie(
    private val dollyService: DollyInterface,
    private val dev: Boolean = false,
) : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett Familie"
    override val path: String
        get() = "opprett-familie"
    override val kunEtterlatte: Boolean
        get() = true

    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "opprettfamilie/opprettFamilie.hbs",
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

                    if (familier.isEmpty()) {
                        call.respond("<div>Du har ingen familier, vennligst opprett en nedenfor<div>")
                    } else {
                        call.respond(
                            MustacheContent(
                                "opprettfamilie/familie.hbs",
                                mapOf(
                                    "path" to path,
                                    "familier" to familier.map { FamilieView(it) },
                                ),
                            ),
                        )
                    }
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
                    val antallDagerSidenDoedsfall = params["antallDagerSidenDoedsfall"]?.toInt()
                    sjekk(gjenlevendeAlder >= 0) { "GjenlevendeAlder kan ikke være negativ" }

                    val (brukerId, accessToken) =
                        when (dev) {
                            true -> Pair("", "")
                            false -> Pair(brukerIdFraToken()!!, getDollyAccessToken())
                        }
                    val gruppeId =
                        dollyService.hentTestGruppeId(brukerId, accessToken) ?: throw Exception("Mangler grupperId")

                    val dollyReq =
                        BestillingRequest(
                            gjenlevendeAlder =
                                when (gjenlevendeAlder) {
                                    0 -> alderVoksenRandom()
                                    else -> gjenlevendeAlder
                                },
                            erOver18 = barnOver18,
                            helsoesken = helSoesken,
                            halvsoeskenAvdoed = halvSoesken,
                            gruppeId = gruppeId,
                            antall = 1,
                            antallDagerSidenDoedsfall = antallDagerSidenDoedsfall ?: defaultDagerSidenDoedsfall(),
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

            post("send-soeknad") {
                try {

                    val params = call.receiveParameters()
                    val ytelse = SoeknadType.valueOf(params["ytelse"]!!)
                    val behandlingssteg = Behandlingssteg.valueOf(params["behandlingssteg"]!!)
                    val gjenlevende = params["gjenlevende"]!!
                    val avdoed = params["avdoed"]!!
                    val barnListe = params.getAll("barnListe")!!
                    val soeker =
                        when (ytelse) {
                            SoeknadType.BARNEPENSJON -> params["barn"]!!
                            SoeknadType.OMSTILLINGSSTOENAD -> gjenlevende
                        }

                    val request =
                        NySoeknadRequest(
                            ytelse,
                            avdoed,
                            gjenlevende,
                            barnListe,
                            soeker = soeker,
                        )

                    val brukerId =
                        when (dev) {
                            true -> ""
                            false -> brukerTokenInfo.ident()
                        }

                    val noekkel = dollyService.sendSoeknad(request, brukerId, behandlingssteg)

                    call.respond(
                        """
                        <div>
                        Søknad($ytelse) for $soeker er innsendt og registrert med nøkkel: $noekkel}
                        </div>
                        """.trimIndent(),
                    )
                } catch (e: Exception) {
                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString()),
                        ),
                    )
                }
            }
        }
}

data class FamilieView(
    val familie: ForenkletFamilieModell,
    val sendSoeknadData: String =
        """
        {
            "avdoed": "${familie.avdoed}",
            "barnListe": ${
            familie.barn.map {
                """
                "$it"
                """.trimIndent()
            }
        }
        }
        """.trimIndent(),
)
