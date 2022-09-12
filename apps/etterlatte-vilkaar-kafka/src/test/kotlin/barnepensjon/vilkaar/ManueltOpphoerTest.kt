package barnepensjon.vilkaar

import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ManueltOpphoerTest {

    @Test
    fun `en eller flere opphørsgrunner gir vilkår ikke oppfylt`() {
        val vurderingOpphoerFlereGrunner = vilkaarKanBehandleSakenISystemet(
            listOf("Vi klarer ikke denne saken :(", "annet")
        )
        val vurderingOpphoerEnGrunn = vilkaarKanBehandleSakenISystemet(
            listOf("Søskenjustering er ikke støttet enda så vi drar til PeSys")
        )
        Assertions.assertEquals(vurderingOpphoerFlereGrunner.resultat, VurderingsResultat.IKKE_OPPFYLT)
        Assertions.assertEquals(vurderingOpphoerEnGrunn.resultat, VurderingsResultat.IKKE_OPPFYLT)
    }

    @Test
    fun `ingen opphørsgrunn gir vilkår oppfylt`() {
        val vurderingOpphoerIngenOpphoersgrunn = vilkaarKanBehandleSakenISystemet(listOf())
        Assertions.assertEquals(vurderingOpphoerIngenOpphoersgrunn.resultat, VurderingsResultat.OPPFYLT)
    }
}