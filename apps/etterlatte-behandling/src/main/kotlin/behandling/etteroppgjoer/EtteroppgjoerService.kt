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

        val skalHaEtteroppgjoer =
            when (etteroppgjoer?.status) {
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER -> true
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER -> true
                else -> false
            }

        return SkalHaEtteroppgjoerResultat(
            skalHaEtteroppgjoer,
            etteroppgjoer,
        )
    }

    // finn saker som skal ha etteroppgjør for inntektsår og opprett etteroppgjør
    fun finnOgOpprettEtteroppgjoer(
        inntektsaar: Int,
        trigger: String = "Manuell",
    ) {
        logger.info("$trigger: Søker etter og opprett etteroppgjoer for inntektsaar=$inntektsaar")

        // TODO: er det flere ting vi må sjekke på en kun utbetaling i inntektsaar
        val sakerMedUtbetaling =
            runBlocking {
                vedtakKlient.hentSakerMedUtbetalingForInntektsaar(
                    inntektsaar,
                    HardkodaSystembruker.etteroppgjoer,
                )
            }

        sakerMedUtbetaling
            .filter { sakId -> dao.hentEtteroppgjoer(sakId, inntektsaar) == null }
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

    fun oppdaterStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        dao.lagerEtteroppgjoer(sakId, inntektsaar, status)
    }

    private fun opprettEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ) {
        // TODO ytterlige sjekker på sak før vi oppretter etteroppgjoer
        dao.lagerEtteroppgjoer(
            sakId,
            inntektsaar,
            EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
        )
        logger.info(
            "Oppretter etteroppgjør for sakId=$sakId for inntektsaar=$inntektsaar med status=${EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER}",
        )
    }
}

data class SkalHaEtteroppgjoerResultat(
    val skalHaEtteroppgjoer: Boolean,
    val etteroppgjoer: Etteroppgjoer?,
)
