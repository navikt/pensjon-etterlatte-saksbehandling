package no.nav.etterlatte

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import java.time.LocalDate

class Vedtakstidslinje(private val vedtak: List<Vedtak>) {
    fun erLoependePaa(dato: LocalDate): Boolean {
        val iverksatteVedtak = hentIverksatteVedtak()
        if (iverksatteVedtak.isEmpty()) return false

        return hentSenesteVedtakPaaDato(dato)?.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING
    }

    private fun hentSenesteVedtakPaaDato(dato: LocalDate): Vedtak? {
        val foersteVirkningsdato = hentIverksatteVedtak().minBy { it.datoattestert!! }.virkningsDato!!
        val foersteMuligeVedtaksdag = maxOf(foersteVirkningsdato, dato)

        return hentIverksatteVedtak()
            .filter { !it.virkningsDato!!.isAfter(foersteMuligeVedtaksdag) }
            .maxByOrNull { it.datoattestert!! }
    }

    private fun hentIverksatteVedtak(): List<Vedtak> {
        return vedtak.filter { it.vedtakStatus === VedtakStatus.IVERKSATT }
    }
}