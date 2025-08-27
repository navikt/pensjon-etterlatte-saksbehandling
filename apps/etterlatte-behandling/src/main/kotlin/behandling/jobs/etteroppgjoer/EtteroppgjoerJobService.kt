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
    val harSanksjon: Boolean,
    val harInsitusjonsopphold: Boolean,
    val harOpphoer: Boolean,
    val harBosattUtland: Boolean,
    val harAdressebeskyttelse: Boolean,
    val harUtbetaling: Boolean,
    val harAktivitetskrav: Boolean,
) {
    ENKEL(false, false, false, false),
    MED_SANKSJON(true, false, false, false),
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
                if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_PERIODISK_JOBB, false)) {
                    logger.info("Starter periodiske jobber for etteroppgjoer")
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
            finnOgOpprettEtteroppgjoer(inntektsaar)
            finnOgOpprettForbehandlinger(inntektsaar, filter)
        }
    }

    // finn saker med etteroppgjoer og mottatt skatteoppgjoer som skal ha forbehandling
    fun finnOgOpprettForbehandlinger(
        inntektsaar: Int,
        filter: EtteroppgjoerFilter?,
    ) {
        val status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
        logger.info(
            "Starter oppretting av forbehandling for etteroppgjør (inntektsår=$inntektsaar, status=$status, filter=${filter ?: "INGEN"})",
        )

        val etteroppgjoerListe =
            filter
                ?.let { etteroppgjoerService.hentEtteroppgjoerForFilter(it, inntektsaar) }
                ?: etteroppgjoerService.hentEtteroppgjoerForStatus(status, inntektsaar)

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

        logger.info("Ferdig. Opprettet forbehandling for ${etteroppgjoerListe.size} saker.")
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
