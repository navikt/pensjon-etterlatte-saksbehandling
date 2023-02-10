package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.ktor.Saksbehandler
import no.nav.etterlatte.personOpplysning
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RealGenerellBehandlingServiceTest {
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
    fun `skal hente behandlinger`() {
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlinger() } returns listOf(
                revurdering(sak = 1, revurderingAarsak = RevurderingAarsak.SOEKER_DOD),
                foerstegangsbehandling(sak = 2),
                revurdering(sak = 3, revurderingAarsak = RevurderingAarsak.SOEKER_DOD),
                foerstegangsbehandling(sak = 4)
            )
        }
        val hendelserMock = mockk<HendelseDao>()
        val manueltOpphoerMock = mockk<ManueltOpphoerService>()
        val sut = RealGenerellBehandlingService(
            behandlingerMock,
            hendleseskanal,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelserMock,
            manueltOpphoerMock,
            mockk(),
            mockk(),
            mockk()
        )

        val behandlinger = sut.hentBehandlinger()

        assertAll(
            "skal hente behandlinger",
            { assertEquals(4, behandlinger.size) },
            { assertEquals(2, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(2, behandlinger.filterIsInstance<Revurdering>().size) }
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
        val manueltOpphoerMock = mockk<ManueltOpphoerService>()
        val sut = RealGenerellBehandlingService(
            behandlingerMock,
            hendleseskanal,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelserMock,
            manueltOpphoerMock,
            mockk(),
            mockk(),
            mockk()
        )
        val behandlingtype = sut.hentBehandlingstype(id)
        assertEquals(BehandlingType.REVURDERING, behandlingtype)
    }

    @Test
    fun `skal hente behandlinger i sak`() {
        val hendleseskanal = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>>()
        val behandlingerMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(sak = 1, revurderingAarsak = RevurderingAarsak.SOEKER_DOD),
                foerstegangsbehandling(sak = 1)
            )
        }
        val hendelserMock = mockk<HendelseDao>()
        val manueltOpphoerMock = mockk<ManueltOpphoerService>()
        val sut = RealGenerellBehandlingService(
            behandlingerMock,
            hendleseskanal,
            FoerstegangsbehandlingFactory(behandlingerMock, hendelserMock),
            RevurderingFactory(behandlingerMock, hendelserMock),
            hendelserMock,
            manueltOpphoerMock,
            mockk(),
            mockk(),
            mockk()
        )

        val behandlinger = sut.hentBehandlingerISak(1)

        assertAll(
            "skal hente behandlinger",
            { assertEquals(2, behandlinger.size) },
            { assertEquals(1, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
            { assertEquals(1, behandlinger.filterIsInstance<Revurdering>().size) }
        )
    }

    @Test
    fun `avbrytBehandling sjekker om behandlingsstatusen er gyldig for avbrudd`() {
        val sakId = 1L
        val avbruttBehandling = foerstegangsbehandling(sak = sakId, status = BehandlingStatus.AVBRUTT)
        val attestertBehandling = foerstegangsbehandling(sak = sakId, status = BehandlingStatus.ATTESTERT)
        val iverksattBehandling = foerstegangsbehandling(sak = sakId, status = BehandlingStatus.IVERKSATT)
        val nyFoerstegangsbehandling = foerstegangsbehandling(sak = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(avbruttBehandling.id) } returns avbruttBehandling
            every { hentBehandling(attestertBehandling.id) } returns attestertBehandling
            every { hentBehandling(iverksattBehandling.id) } returns iverksattBehandling
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                .copy(status = BehandlingStatus.AVBRUTT)
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>> {
            coEvery { send(any()) } returns Unit
        }
        val manueltOpphoerMock = mockk<ManueltOpphoerService>()

        val behandlingService =
            lagRealGenerellBehandlingService(behandlingDaoMock, hendelseskanalMock, hendelserMock, manueltOpphoerMock)

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(avbruttBehandling.id, "")
        }

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(iverksattBehandling.id, "")
        }

        assertThrows<IllegalStateException> {
            behandlingService.avbrytBehandling(attestertBehandling.id, "")
        }
        assertDoesNotThrow {
            behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        }
    }

    @Test
    fun `avbrytBehandling registrer en avbruddshendelse`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sak = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                .copy(status = BehandlingStatus.AVBRUTT)
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>> {
            coEvery { send(any()) } returns Unit
        }
        val manueltOpphoerMock = mockk<ManueltOpphoerService>()

        val behandlingService =
            lagRealGenerellBehandlingService(behandlingDaoMock, hendelseskanalMock, hendelserMock, manueltOpphoerMock)

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        verify {
            hendelserMock.behandlingAvbrutt(any(), any())
        }
    }

    @Test
    fun `avbrytBehandling sender en kafka-melding`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sak = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
                .copy(status = BehandlingStatus.AVBRUTT)
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<SendChannel<Pair<UUID, BehandlingHendelseType>>> {
            coEvery { send(Pair(nyFoerstegangsbehandling.id, BehandlingHendelseType.AVBRUTT)) } returns Unit
        }
        val manueltOpphoerMock = mockk<ManueltOpphoerService>()

        val behandlingService =
            lagRealGenerellBehandlingService(behandlingDaoMock, hendelseskanalMock, hendelserMock, manueltOpphoerMock)

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        coVerify {
            hendelseskanalMock.send(Pair(nyFoerstegangsbehandling.id, BehandlingHendelseType.AVBRUTT))
        }
    }

    @Test
    fun `skal sette rett enum for rolle eller ukjent rolle`() {
        val kjentRolle = "gjenlevende"
        val ukjentRolle = "abcde"

        val resKjentRolle = Saksrolle.enumVedNavnEllerUkjent(kjentRolle)
        val resUkjentRolle = Saksrolle.enumVedNavnEllerUkjent(ukjentRolle)

        assertEquals(Saksrolle.GJENLEVENDE, resKjentRolle)
        assertEquals(Saksrolle.UKJENT, resUkjentRolle)
    }

    @Test
    fun `hentBehandlingMedEnkelPersonopplysning henter behandlingsinfo og etterspurt personopplysning`() {
        val sakId = 1L
        val behandlingsId = UUID.randomUUID()
        val accesstoken = "token"
        val soeknadMottatDato = LocalDateTime.parse("2020-01-01T00:00:00")
        val behandling = foerstegangsbehandling(
            id = behandlingsId,
            sak = sakId,
            soeknadMottattDato = soeknadMottatDato
        )
        val opplysningstype = Opplysningstype.DOEDSDATO
        val doedsdato = LocalDate.parse("2020-01-01")

        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)

        val service = lagRealGenerellBehandlingService(
            behandlinger = mockk {
                every { hentBehandlingType(behandlingsId) } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { hentBehandling(behandlingsId, BehandlingType.FØRSTEGANGSBEHANDLING) } returns behandling
            },
            grunnlagKlient = mockk {
                coEvery {
                    finnPersonOpplysning(sakId, opplysningstype, accesstoken)
                } returns grunnlagsopplysningMedPersonopplysning
            }
        )
        val behandlingMedPersonopplsning = runBlocking {
            service.hentBehandlingMedEnkelPersonopplysning(
                behandlingsId,
                Saksbehandler("id"),
                accesstoken,
                opplysningstype
            )
        }

        assertEquals(soeknadMottatDato, behandlingMedPersonopplsning.soeknadMottattDato)
        assertEquals(doedsdato, behandlingMedPersonopplsning.personopplysning?.opplysning?.doedsdato)
    }

    private fun lagRealGenerellBehandlingService(
        behandlinger: BehandlingDao? = null,
        hendelseKanal: SendChannel<Pair<UUID, BehandlingHendelseType>>? = null,
        hendelseDao: HendelseDao? = null,
        manueltOpphoerService: ManueltOpphoerService? = null,
        grunnlagKlient: GrunnlagKlient? = null
    ): RealGenerellBehandlingService = RealGenerellBehandlingService(
        behandlinger = behandlinger ?: mockk(),
        behandlingHendelser = hendelseKanal ?: mockk(),
        foerstegangsbehandlingFactory = FoerstegangsbehandlingFactory(
            behandlinger = behandlinger ?: mockk(),
            hendelser = hendelseDao ?: mockk()
        ),
        revurderingFactory = RevurderingFactory(
            behandlinger = behandlinger ?: mockk(),
            hendelser = hendelseDao ?: mockk()
        ),
        hendelser = hendelseDao ?: mockk(),
        manueltOpphoerService = manueltOpphoerService ?: mockk(),
        mockk(),
        grunnlagKlient ?: mockk(),
        mockk()
    )
}