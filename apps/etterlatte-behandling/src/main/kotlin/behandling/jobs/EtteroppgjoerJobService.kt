package no.nav.etterlatte.behandling.job

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import org.slf4j.LoggerFactory
import java.time.YearMonth

@OptIn(DelicateCoroutinesApi::class)
class EtteroppgjoerJobService(
    private val etteroppgjoerService: EtteroppgjoerService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val skatteoppgjoerHendelserService: SkatteoppgjoerHendelserService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun run() {
        newSingleThreadContext("etteroppgjoerjob").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_PERIODISK_JOBB, false)) {
                    logger.info("Starter periodiske jobber for etteroppgjoer")
                    startEtteroppgjoerKjoering(etteroppgjoerService, etteroppgjoerForbehandlingService, skatteoppgjoerHendelserService)
                } else {
                    logger.info("Periodisk jobber for etteroppgjoer er deaktivert")
                }
            }
        }
    }
}

internal fun startEtteroppgjoerKjoering(
    etteroppgjoerService: EtteroppgjoerService,
    etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    skatteoppgjoerHendelserService: SkatteoppgjoerHendelserService,
) {
    val yearNow = YearMonth.now().year
    val aarMellom2024OgNaa = (2024..yearNow).toList()

    val antallHendelser = 500

    for (inntektsaar in aarMellom2024OgNaa) {
        etteroppgjoerService.finnOgOpprettEtteroppgjoer(inntektsaar)

        // TODO: legge inn denne når vi har testet litt mer?
        // skatteoppgjoerHendelserService.startHendelsesKjoering(HendelseKjoeringRequest(500),"automatisk")

        etteroppgjoerForbehandlingService.finnOgOpprettForbehandlinger(inntektsaar)
    }
}
