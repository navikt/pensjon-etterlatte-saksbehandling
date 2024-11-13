package no.nav.etterlatte.behandling.aktivitetsplikt

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveKanIkkeEndres
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktOppgaveServiceTest {
    private val aktivitetspliktService: AktivitetspliktService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val sakService: SakService = mockk()
    private val aktivitetspliktBrevDao: AktivitetspliktBrevDao = mockk()
    private val brevApiKlient: BrevApiKlient = mockk()
    private val service =
        AktivitetspliktOppgaveService(
            aktivitetspliktService = aktivitetspliktService,
            oppgaveService = oppgaveService,
            sakService = sakService,
            aktivitetspliktBrevDao,
            brevApiKlient,
            mockk(relaxed = true),
        )

    private val sak =
        Sak(
            "en ident",
            sakType = SakType.OMSTILLINGSSTOENAD,
            id = randomSakId(),
            enhet = Enheter.defaultEnhet.enhetNr,
        )

    @BeforeEach
    fun setupMocks() {
        clearAllMocks()
        every { sakService.finnSak(sak.id) } returns sak
    }

    @Test
    fun `hentVurderingForOppgave kopierer inn nyeste vurderinger på sak hvis det ikke er lagret noe på oppgaven`() {
        val oppgave =
            lagNyOppgave(
                sak = sak,
                oppgaveKilde = OppgaveKilde.HENDELSE,
                oppgaveType = OppgaveType.AKTIVITETSPLIKT,
            )

        every { oppgaveService.hentOppgave(oppgave.id) } returns oppgave
        every { aktivitetspliktService.hentVurderingForOppgave(oppgave.id) } returns AktivitetspliktVurdering(emptyList(), emptyList())
        every { aktivitetspliktService.kopierInnTilOppgave(sak.id, oppgave.id) } returns null
        every { aktivitetspliktBrevDao.hentBrevdata(oppgave.id) } returns null

        service.hentVurderingForOppgave(oppgave.id)

        verify(exactly = 1) { aktivitetspliktService.kopierInnTilOppgave(sak.id, oppgave.id) }
    }

    @Test
    fun `hentVurderingForOppgave gjør ingen kopiering hvis oppgaven er avsluttet`() {
        val oppgave =
            lagNyOppgave(
                sak = sak,
                oppgaveKilde = OppgaveKilde.HENDELSE,
                oppgaveType = OppgaveType.AKTIVITETSPLIKT_12MND,
            ).copy(status = Status.AVBRUTT)

        every { oppgaveService.hentOppgave(oppgave.id) } returns oppgave
        every { aktivitetspliktService.hentVurderingForOppgave(oppgave.id) } returns AktivitetspliktVurdering(emptyList(), emptyList())
        every { aktivitetspliktBrevDao.hentBrevdata(oppgave.id) } returns null
        service.hentVurderingForOppgave(oppgave.id)

        verify(exactly = 0) { aktivitetspliktService.kopierInnTilOppgave(sak.id, oppgave.id) }
    }

    @Test
    fun `hentVurderingForOppgave gjør ingen kopiering hvis oppgaven er allerede har vurdering eller unntak`() {
        val oppgave =
            lagNyOppgave(
                sak = sak,
                oppgaveKilde = OppgaveKilde.HENDELSE,
                oppgaveType = OppgaveType.AKTIVITETSPLIKT_12MND,
            )
        every { oppgaveService.hentOppgave(oppgave.id) } returns oppgave

        val grad =
            aktivitetsgrad(
                id = UUID.randomUUID(),
                sakId = sak.id,
                behandlingId = null,
                oppgaveId = null,
                aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )

        every { aktivitetspliktService.hentVurderingForOppgave(oppgave.id) } returns
            AktivitetspliktVurdering(
                listOf(grad),
                emptyList(),
            )

        every { aktivitetspliktBrevDao.hentBrevdata(oppgave.id) } returns null
        service.hentVurderingForOppgave(oppgave.id)
        verify(exactly = 0) { aktivitetspliktService.kopierInnTilOppgave(sak.id, oppgave.id) }
    }

    @Test
    fun `hentVurderingForOppgave kaster feil hvis den blir spurt om en ikke-støttet oppgavetype`() {
        val oppgave =
            lagNyOppgave(
                sak = sak,
                oppgaveKilde = OppgaveKilde.HENDELSE,
                oppgaveType = OppgaveType.REVURDERING,
            )

        every { oppgaveService.hentOppgave(oppgave.id) } returns oppgave

        assertThrows<ForespoerselException> {
            service.hentVurderingForOppgave(oppgave.id)
        }
    }

    @Test
    fun `Skal ikke opprette brev da skal sende brev er false`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
            }
        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        val skalIkkeSendebrev = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, false, kilde = kilde)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalIkkeSendebrev
        assertThrows<BrevFeil> {
            service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        }
        verify(exactly = 0) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 0) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }

    @Test
    fun `Skal ikke opprette brev hvis man mangler aktivitetsgrad`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
            }
        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        val skalsendeBrev =
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                null,
                true,
                redusertEtterInntekt = true,
                utbetaling = true,
                kilde = kilde,
            )
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalsendeBrev

        every { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) } returns AktivitetspliktVurdering(emptyList(), emptyList())
        assertThrows<ManglerAktivitetsgrad> {
            service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        }
        verify(exactly = 0) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 1) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }

    @Test
    fun `Skal ikke opprette brev da skal sende brev hvis mangler utbeatling`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
            }

        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        val skalIkkeSendebrev =
            AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, true, redusertEtterInntekt = true, kilde = kilde)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalIkkeSendebrev
        assertThrows<ManglerBrevdata> { service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler) }
        verify(exactly = 0) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 0) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }

    @Test
    fun `Skal ikke opprette brev da skal sende brev hvis mangler redusertEtterInntekt`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
            }
        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")
        val skalIkkeSendebrev = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, true, utbetaling = true, kilde = kilde)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalIkkeSendebrev
        assertThrows<ManglerBrevdata> { service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler) }
    }

    @Test
    fun `Skal ikke opprette brev hvis brevid finnes`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
            }

        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        val brevIdfinnes = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, 2L, true, kilde = kilde)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns brevIdfinnes
        service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        verify(exactly = 0) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 0) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }

    @Test
    fun `Skal opprette brev hvis skalsendebrev true`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { type } returns OppgaveType.AKTIVITETSPLIKT_12MND
            }

        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val aksgrad =
            AktivitetspliktAktivitetsgrad(
                id = UUID.randomUUID(),
                sakId = sakIdForOppgave,
                behandlingId = UUID.randomUUID(),
                oppgaveId = oppgaveId,
                aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                fom = LocalDate.now(),
                tom = null,
                opprettet = kilde,
                endret = kilde,
                beskrivelse = "Beskrivelse",
                vurdertFra12Mnd = true,
            )
        every { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) } returns
            mockk {
                every { aktivitet } returns listOf(aksgrad)
            }
        every { aktivitetspliktBrevDao.lagreBrevId(oppgaveId, any()) } returns 1

        val skalSendeBrev =
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                null,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalSendeBrev
        coEvery { brevApiKlient.opprettSpesifiktBrev(any(), any(), any()) } returns
            mockk {
                every { id } returns 1L
            }
        service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        verify(exactly = 1) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 1) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }

    @Test
    fun `skal ikke opprette brev hvis det ikke er vurdert fra 12 mnd i 12mnd-oppgave`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = SakId(1L)
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { type } returns OppgaveType.AKTIVITETSPLIKT_12MND
            }

        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val aksgrad =
            AktivitetspliktAktivitetsgrad(
                id = UUID.randomUUID(),
                sakId = sakIdForOppgave,
                behandlingId = UUID.randomUUID(),
                oppgaveId = oppgaveId,
                aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                fom = LocalDate.now(),
                tom = null,
                opprettet = kilde,
                endret = kilde,
                beskrivelse = "Beskrivelse",
                vurdertFra12Mnd = false,
            )
        every { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) } returns
            mockk {
                every { aktivitet } returns listOf(aksgrad)
            }
        every { aktivitetspliktBrevDao.lagreBrevId(oppgaveId, any()) } returns 1

        val skalSendeBrev =
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                null,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalSendeBrev
        coEvery { brevApiKlient.opprettSpesifiktBrev(any(), any(), any()) } returns
            mockk {
                every { id } returns 1L
            }
        assertThrows<UgyldigForespoerselException> {
            service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        }
        verify(exactly = 0) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 1) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }

    @Test
    fun `Kan ikke ferdigstille brev og oppgave om oppgaven er i feil state`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { status } returns Status.AVBRUTT
            }
        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        val brevId = 1234L
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                brevId,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { oppgaveService.sjekkOmKanFerdigstilleOppgave(any(), any()) } throws OppgaveKanIkkeEndres(oppgaveId, Status.AVBRUTT)
        every { oppgaveService.ferdigstillOppgave(oppgaveId, simpleSaksbehandler) } returns mockk<OppgaveIntern>()
        coEvery { brevApiKlient.ferdigstillBrev(any(), any()) } returns
            BrevStatusResponse(brevId, no.nav.etterlatte.brev.model.Status.DISTRIBUERT)

        assertThrows<FeilIOppgave> {
            service.ferdigstillBrevOgOppgave(oppgaveId, simpleSaksbehandler)
        }
        verify(exactly = 0) { oppgaveService.ferdigstillOppgave(oppgaveId, simpleSaksbehandler) }
    }

    @Test
    fun `Skal kun ferdigstille oppgave hvis brev blir distribuert`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { status } returns Status.UNDER_BEHANDLING
            }
        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        val brevId = 1234L
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                brevId,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { oppgaveService.ferdigstillOppgave(oppgaveId, simpleSaksbehandler) } returns mockk<OppgaveIntern>()
        every { oppgaveService.sjekkOmKanFerdigstilleOppgave(any(), any()) } just Runs
        coEvery { brevApiKlient.ferdigstillBrev(any(), any()) } returns
            BrevStatusResponse(brevId, no.nav.etterlatte.brev.model.Status.DISTRIBUERT)
        service.ferdigstillBrevOgOppgave(oppgaveId, simpleSaksbehandler)
        verify(exactly = 1) { oppgaveService.ferdigstillOppgave(oppgaveId, simpleSaksbehandler) }
        coVerify(exactly = 1) { brevApiKlient.ferdigstillBrev(any(), any()) }
    }

    @Test
    fun `Skal ikke ferdigstille oppgave hvis brev ikke blir distribuert`() {
        val simpleSaksbehandler = simpleSaksbehandler()
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
            }

        val brevId = 1234L
        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                brevId,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { oppgaveService.ferdigstillOppgave(oppgaveId, simpleSaksbehandler) } returns mockk<OppgaveIntern>()
        every { oppgaveService.sjekkOmKanFerdigstilleOppgave(any(), any()) } just Runs
        coEvery { brevApiKlient.ferdigstillBrev(any(), any()) } returns
            BrevStatusResponse(brevId, no.nav.etterlatte.brev.model.Status.JOURNALFOERT)
        assertThrows<BrevBleIkkeFerdig> {
            service.ferdigstillBrevOgOppgave(oppgaveId, simpleSaksbehandler)
        }
        verify(exactly = 0) { oppgaveService.ferdigstillOppgave(oppgaveId, simpleSaksbehandler) }
    }

    @Test
    fun `Kan ikke lagre om oppgave er avsluttet`() {
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { status } returns Status.FERDIGSTILT
            }

        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")

        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                1234,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        val aktivitetspliktInformasjonBrevdataRequest = AktivitetspliktInformasjonBrevdataRequest(false)
        assertThrows<OppgaveErAvsluttet> {
            service.lagreBrevdata(oppgaveId, aktivitetspliktInformasjonBrevdataRequest)
        }
    }

    @Test
    fun `Skal fjerne brevid og slette brev om man lagrer nei til å sende brev etter å ha lagret med brevid`() {
        nyKontekstMedBruker(mockk<User>().also { every { it.name() } returns this::class.java.simpleName })
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { status } returns Status.UNDER_BEHANDLING
            }

        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")
        every { aktivitetspliktBrevDao.lagreBrevdata(any()) } returns 1
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                1234,
                false,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { aktivitetspliktBrevDao.fjernBrevId(oppgaveId, any()) } returns 1
        coEvery { brevApiKlient.slettBrev(any(), any(), any()) } just Runs
        val aktivitetspliktInformasjonBrevdataRequest = AktivitetspliktInformasjonBrevdataRequest(false)

        service.lagreBrevdata(oppgaveId, aktivitetspliktInformasjonBrevdataRequest)

        coVerify(exactly = 1) { brevApiKlient.slettBrev(any(), any(), any()) }
        verify(exactly = 1) { aktivitetspliktBrevDao.fjernBrevId(oppgaveId, any()) }
    }

    @Test
    fun `Fjerner ikke brev id om den ikke finnes selvom bruker har valgt nei til å sende brev`() {
        nyKontekstMedBruker(mockk<User>().also { every { it.name() } returns this::class.java.simpleName })
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { status } returns Status.UNDER_BEHANDLING
            }

        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")
        every { aktivitetspliktBrevDao.lagreBrevdata(any()) } returns 1
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                brevId = null,
                false,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { aktivitetspliktBrevDao.fjernBrevId(oppgaveId, any()) } returns 1
        coEvery { brevApiKlient.slettBrev(any(), any(), any()) } just Runs
        val aktivitetspliktInformasjonBrevdataRequest = AktivitetspliktInformasjonBrevdataRequest(false)

        service.lagreBrevdata(oppgaveId, aktivitetspliktInformasjonBrevdataRequest)

        coVerify(exactly = 0) { brevApiKlient.slettBrev(any(), any(), any()) }
        verify(exactly = 0) { aktivitetspliktBrevDao.fjernBrevId(oppgaveId, any()) }
    }

    @Test
    fun `Skal ikke fjerne brevid om valgt skal sende brev`() {
        nyKontekstMedBruker(mockk<User>().also { every { it.name() } returns this::class.java.simpleName })
        val oppgaveId = UUID.randomUUID()
        val sakIdForOppgave = sak.id
        every { oppgaveService.hentOppgave(oppgaveId) } returns
            mockk {
                every { sakId } returns sakIdForOppgave
                every { status } returns Status.UNDER_BEHANDLING
            }

        val kilde = Grunnlagsopplysning.Saksbehandler.create("ident")
        every { aktivitetspliktBrevDao.lagreBrevdata(any()) } returns 1
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns
            AktivitetspliktInformasjonBrevdata(
                oppgaveId,
                sakIdForOppgave,
                1234,
                true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = kilde,
            )
        every { aktivitetspliktBrevDao.fjernBrevId(oppgaveId, any()) } returns 1
        coEvery { brevApiKlient.slettBrev(any(), any(), any()) } just Runs
        val aktivitetspliktInformasjonBrevdataRequest = AktivitetspliktInformasjonBrevdataRequest(true)

        service.lagreBrevdata(oppgaveId, aktivitetspliktInformasjonBrevdataRequest)

        coVerify(exactly = 0) { brevApiKlient.slettBrev(any(), any(), any()) }
        verify(exactly = 0) { aktivitetspliktBrevDao.fjernBrevId(oppgaveId, any()) }
    }
}
