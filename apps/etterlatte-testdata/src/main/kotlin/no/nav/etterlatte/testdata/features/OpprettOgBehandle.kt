package no.nav.etterlatte.no.nav.etterlatte.testdata.features

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
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.no.nav.etterlatte.testdata.features.automatisk.Familieoppretter
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.dolly.ForenkletFamilieModell
import no.nav.etterlatte.testdata.features.dolly.NySoeknadRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

class OpprettOgBehandle(private val dollyService: DollyService, private val familieoppretter: Familieoppretter) :
    TestDataFeature {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    override val beskrivelse: String
        get() = "Opprett og behandle søknad(er)"
    override val path: String
        get() = "opprett-og-behandle"

    override val routes: Route.() -> Unit
        get() = {
            get {
                val accessToken = getDollyAccessToken()

                val gruppeId = dollyService.hentTestGruppeId(brukerIdFraToken()!!, accessToken)

                call.respond(
                    MustacheContent(
                        "dolly/opprett-og-behandle.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId,
                        ),
                    ),
                )
            }

            post {
                val gruppeid = call.receiveParameters()["gruppeId"]!!.toLong()
                val soeknadType = SoeknadType.BARNEPENSJON
                val behandlingssteg = Behandlingssteg.IVERKSATT
                val navIdent = navIdentFraToken()
                val oenskaAntall = 10

                opprettOgSendInn(oenskaAntall, gruppeid, soeknadType, navIdent, behandlingssteg)
                call.respond(HttpStatusCode.Created)
            }
        }

    private fun opprettOgSendInn(
        oenskaAntall: Int,
        gruppeid: Long,
        soeknadType: SoeknadType,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ) {
        thread {
            var opprettaFamilier = 0
            logger.info("Oppretter $oenskaAntall familier og sender inn søknad for hver")
            while (opprettaFamilier < oenskaAntall) {
                val familie = familieoppretter.opprettFamilie(getDollyAccessToken(), gruppeid)
                familie.forEach {
                    opprettaFamilier++
                    logger.info("Sender inn søknad for familie med avdød ${it.avdoed}")
                    sendSoeknad(it, soeknadType, navIdent, behandlingssteg)
                    logger.info("Søknad sendt for familie med avdød ${it.avdoed}")
                }
            }
        }
    }

    private fun sendSoeknad(
        it: ForenkletFamilieModell,
        soeknadType: SoeknadType,
        navIdent: String?,
        behandlingssteg: Behandlingssteg,
    ) {
        val request =
            NySoeknadRequest(
                type = soeknadType,
                avdoed = it.avdoed,
                gjenlevende = it.gjenlevende,
                barn = it.barn,
            )
        dollyService.sendSoeknad(
            request = request,
            navIdent = navIdent,
            behandlingssteg = behandlingssteg,
        )
    }
}
