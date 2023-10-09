package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class Tilbakekreving(
    val id: UUID,
    val status: TilbakekrevingStatus,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val vurdering: TilbakekrevingVurdering,
    val perioder: List<TilbakekrevingPeriode>,
    val kravgrunnlag: Kravgrunnlag,
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
        ) = Tilbakekreving(
            id = UUID.randomUUID(),
            status = TilbakekrevingStatus.OPPRETTET,
            sak = sak,
            opprettet = Tidspunkt.now(),
            vurdering =
                TilbakekrevingVurdering(
                    beskrivelse = null,
                    konklusjon = null,
                    aarsak = null,
                    aktsomhet = null,
                ),
            perioder = kravgrunnlag.perioder.tilTilbakekrevingPerioder(),
            kravgrunnlag = kravgrunnlag,
        )
    }
}

data class TilbakekrevingVurdering(
    val beskrivelse: String?,
    val konklusjon: String?,
    val aarsak: TilbakekrevingAarsak?,
    val aktsomhet: TilbakekrevingAktsomhet?,
)

data class TilbakekrevingPeriode(
    val maaned: YearMonth,
    val ytelse: Tilbakekrevingsbelop,
    val feilkonto: Tilbakekrevingsbelop,
)

data class Tilbakekrevingsbelop(
    val id: UUID,
    val klasseKode: String,
    val klasseType: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val skatteprosent: BigDecimal,
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
                id = UUID.randomUUID(),
                klasseKode = grunnlagsbeloep.klasseKode.value,
                klasseType = grunnlagsbeloep.klasseType.name,
                bruttoUtbetaling = grunnlagsbeloep.bruttoUtbetaling.toInt(),
                nyBruttoUtbetaling = grunnlagsbeloep.nyBruttoUtbetaling.toInt(),
                skatteprosent = grunnlagsbeloep.skatteProsent,
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
                id = UUID.randomUUID(),
                klasseKode = grunnlagsbeloep.klasseKode.value,
                klasseType = grunnlagsbeloep.klasseType.name,
                bruttoUtbetaling = 0,
                nyBruttoUtbetaling = 0,
                skatteprosent = BigDecimal(0.0),
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
        val ytelse = requireNotNull(periode.grunnlagsbeloep.find { it.klasseType == KlasseType.YTEL }) { "Fant ingen ytelse" }

        val feilkonto = requireNotNull(periode.grunnlagsbeloep.find { it.klasseType == KlasseType.FEIL }) { "Fant ikke feilkonto" }

        TilbakekrevingPeriode(
            maaned = periode.periode.fraOgMed,
            ytelse = Tilbakekrevingsbelop.ny(ytelse),
            feilkonto = Tilbakekrevingsbelop.feilkonto(feilkonto),
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

enum class TilbakekrevingAarsak {
    ANNET,
    ARBHOYINNT,
    BEREGNFEIL,
    DODSFALL,
    EKTESKAP,
    FEILREGEL,
    FEILUFOREG,
    FLYTTUTLAND,
    IKKESJEKKYTELSE,
    OVERSETTMLD,
    SAMLIV,
    UTBFEILMOT,
}

enum class TilbakekrevingAktsomhet {
    GOD_TRO,
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
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

class TilbakekrevingHarMangelException(message: String?) : RuntimeException(message)

class TilbakekrevingFinnesIkkeException(message: String?) : RuntimeException(message)

class TilbakekrevingFeilTilstandException(message: String?) : RuntimeException(message)
