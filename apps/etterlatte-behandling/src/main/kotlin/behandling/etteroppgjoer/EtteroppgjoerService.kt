package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
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
        val etteroppgjoer = sak?.let { dao.hentEtteroppgjoerForInntektsaar(it.id, inntektsaar) }

        val skalHaEtteroppgjoer = etteroppgjoer?.skalHaEtteroppgjoer() ?: false

        return SkalHaEtteroppgjoerResultat(
            skalHaEtteroppgjoer,
            etteroppgjoer,
        )
    }

    fun hentAlleAktiveEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> = dao.hentAlleAktiveEtteroppgjoerForSak(sakId)

    fun hentEtteroppgjoerForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? = dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar)

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
        if (dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar) != null) {
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
