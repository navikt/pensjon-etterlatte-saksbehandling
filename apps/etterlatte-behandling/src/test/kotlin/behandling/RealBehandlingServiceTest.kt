package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.time.LocalDateTime

import java.util.*

internal class RealBehandlingServiceTest {

    @BeforeEach
    fun before(){
        Kontekst.set(Context(mockk(), object:DatabaseKontekst{
            override fun activeTx(): Connection {
                throw IllegalArgumentException()
            }
            override fun <T> inTransaction(block: () -> T): T {
                return block()
            }
        }))
    }

    @Test
    fun hentBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val opplysningerMock = mockk<OpplysningDao>()

        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, BehandlingFactory(behandlingerMock, opplysningerMock) , mockk())

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

        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelse = slot<Pair<UUID, BehandlingHendelseType>>()
        val opprettetBehandling = Behandling(UUID.randomUUID(), 1, emptyList(), null, null, false)

        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, BehandlingFactory(behandlingerMock, opplysningerMock), hendleseskanal)

        every { behandlingerMock.opprett(capture(behandlingOpprettes)) } returns Unit
        every { behandlingerMock.hent(capture(behandlingHentes)) } returns opprettetBehandling
        every { opplysningerMock.finnOpplysningerIBehandling(capture(opplysningerHentes)) } returns emptyList()
        every { behandlingerMock.lagreVilkarsproving(any()) } returns Unit
        coEvery { hendleseskanal.send(capture(hendelse)) } returns Unit

        val resultat = sut.startBehandling(1, emptyList())

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sak)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        Assertions.assertEquals(opplysningerHentes.captured, behandlingOpprettes.captured.id)
        Assertions.assertEquals(resultat.id, hendelse.captured.first)
        Assertions.assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second)
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

        val sut = RealBehandlingService(behandlingerMock, opplysningerMock, BehandlingFactory(behandlingerMock, opplysningerMock), mockChannel())


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
        //TODO endre til en annen test her?
        //assertThrows<AvbruttBehandlingException> { sut.vilkårsprøv(behandlingEtterAvbrutt.id) }
    }


}

fun mockChannel() = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>().apply { coEvery { send(any()) } returns Unit }

class NoOpVilkaarKlient : VilkaarKlient {
    override fun vurderVilkaar(opplysninger: List<Behandlingsopplysning<ObjectNode>>): VilkaarResultat {
        return VilkaarResultat(null, listOf(), LocalDateTime.now())
    }
}