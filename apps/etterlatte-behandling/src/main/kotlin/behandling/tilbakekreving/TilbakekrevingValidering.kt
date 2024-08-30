package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.tilbakekreving.JaNei
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBeloepBeholdSvar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVilkaar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import java.time.format.DateTimeFormatter

fun TilbakekrevingBehandling.validerVurderingOgPerioder() {
    val manglendeFelterVurdering = tilbakekreving.vurdering?.valider() ?: throw ManglerTilbakekrevingsvurdering()
    val manglendeFelterPerioder = tilbakekreving.perioder.flatMap { periode -> periode.valider() }
    if (manglendeFelterVurdering.isNotEmpty() || manglendeFelterPerioder.isNotEmpty()) {
        throw UgyldigeFelterForTilbakekrevingsvurdering(manglendeFelterVurdering, manglendeFelterPerioder)
    }
}

private fun TilbakekrevingVurdering.valider(): List<String> {
    val manglendeFelter: MutableList<String> = mutableListOf()

    if (aarsak == null) manglendeFelter.add("Årsak")
    if (forhaandsvarsel == null) manglendeFelter.add("Forhåndsvarsel")
    if (forhaandsvarselDato == null) manglendeFelter.add("Forhåndsvarsel dato / Vedtaksdato")
    if (beskrivelse.isNullOrBlank()) manglendeFelter.add("Beskrivelse")
    if (doedsbosak == null) manglendeFelter.add("Dødsbosak")
    if (foraarsaketAv.isNullOrBlank()) manglendeFelter.add("Forårsaket av")
    if (objektivtVilkaarOppfylt.isNullOrBlank()) manglendeFelter.add("Objektivt vilkår")
    if (vedtak.isNullOrBlank()) manglendeFelter.add("Vedtak")

    validerTilsvar(manglendeFelter)
    validerRettsligGrunnlag(manglendeFelter)
    validerVilkaarsresultat(manglendeFelter)

    return manglendeFelter
}

private fun TilbakekrevingVurdering.validerVilkaarsresultat(manglendeFelter: MutableList<String>) {
    if (vilkaarsresultat == null) {
        manglendeFelter.add("Vilkårsresultat")
    } else {
        if (vilkaarsresultat == TilbakekrevingVilkaar.IKKE_OPPFYLT) {
            validerBeloepIBehold(manglendeFelter)
        }

        if (vilkaarsresultat in
            listOf(
                TilbakekrevingVilkaar.OPPFYLT,
                TilbakekrevingVilkaar.DELVIS_OPPFYLT,
            ) ||
            beloepBehold?.behold == TilbakekrevingBeloepBeholdSvar.BELOEP_I_BEHOLD
        ) {
            if (reduseringAvKravet.isNullOrBlank()) manglendeFelter.add("Redusering av kravet")
            if (foreldet.isNullOrBlank()) manglendeFelter.add("Foreldet")
            if (rentevurdering.isNullOrBlank()) manglendeFelter.add("Rentevurdering")
            if (vurderesForPaatale.isNullOrBlank()) manglendeFelter.add("Vurderes for påtale")
        }
    }
}

private fun TilbakekrevingVurdering.validerBeloepIBehold(manglendeFelter: MutableList<String>) {
    if (beloepBehold?.behold == null) {
        manglendeFelter.add("Beløp i behold")
    } else {
        if (beloepBehold?.beskrivelse.isNullOrBlank()) manglendeFelter.add("Beskrivelse for beløp i behold")
    }
}

private fun TilbakekrevingVurdering.validerRettsligGrunnlag(manglendeFelter: MutableList<String>) {
    when (rettsligGrunnlag) {
        null -> manglendeFelter.add("Rettslig grunnlag")
        TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM ->
            if (burdeBrukerForstaatt.isNullOrBlank()) {
                manglendeFelter.add(
                    "Subjektivt vilkår",
                )
            }

        TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM ->
            if (uaktsomtForaarsaketFeilutbetaling.isNullOrBlank()) {
                manglendeFelter.add(
                    "Subjektivt vilkår",
                )
            }

        TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM ->
            if (burdeBrukerForstaattEllerUaktsomtForaarsaket.isNullOrBlank()) {
                manglendeFelter.add(
                    "Subjektivt vilkår",
                )
            }

        else -> throw RuntimeException("Ugyldig hjemmel for rettslig grunnlag")
    }
}

private fun TilbakekrevingVurdering.validerTilsvar(manglendeFelter: MutableList<String>) {
    if (tilsvar == null) {
        manglendeFelter.add("Tilsvar")
    } else if (tilsvar?.tilsvar == JaNei.JA) {
        if (tilsvar?.dato == null) manglendeFelter.add("Tilsvar dato")
        if (tilsvar?.beskrivelse.isNullOrBlank()) manglendeFelter.add("Tilsvar beskrivelse")
    }
}

private fun TilbakekrevingPeriode.valider(): List<String> {
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

    return manglendeFelter.map { "$it (${maaned.format(DateTimeFormatter.ofPattern("MMMM yyyy"))})" }
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

class ManglerTilbakekrevingsvurdering :
    ForespoerselException(
        status = 400,
        code = "MANGLER_VURDERING",
        detail = "Tilbakekrevingen mangler vurdering",
    )
