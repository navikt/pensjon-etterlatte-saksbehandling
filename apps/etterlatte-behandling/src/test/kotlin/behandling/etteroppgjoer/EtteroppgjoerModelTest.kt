package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.sakId1
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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

    @ParameterizedTest
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["VENTER_PAA_SVAR", "FERDIGSTILT", "OMGJOERING"],
    )
    fun `kanOppretteRevurdering er true for gyldige statuser`(status: EtteroppgjoerStatus) {
        etteroppgjoer(status).kanOppretteRevurdering() shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["VENTER_PAA_SVAR", "FERDIGSTILT", "OMGJOERING"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `kanOppretteRevurdering er false for ugyldige statuser`(status: EtteroppgjoerStatus) {
        etteroppgjoer(status).kanOppretteRevurdering() shouldBe false
    }

    // kanOppretteForbehandling

    @ParameterizedTest
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["MOTTATT_SKATTEOPPGJOER", "MANGLER_SKATTEOPPGJOER"],
    )
    fun `kanOppretteForbehandling er true for gyldige statuser`(status: EtteroppgjoerStatus) {
        etteroppgjoer(status).kanOppretteForbehandling() shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["MOTTATT_SKATTEOPPGJOER", "MANGLER_SKATTEOPPGJOER"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `kanOppretteForbehandling er false for ugyldige statuser`(status: EtteroppgjoerStatus) {
        etteroppgjoer(status).kanOppretteForbehandling() shouldBe false
    }
}
