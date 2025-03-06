package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.sak.SakId

class EtteroppgjoerService(
    val etteroppgjoerDao: EtteroppgjoerDao,
) {
    fun skalHaEtteroppgjoer(ident: String): Boolean {
        val etteroppgjoer = etteroppgjoerDao.hentEtteroppgjoer(ident)
        return etteroppgjoer != null
    }

    fun oppdaterStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        etteroppgjoerDao.oppdaterEtteroppgjoerStatus(sakId, inntektsaar, status)
    }

    fun finnAlleEtteroppgjoer(inntektsaar: Int) {
        // TODO finn alle som har hatt utbetaling i inntektsår og lagre
    }
}
