package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakLesDao

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
    val pdltjenesterKlient: PdlTjenesterKlient,
) {
    fun skalHaEtteroppgjoer(
        ident: String,
        inntektsaar: Int,
    ): SkalHaEtteroppgjoerResultat {
        val sakType = SakType.OMSTILLINGSSTOENAD
        val sak =
            finnSakerForPerson(ident, sakType).let {
                if (it.isEmpty()) {
                    null
                } else if (it.size == 1) {
                    it.single()
                } else {
                    throw InternfeilException("Flere saker TODO")
                }
            }

        val etteroppgjoer = sak?.let { dao.hentEtteroppgjoer(it.id, inntektsaar) }
        return SkalHaEtteroppgjoerResultat(etteroppgjoer != null, etteroppgjoer)
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

    private fun finnSakerForPerson(
        ident: String,
        sakType: SakType? = null,
    ): List<Sak> =
        runBlocking {
            pdltjenesterKlient
                .hentPdlFolkeregisterIdenter(ident)
                .identifikatorer
                .flatMap { sakLesDao.finnSaker(it.folkeregisterident.value, sakType) }
        }
}

data class SkalHaEtteroppgjoerResultat(
    val skalHaEtteroppgjoer: Boolean,
    val etteroppgjoer: Etteroppgjoer?,
)
