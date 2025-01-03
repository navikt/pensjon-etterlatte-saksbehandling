package no.nav.etterlatte.behandling.brevutsendelse

import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.GYLDIG_MOTTAKER
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.UGYLDIG_MOTTAKER_UTDATERT_IDENT
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

internal class BrevutsendelseServiceTest {
    private val sakService: SakService = mockk()
    private val sjekkBrevMottakerService: SjekkBrevMottakerService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val brevKlient: BrevApiKlient = mockk()

    private val brevutsendelseService: BrevutsendelseService =
        BrevutsendelseService(
            sakService = sakService,
            sjekkBrevMottakerService = sjekkBrevMottakerService,
            oppgaveService = oppgaveService,
            brevKlient = brevKlient,
        )

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
        coEvery { brevKlient.ferdigstillJournalFoerOgDistribuerBrev(any(), any()) } returns
            mockk {
                every { brevId } returns 1
                every { status } returns Status.FERDIGSTILT
            }

        brevutsendelseService.prosesserBrevutsendelse(brevutsendelse, mockk { every { ident() } returns "ident" })

        verifyOrder {
            sakService.finnSak(any())
            sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(sak, any())
        }

        coVerifyOrder {
            brevKlient.opprettSpesifiktBrev(sak.id, any(), any())
            brevKlient.ferdigstillJournalFoerOgDistribuerBrev(any(), any())
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

        brevutsendelseService.prosesserBrevutsendelse(brevutsendelse, mockk { every { ident() } returns "ident" })

        verifyOrder {
            sakService.finnSak(any())
            sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(sak, any())
            oppgaveService.opprettOppgave(any(), sak.id, any(), any(), any(), any(), any(), any())
        }

        confirmVerified(sjekkBrevMottakerService, sakService, oppgaveService)
    }

    private fun sak() = Sak(SOEKER_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, SakId(1), Enhetsnummer("1234"))

    private fun brevutsendelse(sakId: SakId): Brevutsendelse =
        opprettNyBrevutsendelse(
            sakId = sakId,
            type = BrevutsendelseType.TREKKPLIKT_2025,
            merknad = null,
        )
}
