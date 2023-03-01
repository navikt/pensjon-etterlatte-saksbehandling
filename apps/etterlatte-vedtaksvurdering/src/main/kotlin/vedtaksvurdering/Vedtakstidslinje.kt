package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
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

internal enum class TolketVedtak {
    INNVILGET,
    OPPHOER
}

/**
 * TODO ai 10.02.2023: Se på denne logikken og fiks tolking av vedtak
 * */
internal fun Vedtak.tolkVedtak(): TolketVedtak = when (this.behandlingType) {
    BehandlingType.FØRSTEGANGSBEHANDLING -> TolketVedtak.INNVILGET
    BehandlingType.OMREGNING -> TolketVedtak.INNVILGET
    BehandlingType.REVURDERING -> TolketVedtak.OPPHOER
    BehandlingType.MANUELT_OPPHOER -> TolketVedtak.OPPHOER
}