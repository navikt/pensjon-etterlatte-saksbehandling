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
    fun oppdaterKravgrunnlag(kravgrunnlag: Kravgrunnlag) =
        this.copy(
            tilbakekreving =
                this.tilbakekreving.copy(
                    perioder = kravgrunnlag.perioder.tilTilbakekrevingPerioder(),
                    kravgrunnlag = kravgrunnlag,
                ),
        )

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

    fun kanEndres(): Boolean = this in listOf(OPPRETTET, UNDER_ARBEID, VALIDERT, UNDERKJENT)
}
