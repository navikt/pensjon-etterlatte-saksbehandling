package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurderingUaktsomhet
import no.nav.etterlatte.libs.common.tilbakekreving.tilTilbakekrevingPerioder
import java.util.UUID

fun TilbakekrevingBehandling.tilStatistikkTilbakekreving(tilbakekrevingHendelseType: TilbakekrevingHendelseType): StatistikkTilbakekreving {
    return StatistikkTilbakekreving(
        id = this.id,
        sak = this.sak,
        behandlingOpprettet = this.opprettet,
        soeknadMottattDato = this.opprettet,
        status = this.status.name,
        type = tilbakekrevingHendelseType.name,
    )
}

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
    UNDERKJENT,
}

class TilbakekrevingHarMangelException(message: String?) : RuntimeException(message)

class TilbakekrevingFinnesIkkeException(message: String?) : RuntimeException(message)

class TilbakekrevingFeilTilstandException(message: String?) : RuntimeException(message)
