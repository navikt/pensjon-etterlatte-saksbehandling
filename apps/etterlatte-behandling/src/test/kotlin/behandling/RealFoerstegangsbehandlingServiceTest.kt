package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

internal class RealFoerstegangsbehandlingServiceTest {

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                mockk(),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    @Test
    fun hentFoerstegangsbehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val hendelserMock = mockk<HendelseDao>()
        val sut = RealFoerstegangsbehandlingService(
            behandlingerMock,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            mockk()
        )
        val id = UUID.randomUUID()

        every {
            behandlingerMock.hentBehandling(
                id,
                BehandlingType.FØRSTEGANGSBEHANDLING
            )
        } returns Foerstegangsbehandling(
            id = id,
            sak = 1,
            behandlingOpprettet = LocalDateTime.now(),
            sistEndret = LocalDateTime.now(),
            status = BehandlingStatus.OPPRETTET,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            soeknadMottattDato = LocalDateTime.now(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            oppgaveStatus = OppgaveStatus.NY
        )

        assertEquals("Soeker", sut.hentFoerstegangsbehandling(id).persongalleri.innsender)
    }

    @Test
    fun startBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val hendelserMock = mockk<HendelseDao>()
        val behandlingOpprettes = slot<Foerstegangsbehandling>()
        val behandlingHentes = slot<UUID>()
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelse = slot<Pair<UUID, BehandlingHendelseType>>()
        val datoNaa = LocalDateTime.now()

        val opprettetBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = 1,
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            soeknadMottattDato = LocalDateTime.now(),
            persongalleri = Persongalleri(
                "Innsender",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            oppgaveStatus = OppgaveStatus.NY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        val sut = RealFoerstegangsbehandlingService(
            behandlingerMock,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            hendleseskanal
        )

        every { behandlingerMock.opprettFoerstegangsbehandling(capture(behandlingOpprettes)) } returns Unit
        every {
            behandlingerMock.hentBehandling(
                capture(behandlingHentes),
                BehandlingType.FØRSTEGANGSBEHANDLING
            )
        } returns opprettetBehandling
        every { hendelserMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingerMock.lagreGyldighetsproving(any()) } returns Unit
        coEvery { hendleseskanal.send(capture(hendelse)) } returns Unit

        val resultat = sut.startFoerstegangsbehandling(1, persongalleri, datoNaa.toString())

        assertEquals(opprettetBehandling.persongalleri.avdoed, resultat.persongalleri.avdoed)
        assertEquals(opprettetBehandling.sak, resultat.sak)
        assertEquals(opprettetBehandling.id, resultat.id)
        assertEquals(opprettetBehandling.persongalleri.soeker, resultat.persongalleri.soeker)
        assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        assertEquals(1, behandlingOpprettes.captured.sak)
        assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        assertEquals(resultat.id, hendelse.captured.first)
        assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second)

        @Test
        fun `avbrutt behandling kan ikke endres`() {
            val behandlingerMock = mockk<BehandlingDao>()
            val hendelserMock = mockk<HendelseDao>()
            val behandlingOpprettes = slot<Foerstegangsbehandling>()
            val behandlingHentes = slot<UUID>()
            val behandlingAvbrytes = slot<Foerstegangsbehandling>()

            val persongalleri = Persongalleri(
                "Soeker",
                "Innsender",
                emptyList(),
                listOf("Avdoed"),
                listOf("Gjenlevende")
            )

            val opprettetBehandling = Foerstegangsbehandling(
                id = UUID.randomUUID(),
                sak = 1,
                behandlingOpprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
                status = BehandlingStatus.OPPRETTET,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                soeknadMottattDato = LocalDateTime.now(),
                persongalleri = persongalleri,
                gyldighetsproeving = null,
                oppgaveStatus = OppgaveStatus.NY
            )

            val sut = RealFoerstegangsbehandlingService(
                behandlingerMock,
                FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
                mockChannel()
            )

            every { behandlingerMock.opprettFoerstegangsbehandling(capture(behandlingOpprettes)) } returns Unit
            every {
                behandlingerMock.hentBehandling(
                    capture(behandlingHentes),
                    BehandlingType.FØRSTEGANGSBEHANDLING
                )
            } returns opprettetBehandling
            every { behandlingerMock.lagreGyldighetsproving(any()) } returns Unit

            val resultat = sut.startFoerstegangsbehandling(1, persongalleri, LocalDateTime.now().toString())

            // behandlingen avbrytes
            assertEquals(false, resultat.status === BehandlingStatus.AVBRUTT)

            // val behandlingEtterAvbrutt = sut.avbrytBehandling(resultat.id)
            //     Assertions.assertEquals(BehandlingStatus.AVBRUTT, behandlingEtterAvbrutt)

            // val behandlingEtterVilkårsvurdering = slot<UUID>()
            // every { behandlingerMock.hentBehandling(capture(behandlingEtterVilkårsvurdering)) } returns behandlingEtterAvbrutt
        }
    }

    fun mockChannel() =
        mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>().apply { coEvery { send(any()) } returns Unit }
}