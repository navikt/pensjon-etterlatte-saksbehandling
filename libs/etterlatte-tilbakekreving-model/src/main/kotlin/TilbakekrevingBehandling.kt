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
) {
    fun underBehandling() =
        when (status) {
            TilbakekrevingStatus.UNDERKJENT,
            TilbakekrevingStatus.UNDER_ARBEID,
            TilbakekrevingStatus.OPPRETTET,
            -> true
            else -> false
        }

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
                    vurdering =
                        TilbakekrevingVurdering(
                            beskrivelse = null,
                            konklusjon = null,
                            aarsak = null,
                            aktsomhet =
                                TilbakekrevingVurderingUaktsomhet(
                                    aktsomhet = null,
                                    reduseringAvKravet = null,
                                    strafferettsligVurdering = null,
                                    rentevurdering = null,
                                ),
                            hjemmel = null,
                        ),
                    perioder = kravgrunnlag.perioder.tilTilbakekrevingPerioder(),
                    kravgrunnlag = kravgrunnlag,
                ),
        )
    }
}

enum class TilbakekrevingStatus {
    OPPRETTET,
    UNDER_ARBEID,
    FATTET_VEDTAK,
    ATTESTERT,
    UNDERKJENT, ;

    fun kanEndres(): Boolean {
        return this in listOf(OPPRETTET, UNDER_ARBEID, FATTET_VEDTAK, ATTESTERT, UNDERKJENT)
    }
}
