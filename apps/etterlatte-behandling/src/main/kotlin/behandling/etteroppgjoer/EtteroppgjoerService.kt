package no.nav.etterlatte.behandling.etteroppgjoer

class EtteroppgjoerService(
    val etteroppgjoerDao: EtteroppgjoerDao,
) {
    // TODO kan brukes under lytting på skatteoppgjørhendelser
    fun skalHaEtteroppgjoer(ident: String): Boolean {
        val etteroppgjoer = etteroppgjoerDao.hentEtteroppgjoer(ident)
        return etteroppgjoer != null
    }

    fun finnAlleEtteroppgjoer(inntektsaar: Int) {
        // TODO finn alle som har hatt utbetaling i inntektsår og lagre med status EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
    }
}
