package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.regulering.RealRevurderingService
import no.nav.etterlatte.behandling.regulering.RevurderingFactory
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID

class RealRevurderingServiceTest {

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
    fun `skal hente revurdering`() {
        val id = UUID.randomUUID()
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandling(id) } returns revurdering(
                id = id,
                sakId = 1,
                revurderingAarsak = RevurderingAarsak.REGULERING
            )
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val sut = RealRevurderingService(
            behandlingerMock,
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendleseskanal
        )

        val revurdering = sut.hentRevurdering(id)
        assertAll(
            "skal hente revurdering",
            { assertEquals(id, revurdering.id) }
        )
    }

    @Test
    fun `skal hente alle revurderinger`() {
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerAvType(type = BehandlingType.REVURDERING) } returns listOf(
                revurdering(sakId = 1, revurderingAarsak = RevurderingAarsak.REGULERING),
                revurdering(sakId = 2, revurderingAarsak = RevurderingAarsak.REGULERING),
                revurdering(sakId = 3, revurderingAarsak = RevurderingAarsak.REGULERING),
                revurdering(sakId = 4, revurderingAarsak = RevurderingAarsak.REGULERING),
                revurdering(sakId = 5, revurderingAarsak = RevurderingAarsak.REGULERING)
            )
        }
        val hendelserMock = mockk<HendelseDao>()
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val sut = RealRevurderingService(
            behandlingerMock,
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelsesKanal
        )

        val revurderinger = sut.hentRevurderinger()
        assertAll(
            "skal hente revurderinger",
            { assertEquals(5, revurderinger.size) }
        )
    }

    @Test
    fun `skal starte revurdering`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val forrigeBehandling = foerstegangsbehandling(sakId = 1)
        val doedsHendelse = Doedshendelse("12345678911", LocalDate.of(2022, 1, 1), Endringstype.OPPRETTET)
        val revurdering =
            revurdering(sakId = 1, revurderingAarsak = RevurderingAarsak.REGULERING)
        val hendelse = slot<Pair<UUID, BehandlingHendelseType>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { opprettBehandling(capture(behandlingOpprettes)) } returns Unit
            every { hentBehandling(capture(behandlingHentes)) } returns revurdering
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }

        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>> {
            coEvery { send(capture(hendelse)) } returns Unit
        }
        val sut = RealRevurderingService(
            behandlingerMock,
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelsesKanal
        )

        val opprettetRevurdering =
            sut.startRevurdering(
                forrigeBehandling,
                doedsHendelse,
                RevurderingAarsak.REGULERING
            )

        assertAll(
            "skal starte revurdering",
            { assertEquals(revurdering.sak, opprettetRevurdering.sak) },
            { assertEquals(revurdering.id, opprettetRevurdering.id) },
            { assertEquals(revurdering.persongalleri, opprettetRevurdering.persongalleri) },
            { assertEquals(revurdering.behandlingOpprettet, opprettetRevurdering.behandlingOpprettet) },
            { assertEquals(revurdering.status, opprettetRevurdering.status) },
            { assertEquals(revurdering.oppgaveStatus, opprettetRevurdering.oppgaveStatus) },
            { assertEquals(revurdering.revurderingsaarsak, opprettetRevurdering.revurderingsaarsak) },
            { assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id) },
            { assertEquals(1, behandlingOpprettes.captured.sakId) },
            { assertEquals(opprettetRevurdering.id, hendelse.captured.first) },
            { assertEquals(BehandlingHendelseType.OPPRETTET, hendelse.captured.second) }
        )
    }
}