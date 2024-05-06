package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class TilbakekrevingBehandling(
    val id: UUID,
    val status: TilbakekrevingStatus,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val tilbakekreving: Tilbakekreving,
    val sendeBrev: Boolean,
) {
    fun underBehandling() =
        when (status) {
            TilbakekrevingStatus.UNDERKJENT,
            TilbakekrevingStatus.UNDER_ARBEID,
            TilbakekrevingStatus.VALIDERT,
            TilbakekrevingStatus.OPPRETTET,
            -> true
            else -> false
        }

    fun gyldigForVedtak() =
        when (status) {
            TilbakekrevingStatus.UNDERKJENT,
            TilbakekrevingStatus.VALIDERT,
            -> true
            else -> false
        }

    fun kanAvbrytes() = underBehandling() || status == TilbakekrevingStatus.FATTET_VEDTAK

    fun underBehandlingEllerFattetVedtak() = underBehandling() || status == TilbakekrevingStatus.FATTET_VEDTAK

    companion object {
        fun ny(
            kravgrunnlag: Kravgrunnlag,
            sak: Sak,
        ) = TilbakekrevingBehandling(
            id = UUID.randomUUID(),
            status = TilbakekrevingStatus.OPPRETTET,
            sak = sak,
            opprettet = Tidspunkt.now(),
            tilbakekreving =
                Tilbakekreving(
                    vurdering = null,
                    perioder = kravgrunnlag.perioder.tilTilbakekrevingPerioder(),
                    kravgrunnlag = kravgrunnlag,
                ),
            sendeBrev = true,
        )
    }
}

enum class TilbakekrevingStatus {
    OPPRETTET,
    UNDER_ARBEID,
    VALIDERT,
    FATTET_VEDTAK,
    ATTESTERT,
    UNDERKJENT,
    AVBRUTT,
    ;

    fun kanEndres(): Boolean {
        return this in listOf(OPPRETTET, UNDER_ARBEID, VALIDERT, UNDERKJENT)
    }
}
