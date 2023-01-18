package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.itest.saksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.manueltOpphoer
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.YearMonth
import java.util.*

internal class RealManueltOpphoerServiceTest {

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
    fun `skal hente manuelt opphoer`() {
        val sak = 1L
        val id = UUID.randomUUID()
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandling(id = id, type = BehandlingType.MANUELT_OPPHOER) } returns manueltOpphoer(
                sak = sak
            )
        }
        val hendelserMock = mockk<HendelseDao>() {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val sut = RealManueltOpphoerService(
            behandlingerMock,
            hendleseskanal,
            hendelserMock
        )
        val manueltOpphoer = sut.hentManueltOpphoer(id)
        assertEquals(sak, manueltOpphoer?.sak)
    }

    @Test
    fun `skal opprette et manuelt opphoer`() {
        val sak = 1L
        val behandlingId = UUID.randomUUID()
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sak = sak,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED,
                ManueltOpphoerAarsak.SOESKEN_DOED
            ),
            fritekstAarsak = "Det var enda en opphoersaarsak"
        )
        val alleBehandlingerISak_sak = slot<Long>()
        val opprettBehandling_slot = slot<ManueltOpphoer>()
        val hendelse_slot = slot<Pair<UUID, BehandlingHendelseType>>()

        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(capture(alleBehandlingerISak_sak)) } returns listOf(
                foerstegangsbehandling(sak = sak)
            )
            every { opprettManueltOpphoer(capture(opprettBehandling_slot)) } returns manueltOpphoer(
                sak = manueltOpphoerRequest.sak,
                behandlingId = behandlingId,
                opphoerAarsaker = manueltOpphoerRequest.opphoerAarsaker,
                fritekstAarsak = manueltOpphoerRequest.fritekstAarsak
            )
        }
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>() {
            coEvery { send(capture(hendelse_slot)) } returns Unit
        }
        val hendelserMock = mockk<HendelseDao>() {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val sut = RealManueltOpphoerService(
            behandlingerMock,
            hendelsesKanal,
            hendelserMock
        )

        val returnertManueltOpphoer = sut.opprettManueltOpphoer(manueltOpphoerRequest)

        assertAll(
            "skal starte manuelt opphoer",
            { assertEquals(manueltOpphoerRequest.sak, alleBehandlingerISak_sak.captured) },
            { assertEquals(manueltOpphoerRequest.sak, opprettBehandling_slot.captured.sak) },
            { assertEquals(manueltOpphoerRequest.opphoerAarsaker, opprettBehandling_slot.captured.opphoerAarsaker) },
            { assertEquals(manueltOpphoerRequest.fritekstAarsak, opprettBehandling_slot.captured.fritekstAarsak) },
            { assertEquals(BehandlingType.MANUELT_OPPHOER, opprettBehandling_slot.captured.type) },
            { assertEquals(manueltOpphoerRequest.sak, opprettBehandling_slot.captured.sak) },
            { assertEquals(behandlingId, hendelse_slot.captured.first) },
            { assertEquals(BehandlingHendelseType.OPPRETTET, hendelse_slot.captured.second) },
            { assertEquals(behandlingId, returnertManueltOpphoer?.id) }
        )
    }

    @Test
    fun `manuelt opphør får tidligste virkningstidspunkt fra iverksatte behandlinger på saken`() {
        val brukerFnr = "123"
        val sakId = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sak = sakId,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED,
                ManueltOpphoerAarsak.SOESKEN_DOED
            ),
            fritekstAarsak = "Det var enda en opphoersaarsak"
        )
        val opprettetManueltOpphoerSlot = slot<ManueltOpphoer>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(any()) } returns listOf(
                foerstegangsbehandling(
                    sak = sakId,
                    status = BehandlingStatus.IVERKSATT,
                    persongalleri = Persongalleri(
                        soeker = brukerFnr
                    ),
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 8),
                        Grunnlagsopplysning.Saksbehandler.create(
                            saksbehandlerToken
                        )
                    )
                ),
                revurdering(
                    sak = sakId,
                    status = BehandlingStatus.IVERKSATT,
                    persongalleri = Persongalleri(soeker = brukerFnr),
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 10),
                        Grunnlagsopplysning.Saksbehandler.create(saksbehandlerToken)
                    ),
                    revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                ),
                revurdering(
                    sak = sakId,
                    status = BehandlingStatus.VILKAARSVURDERT,
                    persongalleri = Persongalleri(soeker = brukerFnr),
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 5),
                        Grunnlagsopplysning.Saksbehandler.create(saksbehandlerToken)
                    ),
                    revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                )
            )
            every { opprettManueltOpphoer(capture(opprettetManueltOpphoerSlot)) } returns manueltOpphoer(
                sak = manueltOpphoerRequest.sak,
                behandlingId = UUID.randomUUID(),
                opphoerAarsaker = manueltOpphoerRequest.opphoerAarsaker,
                fritekstAarsak = manueltOpphoerRequest.fritekstAarsak
            )
        }

        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>> {
            coEvery { send(any()) } returns Unit
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val sut = RealManueltOpphoerService(
            behandlingerMock,
            hendelsesKanal,
            hendelserMock
        )

        sut.opprettManueltOpphoer(manueltOpphoerRequest)
        assertEquals(YearMonth.of(2022, 8), opprettetManueltOpphoerSlot.captured.virkningstidspunkt?.dato)
    }

    @Test
    fun `skal ikke kunne opphoere en sak som allerede er manuelt opphoert`() {
        val sak = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sak = sak,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED,
                ManueltOpphoerAarsak.SOESKEN_DOED
            ),
            fritekstAarsak = "Det var enda en opphoersaarsak"
        )
        val alleBehandlingerISak_sak = slot<Long>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(capture(alleBehandlingerISak_sak)) } returns listOf(
                manueltOpphoer(sak = sak)
            )
        }
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelserMock = mockk<HendelseDao>()
        val sut = RealManueltOpphoerService(
            behandlingerMock,
            hendelsesKanal,
            hendelserMock
        )

        val returnertManueltOpphoer = sut.opprettManueltOpphoer(manueltOpphoerRequest)

        assertNull(returnertManueltOpphoer)
        verify(exactly = 0) { behandlingerMock.opprettManueltOpphoer(any()) }
    }
}