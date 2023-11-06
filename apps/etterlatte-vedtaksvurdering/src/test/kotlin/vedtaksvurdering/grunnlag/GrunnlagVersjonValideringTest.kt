package vedtaksvurdering.grunnlag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import no.nav.etterlatte.vedtaksvurdering.grunnlag.GrunnlagVersjonValidering
import no.nav.etterlatte.vedtaksvurdering.grunnlag.UlikVersjonGrunnlag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class GrunnlagVersjonValideringTest {
    @Test
    fun `Vilkaar eller beregning er null`() {
        assertDoesNotThrow {
            val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>(relaxed = true)
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, null)
        }

        assertDoesNotThrow {
            val beregningOgAvkorting = mockk<BeregningOgAvkorting>()
            GrunnlagVersjonValidering.validerVersjon(null, beregningOgAvkorting)
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

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, beregningOgAvkorting)
        }
    }

    @Test
    fun `Like versjoner – skal ikke kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }

        assertDoesNotThrow {
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, beregningOgAvkorting)
        }
    }
}
