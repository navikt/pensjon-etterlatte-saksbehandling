package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsOpplysningMedPersonopplysning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.personOpplysning
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.token.Bruker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val user = mockk<Saksbehandler>()

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                user,
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
    fun `skal hente behandlinger i sak`() {
        val hendleseskanal = mockk<BehandlingHendelserKanal>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(sakId = 1, revurderingAarsak = RevurderingAarsak.REGULERING),
                foerstegangsbehandling(sakId = 1)
            )
        }
        val hendelserMock = mockk<HendelseDao>()

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val sut = RealGenerellBehandlingService(
            behandlingDaoMock,
            hendleseskanal,
            hendelserMock,
            mockk(),
            mockk(),
            mockk(),
            featureToggleService
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
        val avbruttBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.AVBRUTT)
        val attestertBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val iverksattBehandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(avbruttBehandling.id) } returns avbruttBehandling
            every { hentBehandling(attestertBehandling.id) } returns attestertBehandling
            every { hentBehandling(iverksattBehandling.id) } returns iverksattBehandling
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<BehandlingHendelserKanal> {
            coEvery { send(any()) } returns Unit
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                hendelseKanal = hendelseskanalMock,
                hendelseDao = hendelserMock,
                featureToggleService = featureToggleService
            )

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
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<BehandlingHendelserKanal> {
            coEvery { send(any()) } returns Unit
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                hendelseKanal = hendelseskanalMock,
                hendelseDao = hendelserMock,
                featureToggleService = featureToggleService
            )

        behandlingService.avbrytBehandling(nyFoerstegangsbehandling.id, "")
        verify {
            hendelserMock.behandlingAvbrutt(any(), any())
        }
    }

    @Test
    fun `avbrytBehandling sender en kafka-melding`() {
        val sakId = 1L
        val nyFoerstegangsbehandling = foerstegangsbehandling(sakId = sakId)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(nyFoerstegangsbehandling.id) } returns nyFoerstegangsbehandling
            every { avbrytBehandling(nyFoerstegangsbehandling.id) } just runs
        }
        val hendelserMock = mockk<HendelseDao> {
            every { behandlingAvbrutt(any(), any()) } returns Unit
        }
        val hendelseskanalMock = mockk<BehandlingHendelserKanal> {
            coEvery { send(Pair(nyFoerstegangsbehandling.id, BehandlingHendelseType.AVBRUTT)) } returns Unit
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val behandlingService =
            lagRealGenerellBehandlingService(
                behandlingDao = behandlingDaoMock,
                hendelseKanal = hendelseskanalMock,
                hendelseDao = hendelserMock,
                featureToggleService = featureToggleService
            )

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
        val soeknadMottatDato = LocalDateTime.parse("2020-01-01T00:00:00")
        val behandling = foerstegangsbehandling(
            id = BEHANDLINGS_ID,
            sakId = SAK_ID,
            soeknadMottattDato = soeknadMottatDato
        )
        val opplysningstype = Opplysningstype.AVDOED_PDL_V1
        val doedsdato = LocalDate.parse("2020-01-01")

        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)

        val service = lagRealGenerellBehandlingService(
            behandlingDao = mockk {
                every { hentBehandling(BEHANDLINGS_ID) } returns behandling
            },
            grunnlagKlient = mockk {
                coEvery {
                    finnPersonOpplysning(SAK_ID, opplysningstype, TOKEN)
                } returns grunnlagsopplysningMedPersonopplysning
            },
            featureToggleService = mockk {
                every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
            }
        )
        val behandlingMedPersonopplsning = runBlocking {
            service.hentBehandlingMedEnkelPersonopplysning(
                BEHANDLINGS_ID,
                TOKEN,
                opplysningstype
            )
        }

        assertEquals(soeknadMottatDato, behandlingMedPersonopplsning.soeknadMottattDato)
        assertEquals(doedsdato, behandlingMedPersonopplsning.personopplysning?.opplysning?.doedsdato)
    }

    @Test
    fun `erGyldigVirkningstidspunkt hvis tidspunkt er maaned etter doedsfall og maks tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)

        val gyldigVirkningstidspunkt = runBlocking {
            service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
        }

        assertTrue(gyldigVirkningstidspunkt)
    }

    @Test
    fun `erGyldigVirkningstidspunkt er false hvis tidspunkt er foer en maaned etter doedsfall`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2020-01-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-02-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2020-01-01")

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)

        val gyldigVirkningstidspunkt = runBlocking {
            service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
        }

        assertFalse(gyldigVirkningstidspunkt)
    }

    @Test
    fun `erGyldigVirkningstidspunkt hvis tidspunkt er tre aar foer mottatt soeknad`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-01-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"
        val request = VirkningstidspunktRequest(bodyVirkningstidspunkt.toString(), bodyBegrunnelse)

        val soeknadMottatt = LocalDateTime.parse("2020-01-01T00:00:00.000000000")
        val doedsdato = LocalDate.parse("2016-12-30")

        val service = behandlingServiceMedMocks(doedsdato, soeknadMottatt)

        val gyldigVirkningstidspunkt = runBlocking {
            service.erGyldigVirkningstidspunkt(BEHANDLINGS_ID, TOKEN, request)
        }

        assertFalse(gyldigVirkningstidspunkt)
    }

    @Test
    fun `hentSenestIverksatteBehandling() returnerer seneste iverksatte behandlingen`() {
        val behandling1 = foerstegangsbehandling(sakId = 1, status = BehandlingStatus.IVERKSATT)
        val behandling2 = revurdering(
            sakId = 1,
            status = BehandlingStatus.BEREGNET,
            revurderingAarsak = RevurderingAarsak.REGULERING
        )
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(any()) } returns listOf(behandling1, behandling2)
        }

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val service = lagRealGenerellBehandlingService(
            behandlingDao = behandlingDaoMock,
            featureToggleService = featureToggleService
        )

        assertEquals(behandling1, service.hentSisteIverksatte(1))
    }

    @Test
    fun `skal hente behandlinger i sak hvor sak har enhet og brukeren har enhet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val hendleseskanal = mockk<BehandlingHendelserKanal>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    enhet = Enheter.PORSGRUNN.enhetNr
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr)
            )
        }
        val hendelserMock = mockk<HendelseDao>()

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val sut = RealGenerellBehandlingService(
            behandlingDaoMock,
            hendleseskanal,
            hendelserMock,
            mockk(),
            mockk(),
            mockk(),
            featureToggleService
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
    fun `skal ikke hente behandlinger i sak hvor sak har enhet og brukeren har ikke enhet`() {
        every {
            user.enheter()
        } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val hendleseskanal = mockk<BehandlingHendelserKanal>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(1) } returns listOf(
                revurdering(
                    sakId = 1,
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    enhet = Enheter.PORSGRUNN.enhetNr
                ),
                foerstegangsbehandling(sakId = 1, enhet = Enheter.PORSGRUNN.enhetNr)
            )
        }
        val hendelserMock = mockk<HendelseDao>()

        val featureToggleService = mockk<FeatureToggleService>()
        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val sut = RealGenerellBehandlingService(
            behandlingDaoMock,
            hendleseskanal,
            hendelserMock,
            mockk(),
            mockk(),
            mockk(),
            featureToggleService
        )

        val behandlinger = sut.hentBehandlingerISak(1)

        assertEquals(0, behandlinger.size)
    }

    private fun behandlingServiceMedMocks(
        doedsdato: LocalDate?,
        soeknadMottatt: LocalDateTime
    ): RealGenerellBehandlingService {
        val behandling = foerstegangsbehandling(
            id = BEHANDLINGS_ID,
            sakId = SAK_ID,
            soeknadMottattDato = soeknadMottatt
        )
        val personopplysning = personOpplysning(doedsdato = doedsdato)
        val grunnlagsopplysningMedPersonopplysning = grunnlagsOpplysningMedPersonopplysning(personopplysning)

        return lagRealGenerellBehandlingService(
            behandlingDao = mockk {
                every {
                    hentBehandling(BEHANDLINGS_ID)
                } returns behandling
            },
            grunnlagKlient = mockk {
                coEvery {
                    finnPersonOpplysning(SAK_ID, Opplysningstype.AVDOED_PDL_V1, TOKEN)
                } returns grunnlagsopplysningMedPersonopplysning
            },
            featureToggleService = mockk {
                every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
            }
        )
    }

    private fun lagRealGenerellBehandlingService(
        behandlingDao: BehandlingDao? = null,
        hendelseKanal: BehandlingHendelserKanal? = null,
        hendelseDao: HendelseDao? = null,
        grunnlagKlient: GrunnlagKlient? = null,
        featureToggleService: FeatureToggleService
    ): RealGenerellBehandlingService = RealGenerellBehandlingService(
        behandlingDao = behandlingDao ?: mockk(),
        behandlingHendelser = hendelseKanal ?: mockk(),
        hendelseDao = hendelseDao ?: mockk(),
        mockk(),
        grunnlagKlient ?: mockk(),
        mockk(),
        featureToggleService
    )

    companion object {
        const val SAK_ID = 1L
        val BEHANDLINGS_ID: UUID = UUID.randomUUID()
        val TOKEN = Bruker.of("a", "b", null, null, null)
    }
}