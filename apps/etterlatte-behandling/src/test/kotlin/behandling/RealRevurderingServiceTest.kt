package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.revurdering.RealRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

class RealRevurderingServiceTest {

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
    fun `skal hente revurdering`() {
        val id = UUID.randomUUID()
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandling(id = id, type = BehandlingType.REVURDERING) } returns revurdering(id = id, sak = 1)
        }
        val hendelserMock = mockk<HendelseDao>() {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val sut = RealRevurderingService(
            behandlingerMock, RevurderingFactory(behandlingerMock, hendelserMock), hendleseskanal
        )

        val revurdering = sut.hentRevurdering(id)
        assertAll("skal hente revurdering",
            { assertEquals(id, revurdering!!.id) },
            { assertTrue(revurdering is Revurdering) })
    }

    @Test
    fun `skal hente alle revurderinger`() {
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerAvType(type = BehandlingType.REVURDERING) } returns listOf(
                revurdering(sak = 1),
                revurdering(sak = 2),
                revurdering(sak = 3),
                revurdering(sak = 4),
                revurdering(sak = 5)
            )
        }
        val hendelserMock = mockk<HendelseDao>()
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val sut = RealRevurderingService(
            behandlingerMock, RevurderingFactory(behandlingerMock, hendelserMock), hendelsesKanal
        )

        val revurderinger = sut.hentRevurderinger()
        assertAll("skal hente revurderinger",
            { assertEquals(5, revurderinger.size) },
            { assertTrue(revurderinger.all { it is Revurdering }) })
    }

    @Test
    fun `skal starte revurdering`() {
        val mottattDato = LocalDateTime.now()
        val behandlingOpprettes = slot<Revurdering>()
        val behandlingHentes = slot<UUID>()
        val revurdering = revurdering(sak = 1, soeknadMottattDato = mottattDato)
        val hendelse = slot<Pair<UUID, BehandlingHendelseType>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { opprettRevurdering(capture(behandlingOpprettes)) } returns Unit
            every { hentBehandling(capture(behandlingHentes), BehandlingType.REVURDERING) } returns revurdering
        }
        val hendelserMock = mockk<HendelseDao>() {
            every { behandlingOpprettet(any()) } returns Unit
        }

        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>() {
            coEvery { send(capture(hendelse)) } returns Unit
        }
        val sut = RealRevurderingService(
            behandlingerMock, RevurderingFactory(behandlingerMock, hendelserMock), hendelsesKanal
        )

        val opprettetRevurdering = sut.startRevurdering(1, persongalleri(), mottattDato.toString())

        assertAll(
            "skal starte revurdering",
            { assertEquals(revurdering.sak, opprettetRevurdering.sak) },
            { assertEquals(revurdering.id, opprettetRevurdering.id) },
            { assertEquals(revurdering.persongalleri, opprettetRevurdering.persongalleri) },
            { assertEquals(revurdering.soeknadMottattDato, opprettetRevurdering.soeknadMottattDato) },
            { assertEquals(revurdering.behandlingOpprettet, opprettetRevurdering.behandlingOpprettet) },
            { assertEquals(revurdering.status, opprettetRevurdering.status) },
            { assertEquals(revurdering.oppgaveStatus, opprettetRevurdering.oppgaveStatus) },
            { assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id) },
            { assertEquals(1, behandlingOpprettes.captured.sak) },
            { assertEquals(opprettetRevurdering.id, hendelse.captured.first) },
            { assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second) }
        )
    }
}
