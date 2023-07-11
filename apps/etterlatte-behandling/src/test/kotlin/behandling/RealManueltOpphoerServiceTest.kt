package no.nav.etterlatte.behandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerRequest
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.manueltOpphoer
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
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
import java.util.*

internal class RealManueltOpphoerServiceTest {

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val oppgaveService = mockk<OppgaveServiceNy>()

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
    fun `skal hente manuelt opphoer`() {
        val sakId = 1L
        val id = UUID.randomUUID()

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(id) } returns manueltOpphoer(
                sakId = sakId
            )
        }
        val hendelseDaoMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        }

        val sut = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )
        val manueltOpphoer = sut.hentManueltOpphoer(id)
        assertEquals(sakId, manueltOpphoer!!.sak.id)
    }

    @Test
    fun `skal opprette et manuelt opphoer`() {
        val sak = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sakId = sak,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED,
                ManueltOpphoerAarsak.SOESKEN_DOED
            ),
            fritekstAarsak = "Det var enda en opphoersaarsak"
        )
        val alleBehandlingerISakSlot = slot<Long>()
        val opprettBehandlingSlot = slot<OpprettBehandling>()

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(capture(alleBehandlingerISakSlot)) } returns listOf(
                foerstegangsbehandling(
                    sakId = sak,
                    status = BehandlingStatus.IVERKSATT,
                    virkningstidspunkt = Virkningstidspunkt(
                        dato = YearMonth.of(2022, 8),
                        kilde = Grunnlagsopplysning.Saksbehandler.create(ident = ""),
                        begrunnelse = ""
                    )
                )
            )
            every { opprettBehandling(capture(opprettBehandlingSlot)) } just runs
            every { hentBehandling(any()) } answers {
                manueltOpphoer(
                    sakId = manueltOpphoerRequest.sakId,
                    behandlingId = firstArg(),
                    opphoerAarsaker = manueltOpphoerRequest.opphoerAarsaker,
                    fritekstAarsak = manueltOpphoerRequest.fritekstAarsak
                )
            }
        }

        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any()) } returns Unit

        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer> {
            every { sendMeldingForHendelse(any(), any()) } returns Unit
        }
        val hendelseDaoMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        }

        val sut = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )

        val returnertManueltOpphoer = sut.opprettManueltOpphoer(manueltOpphoerRequest)

        assertAll(
            "skal starte manuelt opphoer",
            { assertEquals(manueltOpphoerRequest.sakId, alleBehandlingerISakSlot.captured) },
            { assertEquals(manueltOpphoerRequest.sakId, opprettBehandlingSlot.captured.sakId) },
            { assertEquals(manueltOpphoerRequest.opphoerAarsaker, opprettBehandlingSlot.captured.opphoerAarsaker) },
            { assertEquals(manueltOpphoerRequest.fritekstAarsak, opprettBehandlingSlot.captured.fritekstAarsak) },
            { assertEquals(opprettBehandlingSlot.captured.virkningstidspunkt?.dato, YearMonth.of(2022, 8)) },
            { assertEquals(BehandlingType.MANUELT_OPPHOER, opprettBehandlingSlot.captured.type) },
            { assertEquals(manueltOpphoerRequest.sakId, opprettBehandlingSlot.captured.sakId) },
            { assertEquals(opprettBehandlingSlot.captured.id, returnertManueltOpphoer?.id) }
        )
        verify {
            behandlingHendelserKafkaProducer.sendMeldingForHendelse(
                any(),
                BehandlingHendelseType.OPPRETTET
            )
        }
    }

    @Test
    fun `manuelt opphør får tidligste virkningstidspunkt fra iverksatte behandlinger på saken`() {
        val brukerFnr = "123"
        val sakId = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sakId = sakId,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED,
                ManueltOpphoerAarsak.SOESKEN_DOED
            ),
            fritekstAarsak = "Det var enda en opphoersaarsak"
        )
        val opprettetManueltOpphoerSlot = slot<OpprettBehandling>()
        val behandlingDaoMock = mockk<BehandlingDao> {
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
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 10),
                        Grunnlagsopplysning.Saksbehandler.create(saksbehandlerToken),
                        "begrunnelse"
                    )
                ),
                revurdering(
                    sakId = sakId,
                    status = BehandlingStatus.VILKAARSVURDERT,
                    persongalleri = Persongalleri(soeker = brukerFnr),
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    virkningstidspunkt = Virkningstidspunkt(
                        YearMonth.of(2022, 5),
                        Grunnlagsopplysning.Saksbehandler.create(saksbehandlerToken),
                        "begrunnelse"
                    )
                )
            )
            every { opprettBehandling(capture(opprettetManueltOpphoerSlot)) } just runs
            every { hentBehandling(any()) } answers {
                manueltOpphoer(
                    sakId = manueltOpphoerRequest.sakId,
                    behandlingId = firstArg(),
                    opphoerAarsaker = manueltOpphoerRequest.opphoerAarsaker,
                    fritekstAarsak = manueltOpphoerRequest.fritekstAarsak
                )
            }
        }
        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any()) } returns Unit

        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer> {
            every { sendMeldingForHendelse(any(), any()) } returns Unit
        }
        val hendelseDaoMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        }

        val sut = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )

        sut.opprettManueltOpphoer(manueltOpphoerRequest)
        assertEquals(YearMonth.of(2022, 8), opprettetManueltOpphoerSlot.captured.virkningstidspunkt?.dato)
    }

    @Test
    fun `skal ikke kunne opphoere en sak som allerede er manuelt opphoert`() {
        val sak = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sakId = sak,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED,
                ManueltOpphoerAarsak.SOESKEN_DOED
            ),
            fritekstAarsak = "Det var enda en opphoersaarsak"
        )
        val alleBehandlingerISak_sak = slot<Long>()
        val behandlingDaoMock = mockk<BehandlingDao>() {
            every { alleBehandlingerISak(capture(alleBehandlingerISak_sak)) } returns listOf(
                manueltOpphoer(sakId = sak)
            )
        }
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val hendelseDaoMock = mockk<HendelseDao>()
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        }

        val sut = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )

        val returnertManueltOpphoer = sut.opprettManueltOpphoer(manueltOpphoerRequest)

        assertNull(returnertManueltOpphoer)
        verify(exactly = 0) { behandlingDaoMock.opprettBehandling(any()) }
    }

    @Test
    fun `skal ikke kunne opphøre en sak som ikke har noen iverksatte behandlinger`() {
        val sak = 1L
        val manueltOpphoerRequest = ManueltOpphoerRequest(
            sakId = sak,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.SOESKEN_DOED,
                ManueltOpphoerAarsak.SOEKER_DOED
            ),
            fritekstAarsak = null
        )

        val behandlingDaoMock = mockk<BehandlingDao> {
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
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val hendelseDaoMock = mockk<HendelseDao>()
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        }

        val service = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )

        val opphoer = service.opprettManueltOpphoer(manueltOpphoerRequest)
        assertNull(opphoer)
    }

    @Test
    fun `hentManueltOpphoerOgAlleIverksatteBehandlingerISak svarer med null hvis ingen manuelt opphør med id finnes`() {
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val hendelseDaoMock = mockk<HendelseDao>()
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(any()) } returns listOf()
            every { hentBehandling(any()) } returns null
        }
        val service = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            mockk()
        )
        assertNull(service.hentManueltOpphoer(UUID.randomUUID()))
    }

    @Test
    fun `hentManueltOpphoerOgAlleIverksatteBehandlingerISak tar også med andre iverksatte behandlinger på saken`() {
        val manueltOpphoerId = UUID.randomUUID()
        val sakId = 1L
        val soeker = "12312312312"
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val hendelseDaoMock = mockk<HendelseDao>()
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
        val behandlingDaoMock = mockk<BehandlingDao> {
            every { alleBehandlingerISak(sakId) } returns listOf(
                opphoer,
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.AVBRUTT),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)
            )
            every { hentBehandling(manueltOpphoerId) } returns opphoer
        }
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        }

        val service = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )
        val (hentetOpphoer, andreBehandlinger) = service.hentManueltOpphoerOgAlleIverksatteBehandlingerISak(
            manueltOpphoerId
        )!!

        assertEquals(hentetOpphoer, opphoer)
        assertEquals(andreBehandlinger.size, 2)
        assertTrue(andreBehandlinger.all { it.status == BehandlingStatus.IVERKSATT })
    }

    @Test
    fun `skal hente manuelt opphoer når brukeren har enhet`() {
        val sakId = 1L
        val id = UUID.randomUUID()

        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(id) } returns manueltOpphoer(
                sakId = sakId,
                enhet = Enheter.PORSGRUNN.enhetNr
            )
        }
        val hendelseDaoMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true
        }

        val sut = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )
        val manueltOpphoer = sut.hentManueltOpphoer(id)
        assertEquals(sakId, manueltOpphoer!!.sak.id)
    }

    @Test
    fun `skal ikke hente manuelt opphoer hvis enhet er satt men brukeren har ikke enhet`() {
        val sakId = 1L
        val id = UUID.randomUUID()

        every {
            user.enheter()
        } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val behandlingDaoMock = mockk<BehandlingDao> {
            every { hentBehandling(id) } returns manueltOpphoer(
                sakId = sakId,
                enhet = Enheter.PORSGRUNN.enhetNr
            )
        }
        val hendelseDaoMock = mockk<HendelseDao> {
            every { behandlingOpprettet(any()) } returns Unit
        }
        val behandlingHendelserKafkaProducer = mockk<BehandlingHendelserKafkaProducer>()
        val featureToggleService = mockk<FeatureToggleService> {
            every { isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true
        }

        val sut = RealManueltOpphoerService(
            oppgaveService,
            behandlingDaoMock,
            behandlingHendelserKafkaProducer,
            hendelseDaoMock,
            featureToggleService
        )
        val manueltOpphoer = sut.hentManueltOpphoer(id)
        assertNull(manueltOpphoer)
    }
}