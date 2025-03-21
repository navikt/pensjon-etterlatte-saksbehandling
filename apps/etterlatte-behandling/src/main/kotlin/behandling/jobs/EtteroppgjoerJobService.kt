package no.nav.etterlatte.behandling.job

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.YearMonth

@OptIn(DelicateCoroutinesApi::class)
class EtteroppgjoerJobService(
    private val etteroppgjoerService: EtteroppgjoerService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun run() {
        newSingleThreadContext("etteroppgjoerjob").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_PERIODISK_JOBB, false)) {
                    logger.info("Starter periodisk jobb for oppretting av etteroppgjoer")
                    finnOgOpprettEtteroppgjoer(logger, etteroppgjoerService)
                } else {
                    logger.info("Periodisk jobb for opprette etteroppgjoer er deaktivert")
                }
            }
        }
    }
}

internal suspend fun finnOgOpprettEtteroppgjoer(
    logger: Logger,
    etteroppgjoerService: EtteroppgjoerService,
) {
    val yearNow = YearMonth.now().year
    // TODO: hvor lang tilbake i tid skal vi sjekke?
    val aarMellom2020OgNaa = (2020..yearNow).toList()

    logger.info("Starter jobb for å opprette etteroppgjoer for inntektsaar=$aarMellom2020OgNaa")
    for (inntektsaar in aarMellom2020OgNaa) {
        etteroppgjoerService.finnOgOpprettEtteroppgjoer(inntektsaar, "Automatisk")
    }
}
