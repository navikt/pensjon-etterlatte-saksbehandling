package no.nav.etterlatte.no.nav.etterlatte.testdata.features.automatisk

import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.BestillingStatus
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.dolly.ForenkletFamilieModell
import no.nav.etterlatte.testdata.features.dolly.generererBestilling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class Familieoppretter(
    private val dollyService: DollyService,
    private val sleep: (millis: Duration) -> Unit = { Thread.sleep(it) },
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun opprettFamilie(
        accessToken: String,
        gruppeid: Long,
        antall: Int,
        antallDagerSidenDoedsfall: Int,
    ): BestillingStatus {
        val req =
            BestillingRequest(
                erOver18 = false,
                helsoesken = 0,
                halvsoeskenAvdoed = 0,
                halvsoeskenGjenlevende = 0,
                gruppeId = gruppeid,
                antall = antall,
                antallDagerSidenDoedsfall = antallDagerSidenDoedsfall,
            )

        logger.info("Oppretter bestilling for gruppeid $gruppeid")
        return dollyService
            .opprettBestilling(generererBestilling(req), req.gruppeId, accessToken)
            .also { bestilling ->
                logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
            }
    }

    fun hentFamilier(
        gruppeid: Long,
        accessToken: String,
        bestilling: BestillingStatus,
        oenskaAntall: Int,
        baselineFamilier: List<ForenkletFamilieModell>,
    ): List<ForenkletFamilieModell> {
        var venta = Duration.ZERO
        val ventetid = Duration.ofSeconds(10)
        val maksVentetid = Duration.ofSeconds(20).multipliedBy(oenskaAntall.toLong())
        while (!hentStatusBestilling(bestilling = bestilling.id, accessToken).ferdig && venta <= maksVentetid) {
            venta += ventetid
            logger.info("Ikke ferdig med å opprette bestilling, venter $ventetid")
            sleep(ventetid)
        }
        logger.info("Ferdig med å vente etter $venta, returnerer")
        return (hentFamilier(gruppeid, accessToken) - baselineFamilier).filterNot { it.ibruk }.also {
            logger.info("${it.size} endra familier")
        }
    }

    fun hentFamilier(
        gruppeid: Long,
        accessToken: String,
    ) = try {
        dollyService.hentFamilier(gruppeid, accessToken)
    } catch (e: Exception) {
        logger.warn("Kunne ikke hente familie, prøver igjen snart", e)
        listOf()
    }

    private fun hentStatusBestilling(
        bestilling: Long,
        accessToken: String,
    ) = try {
        dollyService.statusBestilling(bestilling, accessToken)
    } catch (e: Exception) {
        logger.warn("Kunne ikke hente status bestilling, prøver igjen snart", e)
        BestillingStatus(bestilling, false)
    }
}
