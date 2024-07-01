package no.nav.etterlatte.beregning.regler.omstillingstoenad

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.omstillingstoenadSatsRegel
import no.nav.etterlatte.beregning.regler.omstillingstoenad.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate

data class Avdoed(
    val trygdetid: SamletTrygdetidMedBeregningsMetode,
)

data class PeriodisertOmstillingstoenadGrunnlag(
    val avdoed: PeriodisertGrunnlag<FaktumNode<Avdoed>>,
    val institusjonsopphold: PeriodisertGrunnlag<FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>>,
) : PeriodisertGrunnlag<OmstillingstoenadGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> =
        avdoed.finnAlleKnekkpunkter() +
            institusjonsopphold.finnAlleKnekkpunkter()

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): OmstillingstoenadGrunnlag =
        OmstillingstoenadGrunnlag(
            avdoed.finnGrunnlagForPeriode(datoIPeriode),
            institusjonsopphold.finnGrunnlagForPeriode(datoIPeriode),
        )
}

data class OmstillingstoenadGrunnlag(
    val avdoed: FaktumNode<Avdoed>,
    val institusjonsopphold: FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>,
)

val beregnOmstillingstoenadRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-REDUSER-MOT-TRYGDETID"),
    ) benytter omstillingstoenadSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
        sats.multiply(trygdetidsfaktor)
    }

val OmstillingstoenadSatsMedInstitusjonsopphold =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Sikrer at ytelsen ikke blir større med institusjonsoppholdberegning",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-GUNSTIGHET-INSTITUSJON"),
    ) benytter omstillingstoenadSatsRegel og institusjonsoppholdSatsRegelOMS med { standardSats, institusjonsoppholdSats ->
        institusjonsoppholdSats.coerceAtMost(standardSats)
    }

val omstillingstoenadSats =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Bruker institusjonsoppholdberegning hvis bruker er i institusjon",
        regelReferanse = RegelReferanse("OMS-BEREGNING-KANSKJEANVENDINSTITUSJON"),
    ) benytter omstillingstoenadSatsRegel og OmstillingstoenadSatsMedInstitusjonsopphold og erBrukerIInstitusjonOMS med {
            satsIkkeInstitusjonsopphold,
            satsInstitusjonsopphold,
            harInstitusjonshopphold,
        ->
        if (harInstitusjonshopphold) satsInstitusjonsopphold else satsIkkeInstitusjonsopphold
    }

val kroneavrundetOmstillingstoenadRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Gjør en kroneavrunding av beregningen",
        regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING"),
    ) benytter beregnOmstillingstoenadRegel med { beregnetOmstillingstoenad ->
        beregnetOmstillingstoenad.round(decimals = 0).toInteger()
    }

val beregnOmstillingstoenadRegelMedInstitusjon =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Bruker institusjonsoppholdberegning hvis bruker er i institusjon",
        regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-REDUSER-MOT-TRYGDETID"),
    ) benytter omstillingstoenadSats og trygdetidsFaktor og grunnbeloep med { sats, trygdetidsfaktor, grunnbeloep ->
        val redusertYtelseMotTrygdetidsfaktor = sats.multiply(trygdetidsfaktor)
        val tiProsentAvG = grunnbeloep.grunnbeloepPerMaaned.times(0.10)
        if (redusertYtelseMotTrygdetidsfaktor.toInteger() < tiProsentAvG) {
            Beregningstall(tiProsentAvG)
        } else {
            redusertYtelseMotTrygdetidsfaktor
        }
    }

val kroneavrundetOmstillingstoenadRegelMedInstitusjon =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Gjør en kroneavrunding av omstillingstønad inkludert institusjonsopphold",
        regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING-INSTITUSJON"),
    ) benytter beregnOmstillingstoenadRegelMedInstitusjon med { beregnetOmstillingstoenad ->
        beregnetOmstillingstoenad.round(decimals = 0).toInteger()
    }
