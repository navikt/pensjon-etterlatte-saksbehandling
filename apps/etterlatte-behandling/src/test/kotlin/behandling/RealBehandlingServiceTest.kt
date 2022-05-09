package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

internal class RealBehandlingServiceTest {

    @BeforeEach
    fun before() {
        Kontekst.set(Context(mockk(), object : DatabaseKontekst {
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
        val sut = RealBehandlingService(behandlingerMock, BehandlingFactory(behandlingerMock), mockk())
        val id = UUID.randomUUID()

        every { behandlingerMock.hentBehandling(id) } returns Behandling(
            id,
            1,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            "Ola Olsen",
            "Soeker",
            listOf("Gjenlevende"),
            listOf("Avdoed"),
            null,
            null,
            BehandlingStatus.OPPRETTET
        )

        Assertions.assertEquals("Ola Olsen", sut.hentBehandling(id).innsender)
    }

    @Test
    fun startBehandling() {
        val behandlingerMock = mockk<BehandlingDao>()
        val behandlingOpprettes = slot<Behandling>()
        val behandlingHentes = slot<UUID>()
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelse = slot<Pair<UUID, BehandlingHendelseType>>()
        val datoNaa = LocalDateTime.now()

        val opprettetBehandling = Behandling(
            UUID.randomUUID(),
            1,
            datoNaa,
            datoNaa,
            null,
            "Innsender",
            "Soeker",
            listOf("Gjenlevende"),
            listOf("Avdoed"),
            emptyList(),
            null,
            BehandlingStatus.OPPRETTET
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende"),
        )

        val sut = RealBehandlingService(
            behandlingerMock,
            BehandlingFactory(behandlingerMock),
            hendleseskanal
        )

        every { behandlingerMock.opprett(capture(behandlingOpprettes)) } returns Unit
        every { behandlingerMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingerMock.lagreGyldighetsproving(any()) } returns Unit
        every { behandlingerMock.lagrePersongalleriOgMottattdato(any())} returns Unit
        coEvery { hendleseskanal.send(capture(hendelse)) } returns Unit

        val resultat = sut.startBehandling(1, persongalleri, datoNaa.toString())

        Assertions.assertEquals(opprettetBehandling.avdoed, resultat.avdoed)
        Assertions.assertEquals(opprettetBehandling.sak, resultat.sak)
        Assertions.assertEquals(opprettetBehandling.id, resultat.id)
        Assertions.assertEquals(opprettetBehandling.soeker, resultat.soeker)
        Assertions.assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sak)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)
        Assertions.assertEquals(resultat.id, hendelse.captured.first)
        Assertions.assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second)
    }

    @Test
    fun `avbrutt behandling kan ikke endres`() {
        val behandlingerMock = mockk<BehandlingDao>()
        val behandlingOpprettes = slot<Behandling>()
        val behandlingHentes = slot<UUID>()
        val behandlingAvbrytes = slot<Behandling>()

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende"),
        )

        val opprettetBehandling = Behandling(
            UUID.randomUUID(),
            1,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            "Innsender",
            "Soeker",
            listOf("Gjenlevende"),
            listOf("Avdoed"),
            emptyList(),
            null,
            BehandlingStatus.OPPRETTET
        )

        val sut = RealBehandlingService(
            behandlingerMock,
            BehandlingFactory(behandlingerMock),
            mockChannel()
        )

        every { behandlingerMock.opprett(capture(behandlingOpprettes)) } returns Unit
        every { behandlingerMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingerMock.lagreGyldighetsproving(any()) } returns Unit
        every { behandlingerMock.lagrePersongalleriOgMottattdato(any())} returns Unit
        every { behandlingerMock.avbrytBehandling(capture(behandlingAvbrytes)) } returns Unit
        val resultat = sut.startBehandling(1, persongalleri, LocalDateTime.now().toString())

        // behandlingen avbrytes
        Assertions.assertEquals(false, resultat.status === BehandlingStatus.AVBRUTT)
        val behandlingEtterAvbrutt = sut.avbrytBehandling(resultat.id)
        Assertions.assertEquals(true, behandlingEtterAvbrutt.status === BehandlingStatus.AVBRUTT)

        val behandlingEtterVilkårsvurdering = slot<UUID>()
        every { behandlingerMock.hentBehandling(capture(behandlingEtterVilkårsvurdering)) } returns behandlingEtterAvbrutt
    }
}

fun mockChannel() =
    mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>().apply { coEvery { send(any()) } returns Unit }
