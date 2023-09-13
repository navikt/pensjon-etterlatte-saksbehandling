package no.nav.etterlatte.beregning.regler.barnepensjon

import beregning.regler.barnepensjon.erBrukerIInstitusjon
import beregning.regler.barnepensjon.institusjonsoppholdSatsRegel
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.barnepensjonSatsRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate

data class PeriodisertBarnepensjonGrunnlag(
    val soeskenKull: PeriodisertGrunnlag<FaktumNode<List<Folkeregisteridentifikator>>>,
    val avdoedesTrygdetid: PeriodisertGrunnlag<FaktumNode<Beregningstall>>,
    val institusjonsopphold: PeriodisertGrunnlag<FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>>,
    val avdoedeForeldre: PeriodisertGrunnlag<FaktumNode<List<Folkeregisteridentifikator>>>,
    val brukNyttRegelverk: Boolean
) : PeriodisertGrunnlag<BarnepensjonGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> {
        val soeskenkullKnekkpunkter =
            if (brukNyttRegelverk) {
                soeskenKull.finnAlleKnekkpunkter()
                    .filter { it.isBefore(BP_2024_DATO) }
                    .toSet()
            } else {
                soeskenKull.finnAlleKnekkpunkter()
            }

        val avdoedeForeldreKnekkpunkter =
            if (brukNyttRegelverk) {
                avdoedeForeldre.finnAlleKnekkpunkter()
                    .filter { it.isAfter(BP_2024_DATO) }
                    .toSet()
            } else {
                emptySet()
            }

        return soeskenkullKnekkpunkter +
            avdoedesTrygdetid.finnAlleKnekkpunkter() +
            institusjonsopphold.finnAlleKnekkpunkter() +
            avdoedeForeldreKnekkpunkter
    }

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): BarnepensjonGrunnlag {
        return BarnepensjonGrunnlag(
            soeskenKull.finnGrunnlagForPeriode(datoIPeriode),
            avdoedesTrygdetid.finnGrunnlagForPeriode(datoIPeriode),
            institusjonsopphold.finnGrunnlagForPeriode(datoIPeriode),
            avdoedeForeldre.finnGrunnlagForPeriode(datoIPeriode)
        )
    }
}

data class BarnepensjonGrunnlag(
    val soeskenKull: FaktumNode<List<Folkeregisteridentifikator>>,
    val avdoedesTrygdetid: FaktumNode<Beregningstall>,
    val institusjonsopphold: FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>,
    val avdoedeForeldre: FaktumNode<List<Folkeregisteridentifikator>>
) {
    fun harToAvdoedeForeldre(): FaktumNode<Boolean> = FaktumNode(
        avdoedeForeldre.verdi.size >= 2,
        avdoedeForeldre.kilde,
        avdoedeForeldre.beskrivelse
    )
}

val beregnBarnepensjon1967Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID")
) benytter barnepensjonSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    sats.multiply(trygdetidsfaktor)
}

val kroneavrundetBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Gjør en kroneavrunding av barnepensjonen",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter beregnBarnepensjon1967Regel med { beregnetBarnepensjon ->
    beregnetBarnepensjon.round(decimals = 0).toInteger()
}

val barnepensjonSatsMedInstitusjonsopphold = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Sikrer at ytelsen ikke blir større med institusjonsoppholdberegning",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-GUNSTIGHET-INSTITUSJON")
) benytter barnepensjonSatsRegel og institusjonsoppholdSatsRegel med { standardSats, institusjonsoppholdSats ->
    institusjonsoppholdSats.coerceAtMost(standardSats)
}

val barnepensjonSats = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Bruker institusjonsoppholdberegning hvis barnet er i institusjon",
    regelReferanse = RegelReferanse("BP-BEREGNING-1967-KANSKJEANVENDINSTITUSJON")
) benytter barnepensjonSatsRegel og barnepensjonSatsMedInstitusjonsopphold og erBrukerIInstitusjon med {
        satsIkkeInstitusjonsopphold,
        satsInstitusjonsopphold,
        harInstitusjonshopphold ->
    if (harInstitusjonshopphold) satsInstitusjonsopphold else satsIkkeInstitusjonsopphold
}

val beregnBarnepensjon1967RegelMedInstitusjon = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden inkludert institusjonsopphold",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID-INSTITUSJON")
) benytter barnepensjonSats og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    sats.multiply(trygdetidsfaktor)
}

val kroneavrundetBarnepensjonRegelMedInstitusjon = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Gjør en kroneavrunding av barnepensjonen inkludert institusjonsopphold",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING-INSTITUSJON")
) benytter beregnBarnepensjon1967RegelMedInstitusjon med { beregnetBarnepensjon ->
    beregnetBarnepensjon.round(decimals = 0).toInteger()
}