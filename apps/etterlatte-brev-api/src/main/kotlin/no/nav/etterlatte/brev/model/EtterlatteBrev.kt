package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.hentinformasjon.beregning.UgyldigBeregningsMetode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.behandling.EtterbetalingPeriodeValg
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonEtterbetaling(
    val inneholderKrav: Boolean?,
    val frivilligSkattetrekk: Boolean?,
    val etterbetalingPeriodeValg: EtterbetalingPeriodeValg?,
)

data class OmstillingsstoenadEtterbetaling(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val etterbetalingsperioder: List<OmstillingsstoenadBeregningsperiode>,
)

data class BarnepensjonBeregning(
    override val innhold: List<Slate.Element>,
    val antallBarn: Int,
    val virkningsdato: LocalDate,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<BarnepensjonBeregningsperiode>,
    val sisteBeregningsperiode: BarnepensjonBeregningsperiode,
    val bruktTrygdetid: TrygdetidMedBeregningsmetode,
    val trygdetid: List<TrygdetidMedBeregningsmetode>,
    val erForeldreloes: Boolean = false,
    val forskjelligTrygdetid: ForskjelligTrygdetid? = null,
) : HarVedlegg

data class BarnepensjonBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    val avdoedeForeldre: List<String?>?,
    val trygdetidForIdent: String?,
    var utbetaltBeloep: Kroner,
) {
    companion object {
        fun fra(beregningsperiode: Beregningsperiode): BarnepensjonBeregningsperiode =
            BarnepensjonBeregningsperiode(
                datoFOM = beregningsperiode.datoFOM,
                datoTOM = beregningsperiode.datoTOM,
                grunnbeloep = beregningsperiode.grunnbeloep,
                utbetaltBeloep = beregningsperiode.utbetaltBeloep,
                antallBarn = beregningsperiode.antallBarn,
                avdoedeForeldre = beregningsperiode.avdoedeForeldre,
                trygdetidForIdent = beregningsperiode.trygdetidForIdent,
            )
    }
}

data class OmstillingsstoenadBeregning(
    override val innhold: List<Slate.Element>,
    val virkningsdato: LocalDate,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val trygdetid: TrygdetidMedBeregningsmetode,
) : HarVedlegg

data class OmstillingsstoenadBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val inntekt: Kroner,
    val aarsinntekt: Kroner,
    val fratrekkInnAar: Kroner,
    val relevantMaanederInnAar: Int,
    val grunnbeloep: Kroner,
    val ytelseFoerAvkorting: Kroner,
    val restanse: Kroner,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val sanksjon: Boolean,
    val institusjon: Boolean,
)

data class TrygdetidMedBeregningsmetode(
    val navnAvdoed: String,
    val trygdetidsperioder: List<Trygdetidsperiode>,
    val beregnetTrygdetidAar: Int,
    val prorataBroek: IntBroek?,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
    val ident: String,
)

data class ForskjelligTrygdetid(
    val foersteTrygdetid: TrygdetidMedBeregningsmetode,
    val foersteVirkningsdato: LocalDate,
    val senereVirkningsdato: LocalDate,
    val harForskjelligMetode: Boolean,
    val erForskjellig: Boolean,
)

data class ForskjelligAvdoedPeriode(
    val foersteAvdoed: Avdoed,
    val senereAvdoed: Avdoed,
    val senereVirkningsdato: LocalDate,
)

data class Trygdetidsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val land: String,
    val opptjeningsperiode: BeregnetTrygdetidGrunnlagDto?,
    val type: TrygdetidType,
)

fun TrygdetidDto.fromDto(
    beregningsMetodeAnvendt: BeregningsMetode,
    beregningsMetodeFraGrunnlag: BeregningsMetode,
    avdoede: List<Avdoed>,
) = this.fromDto(
    beregningsMetodeAnvendt,
    beregningsMetodeFraGrunnlag,
    hentAvdoedNavn(this, avdoede),
)

private fun hentAvdoedNavn(
    trygdetidDto: TrygdetidDto,
    avdoede: List<Avdoed>,
): String {
    if (avdoede.isEmpty()) {
        throw IngenStoetteForUkjentAvdoed()
    }

    return avdoede.find { it.fnr.value == trygdetidDto.ident }?.navn ?: run {
        if (trygdetidDto.beregnetTrygdetid?.resultat?.overstyrt == true) {
            sikkerLogg.warn(
                "Fant ikke avdød fra trygdetid (ident: ${trygdetidDto.ident}) blant avdøde fra " +
                    "grunnlag (${avdoede.joinToString { it.fnr.value }})",
            )
            throw OverstyrtTrygdetidManglerAvdoed()
        } else {
            throw FantIkkeIdentTilTrygdetidBlantAvdoede()
        }
    }
}

fun TrygdetidDto.fromDto(
    beregningsMetodeAnvendt: BeregningsMetode,
    beregningsMetodeFraGrunnlag: BeregningsMetode,
    navnAvdoed: String,
) = TrygdetidMedBeregningsmetode(
    navnAvdoed = navnAvdoed,
    trygdetidsperioder =
        when (beregningsMetodeAnvendt) {
            BeregningsMetode.NASJONAL -> trygdetidGrunnlag.filter { it.bosted == "NOR" }
            BeregningsMetode.PRORATA -> {
                // Kun ta med de som er avtaleland
                trygdetidGrunnlag.filter { it.prorata }
            }

            else -> throw IllegalArgumentException("$beregningsMetodeAnvendt er ikke en gyldig beregningsmetode")
        }.map { grunnlag ->
            Trygdetidsperiode(
                datoFOM = grunnlag.periodeFra,
                datoTOM = grunnlag.periodeTil,
                land = grunnlag.bosted,
                opptjeningsperiode = grunnlag.beregnet,
                type = TrygdetidType.valueOf(grunnlag.type),
            )
        },
    beregnetTrygdetidAar =
        when (beregningsMetodeAnvendt) {
            BeregningsMetode.NASJONAL ->
                beregnetTrygdetid?.resultat?.samletTrygdetidNorge
                    ?: throw ManglerMedTrygdetidVeBrukIBrev()

            BeregningsMetode.PRORATA ->
                beregnetTrygdetid?.resultat?.samletTrygdetidTeoretisk
                    ?: throw ManglerMedTrygdetidVeBrukIBrev()

            BeregningsMetode.BEST -> throw UgyldigBeregningsMetode()
            else -> throw ManglerMedTrygdetidVeBrukIBrev()
        },
    prorataBroek = beregnetTrygdetid?.resultat?.prorataBroek,
    mindreEnnFireFemtedelerAvOpptjeningstiden =
        beregnetTrygdetid
            ?.resultat
            ?.fremtidigTrygdetidNorge
            ?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
    beregningsMetodeFraGrunnlag = beregningsMetodeFraGrunnlag,
    beregningsMetodeAnvendt = beregningsMetodeAnvendt,
    ident = this.ident,
)

enum class FeilutbetalingType {
    FEILUTBETALING_UTEN_VARSEL,
    FEILUTBETALING_MED_VARSEL,
    INGEN_FEILUTBETALING,
}

// Brukes der mangler med trygdetid ikke skal kunne skje men felt likevel er nullable
class ManglerMedTrygdetidVeBrukIBrev :
    UgyldigForespoerselException(
        code = "MANGLER_TRYGDETID_VED_BREV",
        detail = "Trygdetid har mangler ved bruk til brev",
    )

class ManglerAvdoedBruktTilTrygdetid :
    UgyldigForespoerselException(
        code = "MANGLER_AVDOED_INFO_I_BEREGNING",
        detail = "Det mangler avdød i beregning. Utfør beregning på nytt og prøv igjen.",
    )

class FantIkkeIdentTilTrygdetidBlantAvdoede :
    UgyldigForespoerselException(
        code = "FANT_IKKE_TRYGDETID_IDENT_BLANT_AVDOEDE",
        detail = "Ident knyttet til trygdetid er ikke blant avdøde knyttet til sak",
    )

class OverstyrtTrygdetidManglerAvdoed :
    UgyldigForespoerselException(
        code = "OVERSTYRT_TRYGDETID_MANGLER_AVDOED",
        detail = "Overstyrt trygdetid mangler avdød. Trygdetiden må overskrives med ny overstyrt trygdetid",
    )

class IngenStoetteForUkjentAvdoed :
    UgyldigForespoerselException(
        code = "INGEN_STOETTE_FOR_UKJENT_AVDOED",
        detail = "Brevløsningen støtter ikke ukjent avdød",
    )
