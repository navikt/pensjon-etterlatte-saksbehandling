package no.nav.etterlatte.libs.common.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Tilbakekreving(
    val vurdering: TilbakekrevingVurdering,
    val perioder: List<TilbakekrevingPeriode>,
    val kravgrunnlag: Kravgrunnlag,
)

data class TilbakekrevingVurdering(
    val beskrivelse: String?,
    val konklusjon: String?,
    val aarsak: TilbakekrevingAarsak?,
    val aktsomhet: TilbakekrevingVurderingUaktsomhet,
    val hjemmel: TilbakekrevingHjemmel?,
)

data class TilbakekrevingVurderingUaktsomhet(
    val aktsomhet: TilbakekrevingAktsomhet?,
    val reduseringAvKravet: String?,
    val strafferettsligVurdering: String?,
    val rentevurdering: String?,
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

    fun toVedtak() =
        TilbakekrevingsbelopVedtak(
            klasseKode = klasseKode,
            bruttoUtbetaling = bruttoUtbetaling,
            nyBruttoUtbetaling = nyBruttoUtbetaling,
            skatteprosent = skatteprosent,
            beregnetFeilutbetaling = requireNotNull(beregnetFeilutbetaling),
            bruttoTilbakekreving = requireNotNull(bruttoTilbakekreving),
            nettoTilbakekreving = requireNotNull(nettoTilbakekreving),
            skatt = requireNotNull(skatt),
            skyld = requireNotNull(skyld),
            resultat = requireNotNull(resultat),
            tilbakekrevingsprosent = requireNotNull(tilbakekrevingsprosent),
            rentetillegg = requireNotNull(rentetillegg),
        )
}

fun List<KravgrunnlagPeriode>.tilTilbakekrevingPerioder(): List<TilbakekrevingPeriode> {
    return map { periode ->
        val ytelse =
            requireNotNull(periode.grunnlagsbeloep.find { it.klasseType == KlasseType.YTEL }) { "Fant ingen ytelse" }

        val feilkonto =
            requireNotNull(periode.grunnlagsbeloep.find { it.klasseType == KlasseType.FEIL }) { "Fant ikke feilkonto" }

        TilbakekrevingPeriode(
            maaned = periode.periode.fraOgMed,
            ytelse = Tilbakekrevingsbelop.ny(ytelse),
            feilkonto = Tilbakekrevingsbelop.feilkonto(feilkonto),
        )
    }
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

enum class TilbakekrevingHjemmel {
    ULOVFESTET,
    TJUETO_FEMTEN_EN_LEDD_EN,
    TJUETO_FEMTEN_EN_LEDD_TO_FORSETT,
    TJUETO_FEMTEN_EN_LEDD_TO_UAKTSOMT,
    TJUETO_FEMTEN_FEM,
    TJUETO_FEMTEN_SEKS,
    TJUETO_SEKSTEN,
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

/*
* N.B Inneholder ikke alle vedtaksinfo kun det som er nødvendig for Tilbakekrevingskomponent.
*/
data class TilbakekrevingVedtak(
    val vedtakId: Long,
    val fattetVedtak: FattetVedtak,
    val aarsak: TilbakekrevingAarsak,
    val hjemmel: String,
    val kravgrunnlagId: String,
    val kontrollfelt: String,
    val perioder: List<TilbakekrevingPeriodeVedtak>,
)

data class FattetVedtak(
    val saksbehandler: String,
    val enhet: String,
    val dato: LocalDate,
)

data class TilbakekrevingPeriodeVedtak(
    val maaned: YearMonth,
    val ytelse: TilbakekrevingsbelopVedtak,
    val feilkonto: TilbakekrevingsbelopVedtak,
)

data class TilbakekrevingsbelopVedtak(
    val klasseKode: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val skatteprosent: BigDecimal,
    val beregnetFeilutbetaling: Int,
    val bruttoTilbakekreving: Int,
    val nettoTilbakekreving: Int,
    val skatt: Int,
    val skyld: TilbakekrevingSkyld,
    val resultat: TilbakekrevingResultat,
    val tilbakekrevingsprosent: Int,
    val rentetillegg: Int,
)
