package no.nav.etterlatte.oppgave

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

class OppgaveServiceAapneOppgaverTest {
    private val sakId = SakId(123L)
    private val oppgaveDao = mockk<OppgaveDaoMedEndringssporing>()
    private val service = OppgaveService(oppgaveDao, mockk(), mockk(), mockk(), mockk())

    @Test
    fun `sak uten oppgaver har ikke aapne oppgaver`() {
        every { oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries) } returns emptyList()

        service.harSakAapenBehandlingOppgave(sakId) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(Status::class)
    fun `bare uavsluttede forbehandlingsoppgaver blokkerer`(status: Status) {
        every { oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries) } returns
            listOf(oppgave(status = status, type = OppgaveType.ETTEROPPGJOER))

        service.harSakAapenBehandlingOppgave(sakId) shouldBe
            (status in setOf(Status.NY, Status.UNDER_BEHANDLING, Status.PAA_VENT, Status.ATTESTERING, Status.UNDERKJENT))
    }

    @ParameterizedTest
    @EnumSource(Status::class)
    fun `bare uavsluttede behandlingsoppgaver regnes som aapne`(status: Status) {
        every { oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries) } returns
            listOf(oppgave(status = status))

        service.harSakAapenBehandlingOppgave(sakId) shouldBe
            (status in setOf(Status.NY, Status.UNDER_BEHANDLING, Status.PAA_VENT, Status.ATTESTERING, Status.UNDERKJENT))
    }

    @ParameterizedTest
    @EnumSource(OppgaveKilde::class, names = ["BEHANDLING"], mode = EnumSource.Mode.EXCLUDE)
    fun `aapne oppgaver med andre kilder skal ikke blokkere`(kilde: OppgaveKilde) {
        every { oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries) } returns
            listOf(oppgave(kilde = kilde))

        service.harSakAapenBehandlingOppgave(sakId) shouldBe false
    }

    @Test
    fun `aapen forbehandlingsoppgave blokkerer selv uten tilhoerende behandling`() {
        every { oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries) } returns
            listOf(
                oppgave(status = Status.FERDIGSTILT),
                oppgave(kilde = OppgaveKilde.BRUKERDIALOG),
                oppgave(type = OppgaveType.ETTEROPPGJOER),
            )

        service.harSakAapenBehandlingOppgave(sakId) shouldBe true
    }

    @Test
    fun `aapen oppgave uten kilde skal ikke blokkere`() {
        every { oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries) } returns
            listOf(oppgave(kilde = null))

        service.harSakAapenBehandlingOppgave(sakId) shouldBe false
    }

    private fun oppgave(
        status: Status = Status.NY,
        kilde: OppgaveKilde? = OppgaveKilde.BEHANDLING,
        type: OppgaveType = OppgaveType.REVURDERING,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = status,
        enhet = Enhetsnummer("1234"),
        sakId = sakId,
        kilde = kilde,
        type = type,
        referanse = UUID.randomUUID().toString(),
        gruppeId = null,
        opprettet = Tidspunkt.now(),
        sakType = SakType.OMSTILLINGSSTOENAD,
        frist = null,
    )
}
