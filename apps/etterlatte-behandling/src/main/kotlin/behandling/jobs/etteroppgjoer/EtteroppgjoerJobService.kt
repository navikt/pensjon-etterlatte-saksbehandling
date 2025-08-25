package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory
import java.time.YearMonth

enum class EtteroppgjoerFilter(
    val status: EtteroppgjoerStatus,
    val harSanksjon: Boolean,
    val harInsitusjonsopphold: Boolean,
    val harOpphoer: Boolean,
    val harBosattUtland: Boolean,
) {
    ENKEL(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER, false, false, false, false),
}

@OptIn(DelicateCoroutinesApi::class)
class EtteroppgjoerJobService(
    private val etteroppgjoerService: EtteroppgjoerService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val vedtakKlient: VedtakKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun run() {
        newSingleThreadContext("etteroppgjoerjob").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER, false)) {
                    // TODO: definere filter i endepunkt som kaller
                    startEtteroppgjoerKjoering(EtteroppgjoerFilter.ENKEL)
                } else {
                    logger.info("Periodisk jobber for etteroppgjoer er deaktivert")
                }
            }
        }
    }

    suspend fun startEtteroppgjoerKjoering(filter: EtteroppgjoerFilter) {
        val yearNow = YearMonth.now().year
        val aarMellom2024OgNaa = (2024..yearNow).toList()

        for (inntektsaar in aarMellom2024OgNaa) {
            if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_KJOERING_OPPRETT_ETTEROPPGJOER, false)) {
                finnOgOpprettEtteroppgjoer(inntektsaar)
            } else {
                logger.info("Opprette etteroppgjoer i forbindelse med kjoering er deaktivert")
            }

            if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_KJOERING_OPPRETT_FORBEHANDLING, false)) {
                finnOgOpprettForbehandlinger(inntektsaar, filter)
            } else {
                logger.info("Opprette forbehandling for etteroppgjoer i forbindelse med kjoering er deaktivert")
            }
        }
    }

    // finn saker med etteroppgjoer og mottatt skatteoppgjoer som skal ha forbehandling
    fun finnOgOpprettForbehandlinger(
        inntektsaar: Int,
        filter: EtteroppgjoerFilter?,
    ) {
        logger.info(
            "Starter oppretting av forbehandling for etteroppgjør med mottatt skatteoppgjør (inntektsår=$inntektsaar, filter=${filter ?: "INGEN"})",
        )

        // for å støtte filter og uten i dev ifm testing
        val etteroppgjoerListe =
            filter?.let { etteroppgjoerService.hentEtteroppgjoerForFilter(it, inntektsaar) }
                ?: etteroppgjoerService.hentEtteroppgjoerMedMottattSkatteoppgjoer(inntektsaar)

        etteroppgjoerListe.forEach { etteroppgjoer ->
            try {
                etteroppgjoerForbehandlingService.opprettEtteroppgjoerForbehandling(
                    etteroppgjoer.sakId,
                    etteroppgjoer.inntektsaar,
                    HardkodaSystembruker.etteroppgjoer,
                )
            } catch (e: Exception) {
                logger.error(
                    "Kunne ikke opprette forbehandling for sakId=${etteroppgjoer.sakId}. Årsak: ${e.message}",
                    e,
                )
            }
        }

        logger.info("Det er opprettet forbehandling for ${etteroppgjoerListe.size} saker for inntektsaar=$inntektsaar.")
    }

    // finn saker som skal ha etteroppgjør for inntektsår og opprett etteroppgjør
    suspend fun finnOgOpprettEtteroppgjoer(inntektsaar: Int) {
        logger.info("Starter oppretting av etteroppgjør for inntektsår $inntektsaar")
        val sakerMedUtbetaling =
            vedtakKlient.hentSakerMedUtbetalingForInntektsaar(
                inntektsaar,
                HardkodaSystembruker.etteroppgjoer,
            )

        sakerMedUtbetaling
            .forEach { sakId -> etteroppgjoerService.opprettEtteroppgjoer(sakId, inntektsaar) }

        logger.info("Opprettet totalt ${sakerMedUtbetaling.size} etteroppgjoer for inntektsaar=$inntektsaar")
    }
}
