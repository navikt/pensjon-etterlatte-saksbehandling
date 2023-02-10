package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import java.time.LocalDate

class Vedtakstidslinje(private val vedtak: List<Vedtak>) {
    fun erLoependePaa(dato: LocalDate): Boolean {
        val iverksatteVedtak = hentIverksatteVedtak()
        if (iverksatteVedtak.isEmpty()) return false

        return hentSenesteVedtakPaaDato(dato)?.tolkVedtak() == TolketVedtak.INNVILGET
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