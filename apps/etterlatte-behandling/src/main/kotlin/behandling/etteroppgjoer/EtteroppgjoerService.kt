package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
    val sakService: SakService,
) {
    fun skalHaEtteroppgjoer(
        ident: String,
        inntektsaar: Int,
    ): SkalHaEtteroppgjoerResultat {
        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
        val etteroppgjoer = sak?.let { dao.hentEtteroppgjoer(it.id, inntektsaar) }
        return SkalHaEtteroppgjoerResultat(etteroppgjoer != null, etteroppgjoer)
    }

    fun finnSakerForEtteroppgjoer(inntektsaar: Int) {
        // TODO finn alle som har hatt utbetaling i inntektsår og lagre med status EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER

        // TODO finn alle saker som har hatt utbetaling i inntektsår

        // TODO opprett etteroppgjoer med status VENTER_PAA_SKATTEOPPGJOER

        // TODO lagre
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
