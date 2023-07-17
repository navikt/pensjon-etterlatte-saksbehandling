package no.nav.etterlatte.behandling

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingServiceImpl
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.revurdering.RevurderingServiceImpl
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.fixedNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.oppgaveny.OppgaveType
import no.nav.etterlatte.oppgaveny.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakServiceFeatureToggle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class FoerstegangsbehandlingServiceImplTest {

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val sakDaoMock = mockk<SakDao>()
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val grunnlagsendringshendelseDao = mockk<GrunnlagsendringshendelseDao>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val oppgaveService = mockk<OppgaveServiceNy>()
    private val mockOppgave = opprettNyOppgaveMedReferanseOgSak(
        "behandling",
        Sak("ident", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr),
        OppgaveType.FOERSTEGANGSBEHANDLING
    )
    private val revurderingService = RevurderingServiceImpl(
        oppgaveService,
        grunnlagService,
        behandlingHendelserKafkaProducerMock,
        featureToggleService,
        behandlingDaoMock,
        hendelseDaoMock,
        grunnlagsendringshendelseDao
    )
    private val naaTid = Tidspunkt.now()
    private val behandlingsService = FoerstegangsbehandlingServiceImpl(
        oppgaveService,
        grunnlagService,
        revurderingService,
        sakDaoMock,
        behandlingDaoMock,
        hendelseDaoMock,
        behandlingHendelserKafkaProducerMock,
        featureToggleService,
        naaTid.fixedNorskTid()
    )

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

    @AfterEach
    fun after() {
        confirmVerified(sakDaoMock, behandlingDaoMock, hendelseDaoMock, behandlingHendelserKafkaProducerMock)
        clearAllMocks()
    }

    @Test
    fun hentFoerstegangsbehandling() {
        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertEquals("Soeker", behandlingsService.hentFoerstegangsbehandling(id)!!.persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun startBehandling() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val opprettetBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                "Innsender",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingDaoMock.lagreGyldighetsproving(any()) } returns Unit
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns listOf(opprettetBehandling)
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any()) } returns Unit
        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any()) } returns mockOppgave

        val resultat = behandlingsService.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        assertEquals(opprettetBehandling, resultat)
        assertEquals(opprettetBehandling.persongalleri.avdoed, resultat.persongalleri.avdoed)
        assertEquals(opprettetBehandling.sak, resultat.sak)
        assertEquals(opprettetBehandling.id, resultat.id)
        assertEquals(opprettetBehandling.persongalleri.soeker, resultat.persongalleri.soeker)
        assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        assertEquals(1, behandlingOpprettes.captured.sakId)
        assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)

        verify(exactly = 1) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingDaoMock.lagreStatus(any(), any(), any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), BehandlingHendelseType.OPPRETTET)
            grunnlagService.leggInnNyttGrunnlag(any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
        }
    }

    @Test
    fun `skal opprette kun foerstegangsbehandling hvis det ikke finnes noen tidligere behandlinger`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val opprettetBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                "Innsender",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns emptyList()
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any()) } returns Unit
        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any()) } returns mockOppgave

        val foerstegangsbehandling = behandlingsService.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 1) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any())
            grunnlagService.leggInnNyttGrunnlag(any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
        }
    }

    @Test
    fun `skal avbryte behandling hvis under behandling og opprette en ny`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every {
            featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false)
        } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val underArbeidBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                "Innsender",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns underArbeidBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns underArbeidBehandling
        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any()) } returns Unit
        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any()) } returns mockOppgave

        val foerstegangsbehandling = behandlingsService.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns listOf(underArbeidBehandling)

        val nyfoerstegangsbehandling = behandlingsService.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )
        assertTrue(nyfoerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 2) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any())
            grunnlagService.leggInnNyttGrunnlag(any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
        }
        verify {
            behandlingDaoMock.lagreStatus(any(), BehandlingStatus.AVBRUTT, any())
        }
    }

    @Test
    fun `skal lage ny behandling som revurdering hvis behandling er satt til iverksatt`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every {
            featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false)
        } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val nyBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                "Innsender",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns nyBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns nyBehandling
        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any()) } returns Unit
        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any()) } returns mockOppgave

        val foerstegangsbehandling = behandlingsService.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        val iverksattBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.IVERKSATT,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                "Innsender",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns listOf(iverksattBehandling)

        every { behandlingDaoMock.hentBehandling(any()) } returns revurdering(
            sakId = 1,
            revurderingAarsak = RevurderingAarsak.NY_SOEKNAD,
            enhet = Enheter.defaultEnhet.enhetNr
        )

        val revurderingsBehandling = behandlingsService.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )
        assertTrue(revurderingsBehandling is Revurdering)
        verify {
            grunnlagService.leggInnNyttGrunnlag(any())
        }
        verify(exactly = 2) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any())
        }
    }

    @Test
    fun `lagring av gyldighetsproeving skal lagre og returnere gyldighetsresultat for innsender er gjenlevende`() {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        val behandling = Foerstegangsbehandling(
            id = id,
            sak = Sak("", SakType.BARNEPENSJON, 1, Enheter.PORSGRUNN.enhetNr),
            behandlingOpprettet = now,
            sistEndret = now,
            status = BehandlingStatus.OPPRETTET,
            persongalleri = persongalleri(),
            kommerBarnetTilgode = null,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            utenlandstilsnitt = null,
            soeknadMottattDato = now,
            gyldighetsproeving = null,
            prosesstype = Prosesstype.MANUELL,
            kilde = Vedtaksloesning.GJENNY
        )
        every { behandlingDaoMock.hentBehandling(any()) } returns behandling

        every { behandlingDaoMock.lagreGyldighetsproving(any()) } just Runs

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val forventetResultat = GyldighetsResultat(
            resultat = VurderingsResultat.OPPFYLT,
            vurderinger = listOf(
                VurdertGyldighet(
                    navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
                    resultat = VurderingsResultat.OPPFYLT,
                    basertPaaOpplysninger = ManuellVurdering(
                        begrunnelse = "begrunnelse",
                        kilde = Grunnlagsopplysning.Saksbehandler("saksbehandler", naaTid)
                    )
                )
            ),
            vurdertDato = naaTid.toLocalDatetimeNorskTid()
        )

        val resultat = behandlingsService.lagreGyldighetsproeving(
            id,
            "saksbehandler",
            JaNeiMedBegrunnelse(JaNei.JA, "begrunnelse")
        )

        assertEquals(forventetResultat, resultat)

        verify(exactly = 1) {
            behandlingDaoMock.hentBehandling(id)
            behandlingDaoMock.lagreGyldighetsproving(any())
        }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetMensFeatureErSkruddAv() {
        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertEquals("Soeker", behandlingsService.hentFoerstegangsbehandling(id)!!.persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetOgSaksbehandlerHarEnhet() {
        every {
            user.enheter()
        } returns listOf(Enheter.PORSGRUNN.enhetNr)

        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertEquals("Soeker", behandlingsService.hentFoerstegangsbehandling(id)!!.persongalleri.innsender)

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }

    @Test
    fun hentFoerstegangsbehandlingMedEnhetOgSaksbehandlerHarIkkeEnhet() {
        every {
            user.enheter()
        } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val id = UUID.randomUUID()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        every {
            behandlingDaoMock.hentBehandling(id)
        } returns Foerstegangsbehandling(
            id = id,
            sak = Sak(
                ident = "Ola Olsen",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr
            ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            persongalleri = Persongalleri(
                soeker = "Ola Olsen",
                "Soeker",
                listOf("Gjenlevende"),
                listOf("Avdoed"),
                emptyList()
            ),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        assertNull(behandlingsService.hentFoerstegangsbehandling(id))

        verify(exactly = 1) { behandlingDaoMock.hentBehandling(id) }
    }
}