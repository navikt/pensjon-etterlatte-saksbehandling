package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate

class Vedtakstidslinje(private val vedtak: List<Vedtak>) {
    private val iverksatteVedtak = hentIverksatteVedtak()

    fun erLoependePaa(dato: LocalDate): LoependeYtelse {
        if (iverksatteVedtak.isEmpty()) return LoependeYtelse(false, dato)

        val erLoepende =
            hentSenesteVedtakPaaDato(dato)?.type in listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING)
        return LoependeYtelse(
            erLoepende = erLoepende,
            dato = if (erLoepende) foersteMuligeVedtaksdag(dato) else dato
        )
    }

    private fun hentSenesteVedtakPaaDato(dato: LocalDate): Vedtak? = iverksatteVedtak
        .filter { it.virkningstidspunkt.atDay(1).isAfter(foersteMuligeVedtaksdag(dato)).not() }
        .maxByOrNull { it.attestasjon?.tidspunkt!! }

    private fun hentIverksatteVedtak(): List<Vedtak> = vedtak.filter { it.status === VedtakStatus.IVERKSATT }

    private fun foersteMuligeVedtaksdag(fraDato: LocalDate): LocalDate {
        val foersteVirkningsdato = iverksatteVedtak.minBy {
            it.attestasjon?.tidspunkt ?: throw Error("Kunne ikke finne datoattestert paa vedtak")
        }.virkningstidspunkt.atDay(1)
        return maxOf(foersteVirkningsdato, fraDato)
    }
}