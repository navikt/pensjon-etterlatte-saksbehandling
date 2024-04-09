package no.nav.etterlatte.libs.common.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Tilbakekreving(
    val vurdering: TilbakekrevingVurdering?,
    val perioder: List<TilbakekrevingPeriode>,
    val kravgrunnlag: Kravgrunnlag,
)

data class TilbakekrevingVurdering(
    val aarsak: TilbakekrevingAarsak?,
    val beskrivelse: String?,
    val forhaandsvarsel: TilbakekrevingVarsel?,
    val forhaandsvarselDato: LocalDate?,
    val doedsbosak: JaNei?,
    val foraarsaketAv: String?,
    val tilsvar: TilbakekrevingTilsvar?,
    val rettsligGrunnlag: TilbakekrevingHjemmel?,
    val objektivtVilkaarOppfylt: String?,
    val uaktsomtForaarsaketFeilutbetaling: String?,
    val burdeBrukerForstaatt: String?,
    val burdeBrukerForstaattEllerUaktsomtForaarsaket: String?,
    val vilkaarsresultat: TilbakekrevingVilkaar?,
    val beloepBehold: TilbakekrevingBeloepBehold?,
    val reduseringAvKravet: String?,
    val foreldet: String?,
    val rentevurdering: String?,
    val vedtak: String?,
    val vurderesForPaatale: String?,
    val hjemmel: TilbakekrevingHjemmel?,
) {
    fun valider(): List<String> {
        val manglendeFelter: MutableList<String> = mutableListOf()

        if (aarsak == null) manglendeFelter.add("Årsak")
        if (forhaandsvarsel == null) manglendeFelter.add("Forhåndsvarsel")
        if (forhaandsvarselDato == null) manglendeFelter.add("Forhåndsvarsel dato")
        if (beskrivelse.isNullOrBlank()) manglendeFelter.add("Beskrivelse")
        if (doedsbosak == null) manglendeFelter.add("Dødsbosak")
        if (foraarsaketAv.isNullOrBlank()) manglendeFelter.add("Forårsaket av")

        if (tilsvar == null) {
            manglendeFelter.add("Tilsvar")
        } else if (tilsvar.tilsvar == JaNei.JA) {
            if (tilsvar.dato == null) manglendeFelter.add("Tilsvar dato")
            if (tilsvar.beskrivelse.isNullOrBlank()) manglendeFelter.add("Tilsvar beskrivelse")
        }

        if (objektivtVilkaarOppfylt.isNullOrBlank()) manglendeFelter.add("Objektivt vilkår")

        when (rettsligGrunnlag) {
            null -> manglendeFelter.add("Rettslig grunnlag")
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM ->
                if (burdeBrukerForstaatt.isNullOrBlank()) manglendeFelter.add("Subjektivt vilkår")
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM ->
                if (uaktsomtForaarsaketFeilutbetaling.isNullOrBlank()) manglendeFelter.add("Subjektivt vilkår")
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM ->
                if (burdeBrukerForstaattEllerUaktsomtForaarsaket.isNullOrBlank()) manglendeFelter.add("Subjektivt vilkår")
            else -> throw RuntimeException("Ugyldig hjemmel for rettslig grunnlag")
        }

        if (vilkaarsresultat == null) {
            manglendeFelter.add("Vilkårsresultat")
        } else {
            if (vilkaarsresultat == TilbakekrevingVilkaar.IKKE_OPPFYLT) {
                if (beloepBehold?.behold == null) {
                    manglendeFelter.add("Beløp i behold")
                } else {
                    if (beloepBehold.beskrivelse.isNullOrBlank()) manglendeFelter.add("Beskrivelse for beløp i behold")
                }
            }

            if (vilkaarsresultat == TilbakekrevingVilkaar.OPPFYLT ||
                vilkaarsresultat == TilbakekrevingVilkaar.DELVIS_OPPFYLT ||
                beloepBehold?.behold == TilbakekrevingBeloepBeholdSvar.BELOEP_I_BEHOLD
            ) {
                if (reduseringAvKravet.isNullOrBlank()) manglendeFelter.add("Redusering av kravet")
                if (foreldet.isNullOrBlank()) manglendeFelter.add("Foreldet")
                if (rentevurdering.isNullOrBlank()) manglendeFelter.add("Rentevurdering")
                if (vurderesForPaatale.isNullOrBlank()) manglendeFelter.add("Vurderes for påtale")
            }
        }

        if (vedtak.isNullOrBlank()) manglendeFelter.add("Vedtak")

        return manglendeFelter
    }
}

enum class TilbakekrevingVarsel {
    EGET_BREV,
    MED_I_ENDRINGSBREV,
}

enum class JaNei {
    JA,
    NEI,
}

enum class TilbakekrevingVilkaar {
    OPPFYLT,
    DELVIS_OPPFYLT,
    IKKE_OPPFYLT,
}

data class TilbakekrevingTilsvar(
    val tilsvar: JaNei?,
    val dato: LocalDate?,
    val beskrivelse: String?,
)

data class TilbakekrevingBeloepBehold(
    val behold: TilbakekrevingBeloepBeholdSvar?,
    val beskrivelse: String?,
)

enum class TilbakekrevingBeloepBeholdSvar {
    BELOEP_I_BEHOLD,
    BELOEP_IKKE_I_BEHOLD,
}

data class TilbakekrevingPeriode(
    val maaned: YearMonth,
    val ytelse: Tilbakekrevingsbelop,
    val feilkonto: Tilbakekrevingsbelop,
) {
    fun valider(): List<String> {
        val manglendeFelter: MutableList<String> = mutableListOf()
        with(ytelse) {
            if (beregnetFeilutbetaling == null) manglendeFelter.add("Beregnet feilutbetaling")
            if (bruttoTilbakekreving == null) manglendeFelter.add("Brutto tilbakekreving")
            if (nettoTilbakekreving == null) manglendeFelter.add("Netto tilbakekreving")
            if (skatt == null) manglendeFelter.add("Skatt")
            if (skyld == null) manglendeFelter.add("Skyld")
            if (resultat == null) manglendeFelter.add("Resultat")
            if (tilbakekrevingsprosent == null) manglendeFelter.add("Tilbakekrevingsprosent")
            if (rentetillegg == null) manglendeFelter.add("Rentetillegg")
        }

        // Legger på tilhørende periode
        return manglendeFelter.map { "$it (${maaned.format(DateTimeFormatter.ofPattern("MMMM yyyy"))})" }
    }
}

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
        fun ytelse(grunnlagsbeloep: Grunnlagsbeloep) =
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
                bruttoUtbetaling = grunnlagsbeloep.bruttoUtbetaling.toInt(),
                nyBruttoUtbetaling = grunnlagsbeloep.nyBruttoUtbetaling.toInt(),
                skatteprosent = grunnlagsbeloep.skatteProsent,
                beregnetFeilutbetaling = null,
                bruttoTilbakekreving = grunnlagsbeloep.bruttoTilbakekreving.toInt(),
                nettoTilbakekreving = null,
                skatt = null,
                skyld = null,
                resultat = null,
                tilbakekrevingsprosent = null,
                rentetillegg = null,
            )
    }

    fun toYtelseVedtak() =
        TilbakekrevingsbelopYtelseVedtak(
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

    fun toFeilkontoVedtak() =
        TilbakekrevingsbelopFeilkontoVedtak(
            klasseKode = klasseKode,
            bruttoUtbetaling = bruttoUtbetaling,
            nyBruttoUtbetaling = nyBruttoUtbetaling,
            bruttoTilbakekreving = requireNotNull(bruttoTilbakekreving),
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
            ytelse = Tilbakekrevingsbelop.ytelse(ytelse),
            feilkonto = Tilbakekrevingsbelop.feilkonto(feilkonto),
        )
    }
}

enum class TilbakekrevingAarsak {
    OMGJOERING,
    OPPHOER,
    REVURDERING,
    UTBFEILMOT,
    ANNET,
}

enum class TilbakekrevingHjemmel(val kode: String) {
    TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM("22-15-1-1"),
    TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM("22-15-1-2"),
    TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM("22-15-1-1/22-15-1-2"),
    TJUETO_FEMTEN_FEMTE_LEDD("22-15-5"),
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
    val hjemmel: TilbakekrevingHjemmel,
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
    val ytelse: TilbakekrevingsbelopYtelseVedtak,
    val feilkonto: TilbakekrevingsbelopFeilkontoVedtak,
)

data class TilbakekrevingsbelopYtelseVedtak(
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

data class TilbakekrevingsbelopFeilkontoVedtak(
    val klasseKode: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val bruttoTilbakekreving: Int,
)
