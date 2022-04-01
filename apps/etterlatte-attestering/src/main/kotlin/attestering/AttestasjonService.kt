package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.AttestertVedtak

class AttestasjonService(
    val attestasjonDao: AttestasjonDao,
) {

    fun opprettAttestertVedtak(vedtak: Vedtak, attestasjon: Attestasjon): AttestertVedtak? =
        attestasjonDao.opprettAttestertVedtak(vedtak, attestasjon)

}