package no.nav.etterlatte.vedtaksvurdering.grunnlag

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class GrunnlagVersjonValideringTest {
    @AfterEach
    fun reset() = clearAllMocks()

    @Test
    fun `Vilkaar eller beregning er null`() {
        assertDoesNotThrow {
            val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>(relaxed = true)
            val behandling = mockk<DetaljertBehandling>(relaxed = true)
            val trygdetid = mockTrygdetidDtoUtenDiff()
            GrunnlagVersjonValidering.validerVersjon(vilkaarsvurderingDto, null, listOf(trygdetid), behandling)
        }

        assertDoesNotThrow {
            val beregningOgAvkorting = mockk<BeregningOgAvkorting>()
            val behandling = mockk<DetaljertBehandling>(relaxed = true)
            val trygdetid = mockTrygdetidDtoUtenDiff()
            GrunnlagVersjonValidering.validerVersjon(null, beregningOgAvkorting, listOf(trygdetid), behandling)
        }
    }

    @Test
    fun `Ulike versjoner  beregning og vilkårsvurdering - skal kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 2
                every { resultat } returns
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.OPPFYLT,
                        null,
                        LocalDateTime.now(),
                        "saksbehandler",
                    )
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 3
            }

        val trygdetid = mockTrygdetidDtoUtenDiff()
        val behandling = mockk<DetaljertBehandling>(relaxed = true)

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(
                vilkaarsvurderingDto,
                beregningOgAvkorting,
                listOf(trygdetid),
                behandling,
            )
        }
    }

    @Test
    fun `Like versjoner - skal ikke kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
                every { resultat } returns
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.OPPFYLT,
                        null,
                        LocalDateTime.now(),
                        "saksbehandler",
                    )
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid = mockTrygdetidDtoUtenDiff()
        val behandling = mockk<DetaljertBehandling>(relaxed = true)

        assertDoesNotThrow {
            GrunnlagVersjonValidering.validerVersjon(
                vilkaarsvurderingDto,
                beregningOgAvkorting,
                listOf(trygdetid),
                behandling,
            )
        }
    }

    @Test
    fun `Vilkårsvurdering avslag, ikke avhuket vurdereAvoededsTrygdeavtale, ignorerer trygdetid diff`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
                every { resultat } returns
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT,
                        null,
                        LocalDateTime.now(),
                        "saksbehandler",
                    )
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid = mockTrygdetidDtoMedDiff()
        val behandling =
            mockk<DetaljertBehandling>(relaxed = true) {
                every { boddEllerArbeidetUtlandet } returns
                    BoddEllerArbeidetUtlandet(
                        vurdereAvoededsTrygdeavtale = false,
                        begrunnelse = "tom",
                        kilde = Grunnlagsopplysning.Saksbehandler.create("navIdent"),
                        boddEllerArbeidetUtlandet = false,
                    )
            }

        assertDoesNotThrow {
            GrunnlagVersjonValidering.validerVersjon(
                vilkaarsvurderingDto,
                beregningOgAvkorting,
                listOf(trygdetid),
                behandling,
            )
        }
    }

    @Test
    fun `Differanse mellom trygdetid og behandling - skal kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
                every { resultat } returns
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.OPPFYLT,
                        null,
                        LocalDateTime.now(),
                        "saksbehandler",
                    )
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid =
            mockTrygdetidDtoMedDiff()
        val behandling = mockk<DetaljertBehandling>(relaxed = true)

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(
                vilkaarsvurderingDto,
                beregningOgAvkorting,
                listOf(trygdetid),
                behandling,
            )
        }
    }

    @Test
    fun `Differanse mellom trygdetid og behandling paa minst en trygdetid naar det er flere - skal kaste feil`() {
        val vilkaarsvurderingDto =
            mockk<VilkaarsvurderingDto> {
                every { grunnlagVersjon } returns 1
                every { resultat } returns
                    VilkaarsvurderingResultat(
                        VilkaarsvurderingUtfall.OPPFYLT,
                        null,
                        LocalDateTime.now(),
                        "saksbehandler",
                    )
            }

        val beregningOgAvkorting =
            mockk<BeregningOgAvkorting> {
                every { beregning.grunnlagMetadata.versjon } returns 1
            }
        val trygdetid1 =
            mockTrygdetidDtoUtenDiff()
        val trygdetid2 =
            mockTrygdetidDtoMedDiff()
        val behandling = mockk<DetaljertBehandling>(relaxed = true)

        assertThrows<UlikVersjonGrunnlag> {
            GrunnlagVersjonValidering.validerVersjon(
                vilkaarsvurderingDto,
                beregningOgAvkorting,
                listOf(trygdetid1, trygdetid2),
                behandling,
            )
        }
    }
}

private fun mockTrygdetidDtoUtenDiff(): TrygdetidDto =
    mockk<TrygdetidDto> {
        every {
            opplysningerDifferanse
        } returns OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>())
    }

private fun mockTrygdetidDtoMedDiff(): TrygdetidDto =
    mockk<TrygdetidDto> {
        every {
            opplysningerDifferanse
        } returns OpplysningerDifferanse(true, mockk<GrunnlagOpplysningerDto>())
    }
