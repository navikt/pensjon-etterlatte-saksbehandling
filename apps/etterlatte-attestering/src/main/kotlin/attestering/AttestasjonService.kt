package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.AttestertVedtak

class AttestasjonService(
    val attestasjonDao: AttestasjonDao,
) {

    // TODO: sjekke at vedtaket ikke eksisterer først
    // TODO: sjekke at vedtak ikke allerede er attestert før det attesteres

    fun opprettAttestertVedtak(vedtak: Vedtak, attestasjon: Attestasjon): AttestertVedtak =
        attestasjonDao.opprettAttestertVedtak(vedtak, attestasjon)

    fun opprettVedtakUtenAttestering(vedtak: Vedtak): AttestertVedtak? =
        attestasjonDao.opprettMottattVedtak(vedtak)

    fun attesterVedtak(vedtakId: String, attestasjon: Attestasjon): AttestertVedtak? =
        attestasjonDao.attesterVedtak(vedtakId, attestasjon)

}