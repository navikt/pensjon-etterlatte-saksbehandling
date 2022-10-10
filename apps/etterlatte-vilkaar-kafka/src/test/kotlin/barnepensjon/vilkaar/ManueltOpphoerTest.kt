package barnepensjon.vilkaar

import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ManueltOpphoerTest {

    @Test
    fun `en eller flere opphoersgrunner gir vilkaar ikke oppfylt`() {
        val vurderingOpphoerFlereGrunner = vilkaarKanBehandleSakenISystemet(
            listOf(ManueltOpphoerAarsak.SOESKEN_DOED, ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED),
            null
        )
        val vurderingOpphoerEnGrunnfritekst = vilkaarKanBehandleSakenISystemet(
            emptyList(),
            "Vi vil ikke behandle denne"
        )
        val vurderingOpphoerVurderingOgFritekst = vilkaarKanBehandleSakenISystemet(
            listOf(ManueltOpphoerAarsak.UTFLYTTING_FRA_NORGE),
            "Denne hører hjemme i PeSys"
        )
        Assertions.assertEquals(vurderingOpphoerFlereGrunner.resultat, VurderingsResultat.IKKE_OPPFYLT)
        Assertions.assertEquals(vurderingOpphoerEnGrunnfritekst.resultat, VurderingsResultat.IKKE_OPPFYLT)
        Assertions.assertEquals(vurderingOpphoerVurderingOgFritekst.resultat, VurderingsResultat.IKKE_OPPFYLT)
    }

    @Test
    fun `ingen opphoersgrunn gir vilkaar oppfylt`() {
        val vurderingOpphoerIngenOpphoersgrunn = vilkaarKanBehandleSakenISystemet(listOf(), null)
        Assertions.assertEquals(vurderingOpphoerIngenOpphoersgrunn.resultat, VurderingsResultat.OPPFYLT)
    }
}