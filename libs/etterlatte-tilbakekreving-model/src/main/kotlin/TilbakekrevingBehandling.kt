package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
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

    fun validerGyldigTilbakekreving() {
        val manglendeFelterVurdering = tilbakekreving.vurdering?.valider() ?: throw ManglerTilbakekrevingsvurdering()
        val manglendeFelterPerioder = tilbakekreving.perioder.flatMap { periode -> periode.valider() }
        if (manglendeFelterVurdering.isNotEmpty() || manglendeFelterPerioder.isNotEmpty()) {
            throw UgyldigeFelterForTilbakekrevingsvurdering(manglendeFelterVurdering, manglendeFelterPerioder)
        }
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
                    vurdering = null,
                    perioder = kravgrunnlag.perioder.tilTilbakekrevingPerioder(),
                    kravgrunnlag = kravgrunnlag,
                ),
        )
    }
}

enum class TilbakekrevingStatus {
    OPPRETTET,
    UNDER_ARBEID,
    VALIDERT,
    FATTET_VEDTAK,
    ATTESTERT,
    UNDERKJENT, ;

    fun kanEndres(): Boolean {
        return this in listOf(OPPRETTET, UNDER_ARBEID, VALIDERT, UNDERKJENT)
    }
}

class UgyldigeFelterForTilbakekrevingsvurdering(
    ugyldigeFelterVurdering: List<String>,
    ugyldigeFelterPerioder: List<String>,
) : ForespoerselException(
        status = 400,
        code = "UGYLDIGE_FELTER",
        detail = "Et eller flere felter mangler gyldig verdi",
        meta =
            mapOf(
                "ugyldigeFelterVurdering" to ugyldigeFelterVurdering,
                "ugyldigeFelterPerioder" to ugyldigeFelterPerioder,
            ),
    )

class ManglerTilbakekrevingsvurdering : ForespoerselException(
    status = 400,
    code = "MANGLER_VURDERING",
    detail = "Tilbakekrevingen mangler vurdering",
)
