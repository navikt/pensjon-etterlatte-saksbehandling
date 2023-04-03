package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerRequest
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.manueltOpphoer
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.saksbehandlerToken
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.YearMonth
import java.util.UUID

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
        val sakId = 1L
        val id = UUID.randomUUID()
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandling(id) } returns manueltOpphoer(
                sakId = sakId
            )
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val sut = RealManueltOpphoerService(
            behandlingerMock,
            hendleseskanal,
            hendelserMock
        )
        val manueltOpphoer = sut.hentManueltOpphoer(id)
        assertEquals(sakId, manueltOpphoer!!.sak.id)
    }

    @Test
    fun `skal opprette et manuelt opphoer`() {
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
        val opprettBehandling_slot = slot<OpprettBehandling>()
        val hendelse_slot = slot<Pair<UUID, BehandlingHendelseType>>()

        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(capture(alleBehandlingerISak_sak)) } returns listOf(
                foerstegangsbehandling(
                    sakId = sak,
                    virkningstidspunkt = Virkningstidspunkt(
                        dato = YearMonth.of(2022, 8),
                        kilde = Grunnlagsopplysning.Saksbehandler.create(ident = ""),
                        begrunnelse = ""
                    ),
                    status = BehandlingStatus.IVERKSATT
                )
            )
            every { opprettBehandling(capture(opprettBehandling_slot)) } just runs
            every { hentBehandling(any()) } answers {
                manueltOpphoer(
                    sakId = manueltOpphoerRequest.sak,
                    behandlingId = firstArg(),
                    opphoerAarsaker = manueltOpphoerRequest.opphoerAarsaker,
                    fritekstAarsak = manueltOpphoerRequest.fritekstAarsak
                )
            }
        }
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>> {
            coEvery { send(capture(hendelse_slot)) } returns Unit
        }
        val hendelserMock = mockk<HendelseDao> {
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
            { assertEquals(manueltOpphoerRequest.sak, opprettBehandling_slot.captured.sakId) },
            { assertEquals(manueltOpphoerRequest.opphoerAarsaker, opprettBehandling_slot.captured.opphoerAarsaker) },
            { assertEquals(manueltOpphoerRequest.fritekstAarsak, opprettBehandling_slot.captured.fritekstAarsak) },
            { assertEquals(opprettBehandling_slot.captured.virkningstidspunkt?.dato, YearMonth.of(2022, 8)) },
            { assertEquals(BehandlingType.MANUELT_OPPHOER, opprettBehandling_slot.captured.type) },
            { assertEquals(manueltOpphoerRequest.sak, opprettBehandling_slot.captured.sakId) },
            { assertEquals(opprettBehandling_slot.captured.id, hendelse_slot.captured.first) },
            { assertEquals(BehandlingHendelseType.OPPRETTET, hendelse_slot.captured.second) },
            { assertEquals(opprettBehandling_slot.captured.id, returnertManueltOpphoer?.id) }
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
        val opprettetManueltOpphoerSlot = slot<OpprettBehandling>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(any()) } returns listOf(
                foerstegangsbehandling(
                    sakId = sakId,
                    status = BehandlingStatus.IVERKSATT,
                    persongalleri = Persongalleri(
                        soeker = brukerFnr
                    ),
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 8),
                        Grunnlagsopplysning.Saksbehandler.create(
                            saksbehandlerToken
                        ),
                        "begrunnelse"
                    )
                ),
                revurdering(
                    sakId = sakId,
                    status = BehandlingStatus.IVERKSATT,
                    persongalleri = Persongalleri(soeker = brukerFnr),
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 10),
                        Grunnlagsopplysning.Saksbehandler.create(saksbehandlerToken),
                        "begrunnelse"
                    ),
                    revurderingAarsak = RevurderingAarsak.REGULERING
                ),
                revurdering(
                    sakId = sakId,
                    status = BehandlingStatus.VILKAARSVURDERT,
                    persongalleri = Persongalleri(soeker = brukerFnr),
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 5),
                        Grunnlagsopplysning.Saksbehandler.create(saksbehandlerToken),
                        "begrunnelse"
                    ),
                    revurderingAarsak = RevurderingAarsak.REGULERING
                )
            )
            every { opprettBehandling(capture(opprettetManueltOpphoerSlot)) } just runs
            every { hentBehandling(any()) } answers {
                manueltOpphoer(
                    sakId = manueltOpphoerRequest.sak,
                    behandlingId = firstArg(),
                    opphoerAarsaker = manueltOpphoerRequest.opphoerAarsaker,
                    fritekstAarsak = manueltOpphoerRequest.fritekstAarsak
                )
            }
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
                manueltOpphoer(sakId = sak)
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
        verify(exactly = 0) { behandlingerMock.opprettBehandling(any()) }
    }

    @Test
    fun `skal ikke kunne opphøre en sak som ikke har noen iverksatte behandlinger`() {
        val sak = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sak = sak,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.SOESKEN_DOED,
                ManueltOpphoerAarsak.SOEKER_DOED
            ),
            fritekstAarsak = null
        )
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(sak) } returns listOf(
                foerstegangsbehandling(
                    sakId = sak,
                    status = BehandlingStatus.FATTET_VEDTAK,
                    virkningstidspunkt = Virkningstidspunkt(
                        dato = YearMonth.of(2020, 8),
                        kilde = Grunnlagsopplysning.Saksbehandler.create(ident = ""),
                        begrunnelse = "dab on the haters"
                    )
                )
            )
        }

        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelserMock = mockk<HendelseDao>()
        val service = RealManueltOpphoerService(
            behandlingerMock,
            hendelsesKanal,
            hendelserMock
        )

        val opphoer = service.opprettManueltOpphoer(manueltOpphoerRequest)
        assertNull(opphoer)
    }

    @Test
    fun `hentManueltOpphoerOgAlleIverksatteBehandlingerISak svarer med null hvis ingen manuelt opphør med id finnes`() {
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelserMock = mockk<HendelseDao>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandling(any()) } returns null
            every { alleBehandlingerISak(any()) } returns listOf()
        }
        val service = RealManueltOpphoerService(
            behandlingerMock,
            hendelsesKanal,
            hendelserMock
        )
        assertNull(service.hentManueltOpphoer(UUID.randomUUID()))
    }

    @Test
    fun `hentManueltOpphoerOgAlleIverksatteBehandlingerISak tar også med andre iverksatte behandlinger på saken`() {
        val manueltOpphoerId = UUID.randomUUID()
        val sakId = 1L
        val soeker = "12312312312"
        val hendelsesKanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val hendelserMock = mockk<HendelseDao>()
        val opphoer = manueltOpphoer(
            sakId = sakId,
            behandlingId = manueltOpphoerId,
            persongalleri = Persongalleri(
                soeker = soeker,
                innsender = null,
                soesken = listOf(),
                avdoed = listOf(),
                gjenlevende = listOf()
            ),
            opphoerAarsaker = listOf(ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED)
        )
        val behandlingerMock = mockk<BehandlingDao> {
            every { hentBehandling(manueltOpphoerId) } returns opphoer
            every { alleBehandlingerISak(sakId) } returns listOf(
                opphoer,
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.AVBRUTT),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)
            )
        }
        val service = RealManueltOpphoerService(
            behandlingerMock,
            hendelsesKanal,
            hendelserMock
        )
        val (hentetOpphoer, andreBehandlinger) = service.hentManueltOpphoerOgAlleIverksatteBehandlingerISak(
            manueltOpphoerId
        )!!

        assertEquals(hentetOpphoer, opphoer)
        assertEquals(andreBehandlinger.size, 2)
        assertTrue(andreBehandlinger.all { it.status == BehandlingStatus.IVERKSATT })
    }
}