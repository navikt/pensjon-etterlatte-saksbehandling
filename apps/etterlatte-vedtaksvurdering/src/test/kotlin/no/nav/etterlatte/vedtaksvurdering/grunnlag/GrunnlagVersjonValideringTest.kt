package no.nav.etterlatte.vedtaksvurdering.grunnlag

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class GrunnlagVersjonValideringTest {
    @AfterEach
    fun reset() = clearAllMocks()

    @Test
    fun `Vilkaar eller beregning er null`() {
        assertDoesNotThrow {
            val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>(relaxed = true)
            val trygdetid = mockTrygdetidDtoUtenDiff()
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, null, listOf(trygdetid))
        }

        assertDoesNotThrow {
            val beregningOgAvkorting = mockk<BeregningOgAvkorting>()
            val trygdetid = mockTrygdetidDtoUtenDiff()
            GrunnlagVersjonValidering.validerVersjon(null, beregningOgAvkorting, listOf(trygdetid))
        }
    }

    @Test
    fun `Ulike versjoner - skal kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 2
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 3
            }

        val trygdetid = mockTrygdetidDtoUtenDiff()

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, beregningOgAvkorting, listOf(trygdetid))
        }
    }

    @Test
    fun `Like versjoner - skal ikke kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid = mockTrygdetidDtoUtenDiff()

        assertDoesNotThrow {
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, beregningOgAvkorting, listOf(trygdetid))
        }
    }

    @Test
    fun `Differanse mellom trygdetid og behandling - skal kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid =
            mockTrygdetidDtoMedDiff()

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, beregningOgAvkorting, listOf(trygdetid))
        }
    }

    @Test
    fun `Differanse mellom trygdetid og behandling paa minst en trygdetid naar det er flere - skal kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid1 =
            mockTrygdetidDtoUtenDiff()
        val trygdetid2 =
            mockTrygdetidDtoMedDiff()

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, beregningOgAvkorting, listOf(trygdetid1, trygdetid2))
        }
    }
}

private fun mockTrygdetidDtoUtenDiff(): TrygdetidDto {
    return mockk<TrygdetidDto> {
        every {
            opplysningerDifferanse
        } returns OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>())
    }
}

private fun mockTrygdetidDtoMedDiff(): TrygdetidDto {
    return mockk<TrygdetidDto> {
        every {
            opplysningerDifferanse
        } returns OpplysningerDifferanse(true, mockk<GrunnlagOpplysningerDto>())
    }
}
