package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
    val sakService: SakService,
    val vedtakKlient: VedtakKlient,
) {
    // når vi mottar hendelse fra skatt, sjekk om ident skal ha etteroppgjør
    fun skalHaEtteroppgjoer(
        ident: String,
        inntektsaar: Int,
    ): SkalHaEtteroppgjoerResultat {
        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
        val etteroppgjoer = sak?.let { dao.hentEtteroppgjoer(it.id, inntektsaar) }

        val skalHaEtteroppgjoer = etteroppgjoer?.skalHaEtteroppgjoer() ?: false

        return SkalHaEtteroppgjoerResultat(
            skalHaEtteroppgjoer,
            etteroppgjoer,
        )
    }

    // finn saker som skal ha etteroppgjør for inntektsår og opprett etteroppgjør
    fun finnOgOpprettEtteroppgjoer(inntektsaar: Int) {
        logger.info("Starter oppretting av etteroppgjør for inntektsår $inntektsaar")
        val sakerMedUtbetaling =
            runBlocking {
                vedtakKlient.hentSakerMedUtbetalingForInntektsaar(
                    inntektsaar,
                    HardkodaSystembruker.etteroppgjoer,
                )
            }

        sakerMedUtbetaling
            .forEach { sakId -> opprettEtteroppgjoer(sakId, inntektsaar) }

        logger.info("Opprettet totalt ${sakerMedUtbetaling.size} etteroppgjoer for inntektsaar=$inntektsaar")
    }

    fun hentEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? = dao.hentEtteroppgjoer(sakId, inntektsaar)

    fun hentEtteroppgjoerForStatus(
        status: EtteroppgjoerStatus,
        inntektsaar: Int,
    ): List<Etteroppgjoer> = dao.hentEtteroppgjoerForStatus(status, inntektsaar)

    fun oppdaterEtteroppgjoerStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        dao.lagerEtteroppgjoer(sakId, inntektsaar, status)
    }

    fun opprettEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ) {
        if (dao.hentEtteroppgjoer(sakId, inntektsaar) != null) {
            logger.error("Kan ikke opprette etteroppgjør for sak=$sakId for inntektsaar=$inntektsaar da det allerede eksisterer")
            return
        }

        logger.info(
            "Oppretter etteroppgjør for sakId=$sakId for inntektsaar=$inntektsaar med status=${EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER}",
        )
        dao.lagerEtteroppgjoer(
            sakId,
            inntektsaar,
            EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
        )
    }
}

data class SkalHaEtteroppgjoerResultat(
    val skalHaEtteroppgjoer: Boolean,
    val etteroppgjoer: Etteroppgjoer?,
)
