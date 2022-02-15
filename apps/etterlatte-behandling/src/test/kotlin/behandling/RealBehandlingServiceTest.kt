package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Vilkårsprøving
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

        val opplysningers = listOf(
            Behandlingsopplysning(
                UUID.randomUUID(),
                Behandlingsopplysning.Saksbehandler("S01"),
                "trygdetid",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
            Behandlingsopplysning(
                UUID.randomUUID(),
                Behandlingsopplysning.Saksbehandler("S01"),
                "medlemskap",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
        )

        every { behandlingerMock.hent(id) } returns Behandling(id, 1, emptyList(), null, null, false)
        every { opplysningerMock.finnOpplysningerIBehandling(id) } returns opplysningers
        Assertions.assertEquals(2, sut.hentBehandling(id).grunnlag.size)
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
        every { behandlingerMock.lagreVilkarsproving(any()) } returns Unit

        val resultat = sut.startBehandling(1, emptyList())

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sak)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        Assertions.assertEquals(opplysningerHentes.captured, behandlingOpprettes.captured.id)
    }


}

class NoOpVilkaarKlient : VilkaarKlient {
    override fun vurderVilkaar(opplysninger: List<Behandlingsopplysning<ObjectNode>>): ObjectNode {
        return objectMapper.createObjectNode()
    }
}