package no.nav.etterlatte.no.nav.etterlatte.testdata.features.automatisk

import no.nav.etterlatte.testdata.dolly.BestillingRequest
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.dolly.ForenkletFamilieModell
import no.nav.etterlatte.testdata.features.dolly.generererBestilling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.concurrent.thread

class Familieoppretter(
    private val dollyService: DollyService,
    private val sleep: (millis: Duration) -> Unit = { Thread.sleep(it) },
    private val iTraad: (handling: () -> Unit) -> Unit = { thread { it() } },
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val maksVentetid = Duration.ofMinutes(1)

    fun opprettFamilie(
        accessToken: String,
        gruppeid: Long,
    ): List<ForenkletFamilieModell> {
        logger.info("Oppretter familie")
        val baselineFamilier = dollyService.hentFamilier(gruppeid, accessToken)
        logger.debug("Baseline er ${baselineFamilier.size} saker")
        val req =
            BestillingRequest(
                erOver18 = false,
                helsoesken = 0,
                halvsoeskenAvdoed = 0,
                halvsoeskenGjenlevende = 0,
                gruppeId = gruppeid,
            )

        logger.info("Oppretter bestilling for gruppeid $gruppeid")
        dollyService.opprettBestilling(generererBestilling(req), req.gruppeId, accessToken)
            .also { bestilling ->
                logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
            }

        var venta = Duration.ZERO
        val ventetid = Duration.ofSeconds(5)
        iTraad {
            while (dollyService.hentFamilier(gruppeid, accessToken) == baselineFamilier && venta <= maksVentetid) {
                venta += ventetid
                logger.info("Ingen ny familie oppretta, venter $ventetid")
                sleep(ventetid)
            }
        }
        logger.info("Ferdig med å vente etter $venta, returnerer")
        return (dollyService.hentFamilier(gruppeid, accessToken) - baselineFamilier).also {
            logger.info("${it.size} endra familier")
        }
    }
}
