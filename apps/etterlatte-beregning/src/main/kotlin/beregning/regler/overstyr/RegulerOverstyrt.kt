package no.nav.etterlatte.beregning.regler.overstyr

import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.regler.FaktumNode
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

data class RegulerManuellBeregningGrunnlag(
    val manueltBeregnetBeloep: FaktumNode<Beregningstall>,
    val forrigeGrunnbeloep: FaktumNode<Beregningstall>,
    val nyttGrunnbeloep: FaktumNode<Beregningstall>,
)

private val historiskeGrunnbeloepIntern =
    GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
        val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
        definerKonstant<Any, Grunnbeloep>(
            gjelderFra = grunnbeloepGyldigFra,
            beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
            regelReferanse = RegelReferanse(id = "REGEL-HISTORISKE-GRUNNBELOEP"),
            verdi = grunnbeloep,
        )
    }

val grunnbeloepUtenGrunnlag: Regel<Any, Grunnbeloep> =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
    ) velgNyesteGyldige historiskeGrunnbeloepIntern

val manueltBeregnetBeloep =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "",
        finnFaktum = RegulerManuellBeregningGrunnlag::manueltBeregnetBeloep,
    ) { it }

val forrigeGrunnbeloep =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "",
        finnFaktum = RegulerManuellBeregningGrunnlag::forrigeGrunnbeloep,
    ) { it }

val nyttGrunnbeloep =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "",
        finnFaktum = RegulerManuellBeregningGrunnlag::nyttGrunnbeloep,
    ) { it }

val regulerOverstyrt =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = ""),
    ) benytter forrigeGrunnbeloep og nyttGrunnbeloep og manueltBeregnetBeloep med { gammelG, nyG, beregnetBeloep ->
        beregnetBeloep.multiply(nyG).divide(gammelG)
    }

val regulerOverstyrtKroneavrundet =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = ""),
    ) benytter regulerOverstyrt med { regulertOverstyrt ->
        regulertOverstyrt.round(decimals = 0).toInteger()
    }
