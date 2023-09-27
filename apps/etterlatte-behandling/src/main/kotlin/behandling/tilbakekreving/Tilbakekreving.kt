package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import java.time.YearMonth
import java.util.UUID

data class Tilbakekreving(
    val id: UUID,
    val status: TilbakekrevingStatus,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val perioder: List<TilbakekrevingPeriode>,
    val kravgrunnlag: Kravgrunnlag,
) {
    companion object {
        fun ny(
            kravgrunnlag: Kravgrunnlag,
            sak: Sak,
        ) = Tilbakekreving(
            id = UUID.randomUUID(),
            status = TilbakekrevingStatus.OPPRETTET,
            sak = sak,
            opprettet = Tidspunkt.now(),
            perioder = kravgrunnlag.perioder.tilTilbakekrevingPerioder(),
            kravgrunnlag = kravgrunnlag,
        )
    }
}

data class TilbakekrevingPeriode(
    val maaned: YearMonth,
    val ytelsebeloeper: Tilbakekrevingsbelop,
    val feilkonto: Tilbakekrevingsbelop,
)

data class Tilbakekrevingsbelop(
    val klasseKode: String,
    val klasseType: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val skatteprosent: Double,
    val beregnetFeilutbetaling: Int?,
    val bruttoTilbakekreving: Int?,
    val nettoTilbakekreving: Int?,
    val skatt: Int?,
    val skyld: TilbakekrevingSkyld?,
    val resultat: TilbakekrevingResultat?,
    val tilbakekrevingsprosent: Int?,
    val rentetillegg: Int?,
) {
    companion object {
        fun ny(grunnlagsbeloep: Grunnlagsbeloep) =
            Tilbakekrevingsbelop(
                klasseKode = grunnlagsbeloep.klasseKode.value,
                klasseType = grunnlagsbeloep.klasseType.name,
                bruttoUtbetaling = grunnlagsbeloep.bruttoUtbetaling.toInt(),
                nyBruttoUtbetaling = grunnlagsbeloep.nyBruttoUtbetaling.toInt(),
                skatteprosent = grunnlagsbeloep.skatteProsent.toDouble(),
                beregnetFeilutbetaling = null,
                bruttoTilbakekreving = null,
                nettoTilbakekreving = null,
                skatt = null,
                skyld = grunnlagsbeloep.skyld?.let { TilbakekrevingSkyld.valueOf(it) },
                resultat = grunnlagsbeloep.resultat?.let { TilbakekrevingResultat.valueOf(it) },
                tilbakekrevingsprosent = null,
                rentetillegg = null,
            )

        fun feilkonto(grunnlagsbeloep: Grunnlagsbeloep) =
            Tilbakekrevingsbelop(
                klasseKode = grunnlagsbeloep.klasseKode.value,
                klasseType = grunnlagsbeloep.klasseType.name,
                bruttoUtbetaling = 0,
                nyBruttoUtbetaling = 0,
                skatteprosent = 0.0,
                beregnetFeilutbetaling = null,
                bruttoTilbakekreving = null,
                nettoTilbakekreving = null,
                skatt = null,
                skyld = null,
                resultat = grunnlagsbeloep.resultat?.let { TilbakekrevingResultat.valueOf(it) },
                tilbakekrevingsprosent = null,
                rentetillegg = null,
            )
    }
}

fun List<KravgrunnlagPeriode>.tilTilbakekrevingPerioder(): List<TilbakekrevingPeriode> {
    return map { periode ->
        val ytelse = periode.grunnlagsbeloep.find { it.klasseType == KlasseType.YTEL }
        requireNotNull(ytelse)
        val feilkonto = periode.grunnlagsbeloep.find { it.klasseType == KlasseType.FEIL }
        requireNotNull(feilkonto)

        TilbakekrevingPeriode(
            maaned = periode.periode.fraOgMed,
            ytelsebeloeper = Tilbakekrevingsbelop.ny(ytelse),
            feilkonto = Tilbakekrevingsbelop.feilkonto(feilkonto),
        )
    }
}

enum class TilbakekrevingStatus {
    OPPRETTET,
    UNDER_ARBEID,
}

enum class TilbakekrevingSkyld {
    BRUKER,
    IKKE_FORDELT,
    NAV,
    SKYLDDELING,
}

enum class TilbakekrevingResultat {
    DELVIS_TILBAKEKREV,
    FEILREGISTRERT,
    FORELDET,
    FULL_TILBAKEKREV,
    INGEN_TILBAKEKREV,
}

class KravgrunnlagHarIkkeEksisterendeSakException : RuntimeException()
