package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import no.nav.etterlatte.tilbakekreving.kravOgVedtakStatus
import no.nav.etterlatte.tilbakekreving.kravgrunnlag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KravgrunnlagServiceTest {
    private lateinit var kravgrunnlagService: KravgrunnlagService
    private lateinit var behandlingKlient: BehandlingKlient

    @BeforeEach
    fun beforeEach() {
        behandlingKlient = mockk<BehandlingKlient>()
        kravgrunnlagService = KravgrunnlagService(behandlingKlient)
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(behandlingKlient)
    }

    @Test
    fun `skal opprette ny tilbakekreving naar kravgrunnlag har status NY`() {
        val kravgrunnlag = kravgrunnlag(status = KravgrunnlagStatus.NY)

        coEvery { behandlingKlient.opprettTilbakekreving(any(), any()) } just runs

        kravgrunnlagService.haandterKravgrunnlag(kravgrunnlag)

        coVerify(exactly = 1) {
            behandlingKlient.opprettTilbakekreving(kravgrunnlag.sakId, kravgrunnlag)
        }
    }

    @Test
    fun `skal sette tilbakekreving tilbake fra paa vent naar kravgrunnlag har status ENDR og ingen perioder`() {
        val kravgrunnlag = kravgrunnlag(status = KravgrunnlagStatus.ENDR, perioder = emptyList())

        coEvery { behandlingKlient.endreOppgaveStatusForTilbakekreving(any(), any()) } just runs

        kravgrunnlagService.haandterKravgrunnlag(kravgrunnlag)

        coVerify(exactly = 1) {
            behandlingKlient.endreOppgaveStatusForTilbakekreving(kravgrunnlag.sakId, paaVent = false)
        }
    }

    @Test
    fun `skal avbryte gjeldende tilbakekreving og opprette ny naar kravgrunnlag har status ENDR og perioder`() {
        val kravgrunnlag = kravgrunnlag(status = KravgrunnlagStatus.ENDR)

        coEvery { behandlingKlient.avbrytTilbakekreving(any(), any()) } just runs
        coEvery { behandlingKlient.opprettTilbakekreving(any(), any()) } just runs

        kravgrunnlagService.haandterKravgrunnlag(kravgrunnlag)

        coVerify(exactly = 1) {
            behandlingKlient.avbrytTilbakekreving(kravgrunnlag.sakId, any())
            behandlingKlient.opprettTilbakekreving(kravgrunnlag.sakId, kravgrunnlag)
        }
    }

    @Test
    fun `skal feile naar kravgrunnlag har en status som ikke er forventet`() {
        val kravgrunnlag = kravgrunnlag(status = KravgrunnlagStatus.SPER)

        assertThrows<KanIkkeHaandtereStatusException> {
            kravgrunnlagService.haandterKravgrunnlag(kravgrunnlag)
        }
    }

    @Test
    fun `skal avbryte tilbakekreving naar kravOgVedtakstatus har status AVSL`() {
        val kravOgVedtakstatus = kravOgVedtakStatus(status = KravgrunnlagStatus.AVSL)

        coEvery { behandlingKlient.avbrytTilbakekreving(any(), any()) } just runs

        kravgrunnlagService.haandterKravOgVedtakStatus(kravOgVedtakstatus)

        coVerify(exactly = 1) {
            behandlingKlient.avbrytTilbakekreving(kravOgVedtakstatus.sakId, any())
        }
    }

    @Test
    fun `skal sette tilbakekreving paa vent naar kravOgVedtakstatus har status SPER`() {
        val kravOgVedtakstatus = kravOgVedtakStatus(status = KravgrunnlagStatus.SPER)

        coEvery { behandlingKlient.endreOppgaveStatusForTilbakekreving(any(), any()) } just runs

        kravgrunnlagService.haandterKravOgVedtakStatus(kravOgVedtakstatus)

        coVerify(exactly = 1) {
            behandlingKlient.endreOppgaveStatusForTilbakekreving(kravOgVedtakstatus.sakId, paaVent = true)
        }
    }

    @Test
    fun `skal sette tilbakekreving tilbake fra paa vent naar kravOgVedtakstatus har status ENDR`() {
        val kravOgVedtakstatus = kravOgVedtakStatus(status = KravgrunnlagStatus.ENDR)

        coEvery { behandlingKlient.endreOppgaveStatusForTilbakekreving(any(), any()) } just runs

        kravgrunnlagService.haandterKravOgVedtakStatus(kravOgVedtakstatus)

        coVerify(exactly = 1) {
            behandlingKlient.endreOppgaveStatusForTilbakekreving(kravOgVedtakstatus.sakId, paaVent = false)
        }
    }

    @Test
    fun `skal feile naar kravOgVedtakstatus har en status som ikke er forventet`() {
        val kravOgVedtakstatus = kravOgVedtakStatus(status = KravgrunnlagStatus.NY)

        assertThrows<KanIkkeHaandtereStatusException> {
            kravgrunnlagService.haandterKravOgVedtakStatus(kravOgVedtakstatus)
        }
    }
}
