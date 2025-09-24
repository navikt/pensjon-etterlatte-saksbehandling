package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory
import java.time.YearMonth

enum class EtteroppgjoerFilter(
    val harSanksjon: Boolean,
    val harInsitusjonsopphold: Boolean,
    val harOpphoer: Boolean,
    val harAdressebeskyttelseEllerSkjermet: Boolean,
    val harAktivitetskrav: Boolean,
    val harBosattUtland: Boolean,
    val harOverstyrtBeregning: Boolean,
) {
    ENKEL(false, false, false, false, false, false, false),
}

@OptIn(DelicateCoroutinesApi::class)
class OpprettEtteroppgjoerJobService(
    private val etteroppgjoerService: EtteroppgjoerService,
    private val vedtakKlient: VedtakKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_PERIODISK_JOBB, false)) {
            logger.info("Starter periodiske jobber for etteroppgjoer")
            runBlocking {
                startEtteroppgjoerKjoering()
            }
        } else {
            logger.info("Periodisk jobber for etteroppgjoer er deaktivert")
        }
    }

    suspend fun startEtteroppgjoerKjoering() {
        val etteroppgjoersAar = YearMonth.now().year - 1
        finnOgOpprettEtteroppgjoer(etteroppgjoersAar)
    }

    // finn saker som skal ha etteroppgjør for inntektsår og opprett etteroppgjør
    suspend fun finnOgOpprettEtteroppgjoer(inntektsaar: Int) {
        logger.info("Starter oppretting av etteroppgjør for inntektsår $inntektsaar")
        val sakerMedUtbetaling =
            vedtakKlient.hentSakerMedUtbetalingForInntektsaar(
                inntektsaar,
                HardkodaSystembruker.etteroppgjoer,
            )

        val antallOpprettet =
            sakerMedUtbetaling.count { sakId ->
                try {
                    inTransaction {
                        runBlocking {
                            etteroppgjoerService.opprettEtteroppgjoer(sakId, inntektsaar) != null
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Feil ved oppretting av etteroppgjør", e)
                    false
                }
            }

        logger.info(
            "Opprettet totalt $antallOpprettet av ${sakerMedUtbetaling.size} " +
                "etteroppgjoer for inntektsaar=$inntektsaar",
        )
    }
}
