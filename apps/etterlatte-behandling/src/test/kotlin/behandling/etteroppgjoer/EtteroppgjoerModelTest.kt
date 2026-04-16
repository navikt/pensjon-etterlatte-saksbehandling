package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.sakId1
import org.junit.jupiter.api.Test

class EtteroppgjoerModelTest {
    private fun etteroppgjoer(status: EtteroppgjoerStatus) =
        Etteroppgjoer(
            sakId = sakId1,
            inntektsaar = 2024,
            status = status,
        )

    // tilbakestill

    @Test
    fun `tilbakestill med erEndringTilUgunst gir MOTTATT_SKATTEOPPGJOER`() {
        val tilbakestilt = etteroppgjoer(EtteroppgjoerStatus.UNDER_REVURDERING).tilbakestill(erEndringTilUgunst = true)

        tilbakestilt.status shouldBe EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
    }

    @Test
    fun `tilbakestill uten erEndringTilUgunst gir VENTER_PAA_SVAR`() {
        val tilbakestilt = etteroppgjoer(EtteroppgjoerStatus.UNDER_REVURDERING).tilbakestill(erEndringTilUgunst = false)

        tilbakestilt.status shouldBe EtteroppgjoerStatus.VENTER_PAA_SVAR
    }

    @Test
    fun `tilbakestill virker fra OMGJOERING`() {
        val tilbakestilt = etteroppgjoer(EtteroppgjoerStatus.OMGJOERING).tilbakestill(erEndringTilUgunst = true)

        tilbakestilt.status shouldBe EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
    }

    @Test
    fun `tilbakestill kaster ved ugyldig status`() {
        shouldThrow<Exception> {
            etteroppgjoer(EtteroppgjoerStatus.VENTER_PAA_SVAR).tilbakestill(erEndringTilUgunst = true)
        }
    }

    // kanOppretteRevurdering

    @Test
    fun `kanOppretteRevurdering er true for VENTER_PAA_SVAR, FERDIGSTILT og OMGJOERING`() {
        etteroppgjoer(EtteroppgjoerStatus.VENTER_PAA_SVAR).kanOppretteRevurdering() shouldBe true
        etteroppgjoer(EtteroppgjoerStatus.FERDIGSTILT).kanOppretteRevurdering() shouldBe true
        etteroppgjoer(EtteroppgjoerStatus.OMGJOERING).kanOppretteRevurdering() shouldBe true
    }

    @Test
    fun `kanOppretteRevurdering er false for statuser som ikke er gyldige`() {
        etteroppgjoer(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER).kanOppretteRevurdering() shouldBe false
        etteroppgjoer(EtteroppgjoerStatus.UNDER_REVURDERING).kanOppretteRevurdering() shouldBe false
        etteroppgjoer(EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER).kanOppretteRevurdering() shouldBe false
    }

    // kanOppretteForbehandling

    @Test
    fun `kanOppretteForbehandling er true for MOTTATT_SKATTEOPPGJOER og MANGLER_SKATTEOPPGJOER`() {
        etteroppgjoer(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER).kanOppretteForbehandling() shouldBe true
        etteroppgjoer(EtteroppgjoerStatus.MANGLER_SKATTEOPPGJOER).kanOppretteForbehandling() shouldBe true
    }

    @Test
    fun `kanOppretteForbehandling er false for alle andre statuser`() {
        etteroppgjoer(EtteroppgjoerStatus.VENTER_PAA_SVAR).kanOppretteForbehandling() shouldBe false
        etteroppgjoer(EtteroppgjoerStatus.FERDIGSTILT).kanOppretteForbehandling() shouldBe false
        etteroppgjoer(EtteroppgjoerStatus.UNDER_REVURDERING).kanOppretteForbehandling() shouldBe false
        etteroppgjoer(EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER).kanOppretteForbehandling() shouldBe false
    }
}
