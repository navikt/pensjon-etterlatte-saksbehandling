package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakLesDao

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
) {
    // TODO kan brukes under lytting på skatteoppgjørhendelser
    fun skalHaEtteroppgjoer(
        ident: String,
        inntektsaar: Int,
    ): Boolean {
        val sak =
            sakLesDao.finnSaker(ident, SakType.OMSTILLINGSSTOENAD).singleOrNull()
                ?: throw InternfeilException("Fant ikke sak med ident") // TODO sikkerlogg
        val etteroppgjoer = dao.hentEtteroppgjoer(sak.id, inntektsaar)
        return etteroppgjoer != null
    }

    fun finnAlleEtteroppgjoerOgLagre(inntektsaar: Int) {
        // TODO finn alle som har hatt utbetaling i inntektsår og lagre med status EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
    }

    fun oppdaterStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        dao.lagerEtteroppgjoer(sakId, inntektsaar, status)
    }
}
