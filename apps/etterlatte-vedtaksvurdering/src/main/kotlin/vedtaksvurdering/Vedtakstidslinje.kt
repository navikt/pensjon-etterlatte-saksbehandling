package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import java.time.LocalDate

class Vedtakstidslinje(private val vedtak: List<Vedtak>) {
    private val iverksatteVedtak = hentIverksatteVedtak()

    fun erLoependePaa(dato: LocalDate): LoependeYtelseDTO {
        if (iverksatteVedtak.isEmpty()) return LoependeYtelseDTO(false, dato)

        val erLoepende = hentSenesteVedtakPaaDato(dato)?.tolkVedtak() == TolketVedtak.INNVILGET
        return LoependeYtelseDTO(
            erLoepende = erLoepende,
            dato = if (erLoepende) foersteMuligeVedtaksdag(dato) else dato
        )
    }

    private fun hentSenesteVedtakPaaDato(dato: LocalDate): Vedtak? = iverksatteVedtak
        .filter { it.virkningsDato?.isAfter(foersteMuligeVedtaksdag(dato))?.not() ?: false }
        .maxByOrNull { it.datoattestert!! }

    private fun hentIverksatteVedtak(): List<Vedtak> = vedtak.filter { it.vedtakStatus === VedtakStatus.IVERKSATT }

    private fun foersteMuligeVedtaksdag(fraDato: LocalDate): LocalDate {
        val foersteVirkningsdato = iverksatteVedtak.minBy {
            it.datoattestert ?: throw Error("Kunne ikke finne datoattestert paa vedtak")
        }.virkningsDato ?: throw Error("Fant ikke vikrningsdato på det iverksatte vedtaket")
        return maxOf(foersteVirkningsdato, fraDato)
    }
}