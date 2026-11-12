package no.nav.etterlatte.modules.beregning.avkorting

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.velgNyesteGyldige
import no.nav.etterlatte.modules.beregning.avkorting.regler.InntektAvkortingGrunnlagWrapper
import no.nav.etterlatte.modules.beregning.avkorting.regler.grunnbeloep
import no.nav.etterlatte.modules.beregning.grunnbeloep.GrunnbeloepRepository

fun mockGrunnbeloepForAvkorting(historiskeGrunnbeloep: List<Grunnbeloep>) {
    val testRegel =
        RegelMeta(
            gjelderFra = grunnbeloep.gjelderFra,
            beskrivelse = grunnbeloep.beskrivelse,
            regelReferanse = grunnbeloep.regelReferanse,
        ) velgNyesteGyldige
            historiskeGrunnbeloep.map { beloep ->
                definerKonstant<InntektAvkortingGrunnlagWrapper, Grunnbeloep>(
                    gjelderFra = beloep.dato.atDay(1),
                    beskrivelse = "Grunnbeløp gyldig fra ${beloep.dato.atDay(1)}",
                    regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
                    verdi = beloep,
                )
            }

    mockkObject(GrunnbeloepRepository, grunnbeloep)
    every { GrunnbeloepRepository.historiskeGrunnbeloep } returns historiskeGrunnbeloep
    every { grunnbeloep.accept(any()) } answers { testRegel.accept(firstArg()) }
    every { grunnbeloep.anvend(any(), any()) } answers { testRegel.anvend(firstArg(), secondArg()) }
}

fun unmockGrunnbeloepForAvkorting() {
    unmockkObject(GrunnbeloepRepository, grunnbeloep)
}
