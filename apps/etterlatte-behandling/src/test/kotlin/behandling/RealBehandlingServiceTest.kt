package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.behandling.Opplysning
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import java.util.*

internal class RealBehandlingServiceTest {

    @Test
    fun hentBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val opplysningerMock = mockk<OpplysningDao>()


        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, NoOpVilkaarKlient())

        val id = UUID.randomUUID()

        val opplysninger = listOf(
            Opplysning(UUID.randomUUID(), Opplysning.Saksbehandler("S01"), "trygdetid", objectMapper.createObjectNode(), objectMapper.createObjectNode()),
            Opplysning(UUID.randomUUID(), Opplysning.Saksbehandler("S01"), "medlemskap", objectMapper.createObjectNode(), objectMapper.createObjectNode()),
        )

        every { behandlingerMock.hent(id) } returns Behandling(id, 1, emptyList(), null, null, false)
        every { opplysningerMock.finnOpplysningerIBehandling(id) } returns opplysninger
        Assertions.assertEquals(2,  sut.hentBehandling(id).grunnlag.size)
    }

    @Test
    fun startBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val behandlingOpprettes = slot<Behandling>()
        val behandlingHentes = slot<UUID>()
        val opplysningerHentes = slot<UUID>()
        val opplysningerMock = mockk<OpplysningDao>()

        val opprettetBehandling = Behandling(UUID.randomUUID(), 1, emptyList(), null, null, false)

        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, NoOpVilkaarKlient())

        every { behandlingerMock.opprett(capture(behandlingOpprettes)) } returns Unit
        every { behandlingerMock.hent(capture(behandlingHentes)) } returns opprettetBehandling
        every { opplysningerMock.finnOpplysningerIBehandling(capture(opplysningerHentes)) } returns emptyList()

        val resultat = sut.startBehandling(1, emptyList())

        Assertions.assertEquals(opprettetBehandling,  resultat)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sakId)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        Assertions.assertEquals(opplysningerHentes.captured, behandlingOpprettes.captured.id)
    }

    @Test
    fun leggTilGrunnlagPåBehandlingUtenVilkårsprøving() {
        val behandlingerMock = mockk<BehandlingDao>()
        val opplysningerMock = mockk<OpplysningDao>()
        val opplysningSomLagres = slot<Opplysning>()

        Kontekst.set(Context(Self("test"), mockk()))


        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, NoOpVilkaarKlient())

        val id = UUID.randomUUID()

        every { behandlingerMock.hent(id) } returns Behandling(id, 1, emptyList(), null, null, false)
        every { opplysningerMock.finnOpplysningerIBehandling(id) } returns emptyList()
        every { opplysningerMock.nyOpplysning(capture(opplysningSomLagres)) } returns Unit
        every { opplysningerMock.leggOpplysningTilBehandling(id, any()) } returns Unit

        val result = sut.leggTilGrunnlag(id, objectMapper.createObjectNode(), "trygdetid", Opplysning.Saksbehandler("S01"))

        Assertions.assertEquals(opplysningSomLagres.captured.id, result)
        Assertions.assertEquals("trygdetid", opplysningSomLagres.captured.opplysningType)
        Assertions.assertTrue(opplysningSomLagres.captured.kilde is Opplysning.Saksbehandler)

    }

    @Test
    fun leggTilGrunnlagPåBehandlingMedVilkårsprøving() {
        val behandlingerMock = mockk<BehandlingDao>()
        val opplysningerMock = mockk<OpplysningDao>()

        Kontekst.set(Context(Self("test"), mockk()))
        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, NoOpVilkaarKlient())
        val id = UUID.randomUUID()
        val vilkårsprøvd_behandling = Behandling(id, 1, emptyList(), Vilkårsprøving(emptyList(), objectMapper.createObjectNode(), ""), null, false)
        every { behandlingerMock.hent(id) } returns vilkårsprøvd_behandling
        every { opplysningerMock.finnOpplysningerIBehandling(id) } returns emptyList()


        Assertions.assertThrows(IllegalArgumentException::class.java){sut.leggTilGrunnlag(id, objectMapper.createObjectNode(), "trygdetid", Opplysning.Saksbehandler("S01"))}
    }
}

class NoOpVilkaarKlient: VilkaarKlient{
    override fun vurderVilkaar(vilkaar: String, opplysninger: List<Opplysning>): ObjectNode {
        return objectMapper.createObjectNode()
    }
}