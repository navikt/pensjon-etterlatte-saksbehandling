package no.nav.etterlatte.behandling.aktivitetsplikt

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AktivitetspliktOppgaveServiceTest {
    private val aktivitetspliktService: AktivitetspliktService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val sakService: SakService = mockk()
    private val service =
        AktivitetspliktOppgaveService(
            aktivitetspliktService = aktivitetspliktService,
            oppgaveService = oppgaveService,
            sakService = sakService,
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
}
