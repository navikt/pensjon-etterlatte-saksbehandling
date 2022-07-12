package no.nav.etterlatte.behandling

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.sql.Connection
import java.util.*

class RealGenerellBehandlingServiceTest {
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
    fun `skal hente behandlinger`() {
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlinger() } returns listOf(
                revurdering(sak = 1),
                foerstegangsbehandling(sak = 2),
                revurdering(sak = 3),
                foerstegangsbehandling(sak = 4)
            )
        }
        val hendelserMock = mockk<HendelseDao>()
        val sut = RealGenerellBehandlingService(
            behandlingerMock,
            hendleseskanal,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelserMock
        )

        val behandlinger = sut.hentBehandlinger()

        assertAll(
            "skal hente behandlinger",
            { assertEquals(4, behandlinger.size) },
            { assertEquals(2, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(2, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }

    @Test
    fun `hent behandlingstype`() {
        val id = UUID.randomUUID()
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandlingType(id) } returns BehandlingType.REVURDERING
        }
        val hendelserMock = mockk<HendelseDao>()
        val sut = RealGenerellBehandlingService(
            behandlingerMock,
            hendleseskanal,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelserMock
        )
        val behandlingtype = sut.hentBehandlingstype(id)
        assertEquals(BehandlingType.REVURDERING, behandlingtype)
    }

    @Test
    fun `skal hente behandlinger i sak`() {
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandingerISak(1) } returns listOf(
                revurdering(sak = 1),
                foerstegangsbehandling(sak = 1),
            )
        }
        val hendelserMock = mockk<HendelseDao>()
        val sut = RealGenerellBehandlingService(
            behandlingerMock,
            hendleseskanal,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelserMock
        )

        val behandlinger = sut.hentBehandlingerISak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) },
        )
    }


}