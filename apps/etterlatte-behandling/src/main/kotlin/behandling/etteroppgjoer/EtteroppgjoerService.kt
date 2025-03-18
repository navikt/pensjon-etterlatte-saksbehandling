package no.nav.etterlatte.behandling.etteroppgjoer

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
    fun skalHaEtteroppgjoer(
        ident: String,
        inntektsaar: Int,
    ): SkalHaEtteroppgjoerResultat {
        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
        val etteroppgjoer = sak?.let { dao.hentEtteroppgjoer(it.id, inntektsaar) }
        return SkalHaEtteroppgjoerResultat(etteroppgjoer != null, etteroppgjoer)
    }

    suspend fun finnSakerForEtteroppgjoer(inntektsaar: Int) {
        val sakerMedUtbetaling =
            vedtakKlient.hentSakerMedUtbetalingForInntektsaar(inntektsaar, HardkodaSystembruker.etteroppgjoer)

        sakerMedUtbetaling
            .filter { sakId -> dao.hentEtteroppgjoer(sakId, inntektsaar) == null }
            .forEach { sakId ->
                dao.lagerEtteroppgjoer(
                    sakId,
                    inntektsaar,
                    EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                )
                logger.info(
                    "Oppretter etteroppgjør for inntektsaar=$inntektsaar med " +
                        "status ${EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER} for sakId=$sakId",
                )
            }
    }

    fun oppdaterStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        dao.lagerEtteroppgjoer(sakId, inntektsaar, status)
    }
}

data class SkalHaEtteroppgjoerResultat(
    val skalHaEtteroppgjoer: Boolean,
    val etteroppgjoer: Etteroppgjoer?,
)
