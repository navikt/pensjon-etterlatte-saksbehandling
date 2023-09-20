package no.nav.etterlatte.beregning.regler.barnepensjon.sats

import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_2024_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.libs.regler.velgNyesteGyldige
import no.nav.etterlatte.regler.Beregningstall

val historiskeGrunnbeloep =
    GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
        val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
        definerKonstant<BarnepensjonGrunnlag, Grunnbeloep>(
            gjelderFra = grunnbeloepGyldigFra,
            beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
            regelReferanse = RegelReferanse(id = "REGEL-HISTORISKE-GRUNNBELOEP"),
            verdi = grunnbeloep,
        )
    }

val grunnbeloep: Regel<BarnepensjonGrunnlag, Grunnbeloep> =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
    ) velgNyesteGyldige historiskeGrunnbeloep

val soeskenIKullet1967: Regel<BarnepensjonGrunnlag, List<Folkeregisteridentifikator>> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Søskenkull fra grunnlaget",
        finnFaktum = BarnepensjonGrunnlag::soeskenKull,
        finnFelt = { it },
    )

val antallSoeskenIKullet1967 =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner antall søsken i kullet",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-ANTALL-SOESKEN"),
    ) benytter soeskenIKullet1967 med { soesken -> soesken.size }

val avdodeForeldre2024: Regel<BarnepensjonGrunnlag, List<Folkeregisteridentifikator>> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Finner om søker har to avdøde foreldre",
        finnFaktum = BarnepensjonGrunnlag::avdoedeForeldre,
        finnFelt = { it },
    )

val harToAvdodeForeldre2024 =
    RegelMeta(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Finner om barnet har to avdøde foreldre",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-2024-HAR-TO-AVDOEDE"),
    ) benytter avdodeForeldre2024 med { avdoedeForeldre ->
        avdoedeForeldre.size >= 2
    }

val prosentsatsFoersteBarnKonstant1967 =
    definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Prosentsats benyttet for første barn",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-SATS-ETTBARN"),
        verdi = Beregningstall(0.40),
    )

val prosentsatsHvertBarnEnForelderAvdoed2024 =
    definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Prosentsats benyttet for hvert barn når en forelder er død",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-2024-SATS-EN-FORELDER-AVDOED"),
        verdi = Beregningstall(1.00),
    )

val prosentsatsHvertBarnToForeldreAvdoed2024 =
    definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Prosentsats benyttet for hvert barn når to foreldre er døde",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-2024-SATS-TO-FORELDRE-AVDOED"),
        verdi = Beregningstall(2.25),
    )

val beloepHvertBarnEnForelderAvdoed2024 =
    RegelMeta(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Prosentsats benyttet for hvert barn når en forelder er død",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-2024-BELOEP-EN-FORELDER-AVDOED"),
    ) benytter prosentsatsHvertBarnEnForelderAvdoed2024 og grunnbeloep med {
            sats, grunnbeloep ->
        sats.multiply(grunnbeloep.grunnbeloepPerMaaned)
    }

val beloepHvertBarnToForeldreAvdoed2024 =
    RegelMeta(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Prosentsats benyttet for hvert barn når to foreldre er døde",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-2024-BELOEP-TO-FORELDRE-AVDOED"),
    ) benytter prosentsatsHvertBarnToForeldreAvdoed2024 og grunnbeloep med {
            sats, grunnbeloep ->
        sats.multiply(grunnbeloep.grunnbeloepPerMaaned)
    }

val belopForFoersteBarn1967 =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Satser i kr for første barn",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-ETTBARN"),
    ) benytter prosentsatsFoersteBarnKonstant1967 og grunnbeloep med { prosentsatsFoersteBarn, grunnbeloep ->
        prosentsatsFoersteBarn.multiply(grunnbeloep.grunnbeloepPerMaaned)
    }

val prosentsatsEtterfoelgendeBarnKonstant1967 =
    definerKonstant<BarnepensjonGrunnlag, Beregningstall>(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Prosentsats benyttet for etterfølgende barn",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-SATS-FLERBARN"),
        verdi = Beregningstall(0.25),
    )

val belopForEtterfoelgendeBarn1967 =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Satser i kr for etterfølgende barn",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-FLERBARN"),
    ) benytter prosentsatsEtterfoelgendeBarnKonstant1967 og grunnbeloep med { prosentsatsEtterfoelgendeBarn, grunnbeloep ->
        prosentsatsEtterfoelgendeBarn.multiply(grunnbeloep.grunnbeloepPerMaaned)
    }

val barnepensjonSatsRegel1967 =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-UAVKORTET"),
    ) benytter belopForFoersteBarn1967 og belopForEtterfoelgendeBarn1967 og antallSoeskenIKullet1967 med {
            foerstebarnSats, etterfoelgendeBarnSats, antallSoesken ->
        foerstebarnSats
            .plus(etterfoelgendeBarnSats.multiply(antallSoesken))
            .divide(antallSoesken.plus(1))
    }

val barnepensjonSatsRegel2024 =
    RegelMeta(
        gjelderFra = BP_2024_DATO,
        beskrivelse = "Beregn barnepensjon etter 2024-regelverk",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-2024-UAVKORTET"),
    ) benytter harToAvdodeForeldre2024 og beloepHvertBarnEnForelderAvdoed2024 og beloepHvertBarnToForeldreAvdoed2024 med {
            harToAvdodeForeldre2024, beloepEnAvdoedForelder, beloepToAvdoedeForeldre ->
        if (harToAvdodeForeldre2024) {
            beloepToAvdoedeForeldre
        } else {
            beloepEnAvdoedForelder
        }
    }

// Kan populeres basert på feature toggle
val aktuelleBarnepensjonSatsRegler = mutableListOf<Regel<BarnepensjonGrunnlag, Beregningstall>>()

val barnepensjonSatsRegel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Velger nyeste tilgjengelig beregningsregel",
        regelReferanse = RegelReferanse(id = "BP-BEREGNING-UAVKORTET"),
    ) velgNyesteGyldige aktuelleBarnepensjonSatsRegler
