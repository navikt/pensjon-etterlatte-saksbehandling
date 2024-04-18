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
import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.BestillingStatus
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.dolly.ForenkletFamilieModell
import no.nav.etterlatte.testdata.features.dolly.NySoeknadRequest
import no.nav.etterlatte.testdata.features.dolly.generererBestilling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class OpprettOgBehandle(private val dollyService: DollyService) : TestDataFeature {
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
                val familier = opprettFamilierIDolly(
                    2,
                    call.receiveParameters()["gruppeId"]!!.toLong(),
                    Duration.ofSeconds(45)
                )
                val soeknadType = SoeknadType.BARNEPENSJON
                val navIdent = navIdentFraToken()
                familier.map {
                    NySoeknadRequest(
                        type = soeknadType,
                        avdoed = it.avdoed,
                        gjenlevende = it.gjenlevende,
                        barn = it.barn
                    )
                }.forEach {
                    dollyService.sendSoeknad(
                        request = it,
                        navIdent = navIdent
                    )
                }
                call.respond(HttpStatusCode.Created)
            }
        }

    private fun opprettFamilierIDolly(antall: Int, gruppeid: Long, ventetid: Duration)
            : List<ForenkletFamilieModell> {
        val accessToken = getDollyAccessToken()
        val baselineFamilier = dollyService.hentFamilier(gruppeid, accessToken)
        val req = BestillingRequest(
            erOver18 = false,
            helsoesken = 0,
            halvsoeskenAvdoed = 0,
            halvsoeskenGjenlevende = 0,
            gruppeId = gruppeid,
        )
        val bestillinger = mutableListOf<BestillingStatus>()
        for (i in 0..antall) {
            bestillinger.add(
                dollyService.opprettBestilling(generererBestilling(req), req.gruppeId, accessToken)
                    .also { bestilling ->
                        logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
                    }
            )
        }
        logger.info("Venter $ventetid så Dolly rekk å komme ajour")
        Thread.sleep(ventetid)
        logger.info("Ferdig med $ventetid-venting")
        return dollyService.hentFamilier(gruppeid, accessToken) - baselineFamilier.toSet()
    }
}