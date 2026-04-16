package no.nav.etterlatte.behandling.etteroppgjoer.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class EtteroppgjoerOppgaveServiceTest {
    private val oppgaveService: OppgaveService = mockk()
    private val service = EtteroppgjoerOppgaveService(oppgaveService)

    @Test
    fun `skal kaste hvis det finnes mer enn to aapne oppgaver for opprette forbehandling`() {
        every { oppgaveService.hentOppgaverForSakAvType(sakId1, listOf(OppgaveType.ETTEROPPGJOER)) } returns
            listOf(
                oppgave(referanse = ""),
                oppgave(referanse = ""),
                oppgave(referanse = ""),
            )

        assertThrows(InternfeilException::class.java) {
            service.opprettOppgaveForOpprettForbehandling(sakId1, inntektsAar = 2024)
        }
    }

    @Test
    fun `skal kaste hvis oppgaven opprettes manuelt og det allerede finnes en aapen oppgave`() {
        every { oppgaveService.hentOppgaverForSakAvType(sakId1, listOf(OppgaveType.ETTEROPPGJOER)) } returns
            listOf(oppgave(referanse = ""))

        assertThrows(InternfeilException::class.java) {
            service.opprettOppgaveForOpprettForbehandling(
                sakId = sakId1,
                inntektsAar = 2024,
                opprettetManuelt = true,
            )
        }
    }

    @Test
    fun `skal ikke opprette ny oppgave hvis det allerede finnes en aapen automatisk oppgave`() {
        every { oppgaveService.hentOppgaverForSakAvType(sakId1, listOf(OppgaveType.ETTEROPPGJOER)) } returns
            listOf(oppgave(referanse = ""))

        service.opprettOppgaveForOpprettForbehandling(
            sakId = sakId1,
            inntektsAar = 2024,
            opprettetManuelt = false,
        )

        verify(exactly = 0) {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `skal opprette oppgave med standardmerknad naar ingen aapen oppgave finnes`() {
        every { oppgaveService.hentOppgaverForSakAvType(sakId1, listOf(OppgaveType.ETTEROPPGJOER)) } returns emptyList()
        every {
            oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId1,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Etteroppgjøret for 2024 er klart til behandling",
                frist = null,
                saksbehandler = null,
                gruppeId = null,
                gjelderAar = 2024,
            )
        } returns oppgave(referanse = "")

        service.opprettOppgaveForOpprettForbehandling(sakId1, inntektsAar = 2024)

        verify {
            oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId1,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Etteroppgjøret for 2024 er klart til behandling",
                frist = null,
                saksbehandler = null,
                gruppeId = null,
                gjelderAar = 2024,
            )
        }
    }

    @Test
    fun `skal opprette oppgave med custom merknad naar den er oppgitt`() {
        every { oppgaveService.hentOppgaverForSakAvType(sakId1, listOf(OppgaveType.ETTEROPPGJOER)) } returns emptyList()
        every {
            oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId1,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Manuell merknad",
                frist = null,
                saksbehandler = null,
                gruppeId = null,
                gjelderAar = 2024,
            )
        } returns oppgave(referanse = "")

        service.opprettOppgaveForOpprettForbehandling(
            sakId = sakId1,
            merknad = "Manuell merknad",
            inntektsAar = 2024,
        )

        verify {
            oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId1,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Manuell merknad",
                frist = null,
                saksbehandler = null,
                gruppeId = null,
                gjelderAar = 2024,
            )
        }
    }

    @Test
    fun `skal kaste hvis ingen oppgave finnes for forbehandling`() {
        val forbehandlingId = UUID.randomUUID()
        every { oppgaveService.hentOppgaverForReferanse(forbehandlingId.toString()) } returns emptyList()

        assertThrows(InternfeilException::class.java) {
            service.sjekkAtOppgavenErTildeltSaksbehandler(forbehandlingId, simpleSaksbehandler())
        }
    }

    @Test
    fun `skal kaste hvis oppgaven er tildelt annen saksbehandler`() {
        val forbehandlingId = UUID.randomUUID()
        every { oppgaveService.hentOppgaverForReferanse(forbehandlingId.toString()) } returns
            listOf(
                oppgave(
                    referanse = forbehandlingId.toString(),
                    saksbehandler = OppgaveSaksbehandler("annen-saksbehandler"),
                ),
            )

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                service.sjekkAtOppgavenErTildeltSaksbehandler(forbehandlingId, simpleSaksbehandler())
            }

        assertEquals("IKKE_TILGANG_TIL_BEHANDLING", exception.code)
    }

    @Test
    fun `skal kaste hvis eneste oppgave er avsluttet`() {
        val forbehandlingId = UUID.randomUUID()
        every { oppgaveService.hentOppgaverForReferanse(forbehandlingId.toString()) } returns
            listOf(
                oppgave(
                    referanse = forbehandlingId.toString(),
                    status = Status.FERDIGSTILT,
                    saksbehandler = OppgaveSaksbehandler("saksbehandler"),
                ),
            )

        val exception =
            assertThrows(UgyldigForespoerselException::class.java) {
                service.sjekkAtOppgavenErTildeltSaksbehandler(forbehandlingId, simpleSaksbehandler())
            }

        assertEquals("OPPGAVE_AVSLUTTET", exception.code)
    }

    private fun oppgave(
        referanse: String,
        status: Status = Status.UNDER_BEHANDLING,
        saksbehandler: OppgaveSaksbehandler? = null,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = status,
        enhet = sak().enhet,
        sakId = sakId1,
        kilde = OppgaveKilde.BEHANDLING,
        type = OppgaveType.ETTEROPPGJOER,
        saksbehandler = saksbehandler,
        referanse = referanse,
        gruppeId = null,
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = sak().sakType,
        fnr = sak().ident,
        frist = null,
    )
}
