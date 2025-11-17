package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory

enum class FilterVerdi(
    val filterEn: Boolean,
    val filterTo: Boolean,
) {
    FALSE(false, false),
    TRUE(true, true),
    DONT_CARE(false, true),
}

enum class EtteroppgjoerFilter(
    val harSanksjon: FilterVerdi,
    val harInsitusjonsopphold: FilterVerdi,
    val harOpphoer: FilterVerdi,
    val harAdressebeskyttelseEllerSkjermet: FilterVerdi,
    val harAktivitetskrav: FilterVerdi,
    val harBosattUtland: FilterVerdi,
    val harOverstyrtBeregning: FilterVerdi,
) {
    ENKEL(
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
    ),
    MED_AKTIVITET_OG_SKJERMET(
        harSanksjon = FilterVerdi.FALSE,
        harInsitusjonsopphold = FilterVerdi.FALSE,
        harOpphoer = FilterVerdi.FALSE,
        harAdressebeskyttelseEllerSkjermet = FilterVerdi.DONT_CARE,
        harAktivitetskrav = FilterVerdi.DONT_CARE,
        harBosattUtland = FilterVerdi.FALSE,
        harOverstyrtBeregning = FilterVerdi.FALSE,
    ),
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
        val etteroppgjoersAar = ETTEROPPGJOER_AAR
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
                            etteroppgjoerService.opprettNyttEtteroppgjoer(sakId, inntektsaar) != null
                        }
                    }
                } catch (e: Exception) {
                    // TODO: Fjerne logglinja hvis det spammer mye
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
