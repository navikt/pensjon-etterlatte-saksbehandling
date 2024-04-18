package no.nav.etterlatte.no.nav.etterlatte.testdata.features

import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.getDollyAccessToken
import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.BestillingStatus
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.dolly.ForenkletFamilieModell
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
            post {
                opprettFamilierIDolly(100, call.receiveParameters()["gruppeId"]!!.toLong())
            }
        }

    private fun opprettFamilierIDolly(antall: Int, gruppeid: Long): List<ForenkletFamilieModell> {
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
        logger.info("Venter 45 sekunder så Dolly rekk å komme ajour")
        Thread.sleep(Duration.ofSeconds(45))
        logger.info("Ferdig med 45 sekunders-venting")
        if (bestillinger.any { !it.ferdig }) {
            throw IllegalStateException("Ikke alle bestillinger er ferdige!")
        }
        val nyeFamilier = dollyService.hentFamilier(gruppeid, accessToken) - baselineFamilier.toSet()
        return nyeFamilier
    }
}