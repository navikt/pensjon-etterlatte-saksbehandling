package no.nav.etterlatte.behandling.aktivitetsplikt

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
    private val service =
        AktivitetspliktOppgaveService(
            aktivitetspliktService = aktivitetspliktService,
            oppgaveService = oppgaveService,
            sakService = sakService,
            aktivitetspliktBrevDao,
            mockk(relaxed = true),
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
        every { aktivitetspliktService.hentVurderingForOppgave(oppgave.id) } returns null
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
        every { aktivitetspliktService.hentVurderingForOppgave(oppgave.id) } returns null
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
        every { aktivitetspliktService.hentVurderingForOppgave(oppgave.id) } returns
            AktivitetspliktVurdering(
                emptyList(),
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

        val skalIkkeSendebrev = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, false)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalIkkeSendebrev
        service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        verify(exactly = 0) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 0) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
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

        val skalIkkeSendebrev = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, true, redusertEtterInntekt = true)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalIkkeSendebrev
        assertThrows<ManglerBrevdata> { service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler) }
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

        val skalIkkeSendebrev = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, true, utbetaling = true)
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

        val brevIdfinnes = AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, 2L, true)
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
            )
        every { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) } returns
            mockk {
                every { aktivitet } returns listOf(aksgrad)
            }
        every { aktivitetspliktBrevDao.lagreBrevId(oppgaveId, any()) } returns 1
        val skalSendeBrev =
            AktivitetspliktInformasjonBrevdata(oppgaveId, sakIdForOppgave, null, true, utbetaling = true, redusertEtterInntekt = true)
        every { aktivitetspliktBrevDao.hentBrevdata(oppgaveId) } returns skalSendeBrev

        service.opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(oppgaveId, simpleSaksbehandler)
        verify(exactly = 1) { aktivitetspliktBrevDao.lagreBrevId(any(), any()) }
        verify(exactly = 1) { aktivitetspliktService.hentVurderingForOppgave(oppgaveId) }
    }
}
