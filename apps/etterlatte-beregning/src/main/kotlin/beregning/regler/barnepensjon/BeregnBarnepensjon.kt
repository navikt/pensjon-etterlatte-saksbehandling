package no.nav.etterlatte.beregning.regler.barnepensjon

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
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
import java.time.LocalDate

data class PeriodisertBarnepensjonGrunnlag(
    val soeskenKull: PeriodisertGrunnlag<FaktumNode<List<Folkeregisteridentifikator>>>,
    val avdoedesTrygdetid: PeriodisertGrunnlag<FaktumNode<List<AnvendtTrygdetid>>>,
    val institusjonsopphold: PeriodisertGrunnlag<FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>>,
) : PeriodisertGrunnlag<BarnepensjonGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> {
        val soeskenkullKnekkpunkter =
            soeskenKull
                .finnAlleKnekkpunkter()
                .filter { it.isBefore(BP_2024_DATO) }
                .toSet()

        return soeskenkullKnekkpunkter +
            avdoedesTrygdetid.finnAlleKnekkpunkter() +
            institusjonsopphold.finnAlleKnekkpunkter()
    }

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): BarnepensjonGrunnlag =
        BarnepensjonGrunnlag(
            soeskenKull.finnGrunnlagForPeriode(datoIPeriode),
            avdoedesTrygdetid.finnGrunnlagForPeriode(datoIPeriode),
            institusjonsopphold.finnGrunnlagForPeriode(datoIPeriode),
        )
}

data class BarnepensjonGrunnlag(
    val soeskenKull: FaktumNode<List<Folkeregisteridentifikator>>,
    val avdoedesTrygdetid: FaktumNode<List<AnvendtTrygdetid>>,
    val institusjonsopphold: FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>,
)

@Deprecated("Ikke i bruk lenger")
val deprecatedBeregnBarnepensjon1967Regel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID", versjon = "2"),
    ) benytter barnepensjonSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
        sats.multiply(trygdetidsfaktor)
    }

val beregnBarnepensjon =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden inkludert institusjonsopphold",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID-INSTITUSJON", versjon = "2"),
    ) benytter barnepensjonSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
        sats.multiply(trygdetidsfaktor)
    }

val beregnRiktigBarnepensjonOppMotInstitusjonsopphold =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Sikrer at ytelsen ikke blir større med institusjonsoppholdberegning",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-GUNSTIGHET-INSTITUSJON", versjon = "3"),
    ) benytter beregnBarnepensjon og institusjonsoppholdSatsRegel og brukerHarTellendeInstitusjonsopphold med
        { beregnetBarnepensjon, beregnetBarnepensjonMedInstitusjonsopphold, harInstitusjonsopphold ->
            if (harInstitusjonsopphold) {
                beregnetBarnepensjonMedInstitusjonsopphold.coerceAtMost(beregnetBarnepensjon)
            } else {
                beregnetBarnepensjon
            }
        }

val kroneavrundetBarnepensjonRegelMedInstitusjon =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Gjør en kroneavrunding av barnepensjonen inkludert institusjonsopphold",
        regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING-INSTITUSJON", versjon = "2"),
    ) benytter beregnRiktigBarnepensjonOppMotInstitusjonsopphold med { beregnetBarnepensjon ->
        beregnetBarnepensjon.round(decimals = 0).toInteger()
    }
