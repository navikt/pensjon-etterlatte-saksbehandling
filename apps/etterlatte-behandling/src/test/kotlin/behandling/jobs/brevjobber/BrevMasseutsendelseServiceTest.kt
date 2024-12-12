package no.nav.etterlatte.behandling.jobs.brevjobber

import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.etterlatte.behandling.jobs.brevjobber.SjekkGyldigBrevMottakerResultat.GYLDIG_MOTTAKER
import no.nav.etterlatte.behandling.jobs.brevjobber.SjekkGyldigBrevMottakerResultat.UGYLDIG_MOTTAKER_UTDATERT_IDENT
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

internal class BrevMasseutsendelseServiceTest {
    private val sakService: SakService = mockk()
    private val sjekkBrevMottakerService: SjekkBrevMottakerService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val brevKlient: BrevApiKlient = mockk()

    private val brevMasseutsendelseService: BrevMasseutsendelseService =
        BrevMasseutsendelseService(
            sakService = sakService,
            sjekkBrevMottakerService = sjekkBrevMottakerService,
            oppgaveService = oppgaveService,
            brevKlient = brevKlient,
        )

    @BeforeEach
    fun beforeEach() {
    }

    @Test
    fun `skal opprette oppgave, sende brev og ferdigstille oppgave`() {
        val sak = sak()
        val brevutsendelse = brevutsendelse(sak.id)
        val oppgaveId = randomUUID()

        every { sakService.finnSak(any()) } returns sak
        every { sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(any(), any()) } returns GYLDIG_MOTTAKER

        every { oppgaveService.opprettOppgave(brevutsendelse.id.toString(), sak.id, any(), any(), any(), any(), any(), any()) } returns
            mockk { every { id } returns oppgaveId }

        coEvery { brevKlient.opprettSpesifiktBrev(any(), any(), any()) } returns mockk { every { id } returns 1 }
        coEvery { brevKlient.ferdigstillBrev(any(), any()) } returns
            mockk {
                every { brevId } returns 1
                every { status } returns Status.FERDIGSTILT
            }
        coEvery { brevKlient.journalfoerBrev(any(), any(), any()) } returns mockk { every { journalpostId } returns listOf("1") }
        coEvery { brevKlient.distribuerBrev(any(), any(), any()) } returns mockk { every { bestillingsId } returns listOf("1") }

        brevMasseutsendelseService.prosesserBrevutsendelse(brevutsendelse, mockk { every { ident() } returns "ident" })

        verifyOrder {
            sakService.finnSak(any())
            sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(sak, any())
        }

        coVerifyOrder {
            brevKlient.opprettSpesifiktBrev(sak.id, any(), any())
            brevKlient.ferdigstillBrev(any(), any())
            brevKlient.journalfoerBrev(sak.id, any(), any())
            brevKlient.distribuerBrev(sak.id, any(), any())
        }

        confirmVerified(sjekkBrevMottakerService, sakService, brevKlient)
    }

    @Test
    fun `skal opprette manuell oppgave hvis brevmottaker ikke er gyldig`() {
        val sak = sak()
        val brevutsendelse = brevutsendelse(sak.id)
        val oppgaveId = randomUUID()

        every { sakService.finnSak(any()) } returns sak
        every { sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(any(), any()) } returns UGYLDIG_MOTTAKER_UTDATERT_IDENT

        every { oppgaveService.opprettOppgave(brevutsendelse.id.toString(), sak.id, any(), any(), any(), any(), any(), any()) } returns
            mockk { every { id } returns oppgaveId }

        brevMasseutsendelseService.prosesserBrevutsendelse(brevutsendelse, mockk { every { ident() } returns "ident" })

        verifyOrder {
            sakService.finnSak(any())
            sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(sak, any())
            oppgaveService.opprettOppgave(any(), sak.id, any(), any(), any(), any(), any(), any())
        }

        confirmVerified(sjekkBrevMottakerService, sakService, oppgaveService)
    }

    private fun sak() = Sak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, SakId(1), Enhetsnummer("1234"))

    private fun brevutsendelse(sakId: SakId): Arbeidsjobb =
        lagNyArbeidsJobb(
            sakId = sakId,
            type = JobbType.TREKKPLIKT_2025,
            merknad = null,
        )
}
