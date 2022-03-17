package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import java.util.*

internal class RealBehandlingServiceTest {

    @Test
    fun hentBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val opplysningerMock = mockk<OpplysningDao>()

        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, NoOpVilkaarKlient())

        val id = UUID.randomUUID()

        val opplysninger = listOf(
            Behandlingsopplysning(
                UUID.randomUUID(),
                Behandlingsopplysning.Saksbehandler("S01"),
                Opplysningstyper.SOEKER_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
            Behandlingsopplysning(
                UUID.randomUUID(),
                Behandlingsopplysning.Saksbehandler("S01"),
                Opplysningstyper.AVDOED_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
        )

        every { behandlingerMock.hent(id) } returns Behandling(id, 1, emptyList(), null, null, false)
        every { opplysningerMock.finnOpplysningerIBehandling(id) } returns opplysninger
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

    @Test
    fun `avbrutt behandling kan ikke endres`() {
        val behandlingerMock = mockk<BehandlingDao>()
        val behandlingOpprettes = slot<Behandling>()
        val behandlingHentes = slot<UUID>()
        val opplysningerHentes = slot<UUID>()
        val opplysningerMock = mockk<OpplysningDao>()
        val behandlingAvbrytes = slot<Behandling>()

        val opprettetBehandling = Behandling(UUID.randomUUID(), 1, emptyList(), null, null, false, false)

        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, NoOpVilkaarKlient())


        every { behandlingerMock.opprett(capture(behandlingOpprettes)) } returns Unit
        every { behandlingerMock.hent(capture(behandlingHentes)) } returns opprettetBehandling
        every { opplysningerMock.finnOpplysningerIBehandling(capture(opplysningerHentes)) } returns emptyList()
        every { behandlingerMock.lagreVilkarsproving(any()) } returns Unit
        every { behandlingerMock.avbrytBehandling(capture(behandlingAvbrytes)) } returns Unit
        val resultat = sut.startBehandling(1, emptyList())

        // behandlingen avbrytes
        Assertions.assertEquals(false, resultat.avbrutt)
        val behandlingEtterAvbrutt = sut.avbrytBehandling(resultat.id)
        Assertions.assertEquals(true, behandlingEtterAvbrutt.avbrutt)

        val behandlingEtterVilkårsvurdering = slot<UUID>()
        every { behandlingerMock.hent(capture(behandlingEtterVilkårsvurdering)) } returns behandlingEtterAvbrutt

        // avbrutt behandling kan ikke endres
        assertThrows<AvbruttBehandlingException> { sut.vilkårsprøv(behandlingEtterAvbrutt.id) }
    }


}

class NoOpVilkaarKlient : VilkaarKlient {
    override fun vurderVilkaar(opplysninger: List<Behandlingsopplysning<ObjectNode>>): List<VurdertVilkaar> {
        return listOf()
    }
}