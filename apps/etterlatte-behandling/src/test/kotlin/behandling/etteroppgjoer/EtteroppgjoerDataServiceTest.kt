package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.UUID

class EtteroppgjoerDataServiceTest {
    private val service = EtteroppgjoerDataService(mockk(), mockk())

    private fun vedtak(
        type: VedtakType = VedtakType.INNVILGELSE,
        datoAttestert: ZonedDateTime? = ZonedDateTime.now(),
        opphoerFraOgMed: YearMonth? = null,
    ) = VedtakSammendragDto(
        id = UUID.randomUUID().toString(),
        behandlingId = UUID.randomUUID(),
        vedtakType = type,
        behandlendeSaksbehandler = null,
        datoFattet = null,
        attesterendeSaksbehandler = null,
        datoAttestert = datoAttestert,
        virkningstidspunkt = null,
        opphoerFraOgMed = opphoerFraOgMed,
    )

    // sisteVedtakMedAvkorting

    @Test
    fun `sisteVedtakMedAvkorting returnerer siste ikke-opphoor vedtak`() {
        val innvilgelse = vedtak(VedtakType.INNVILGELSE, datoAttestert = ZonedDateTime.now().minusDays(2))
        val opphoer = vedtak(VedtakType.OPPHOER, datoAttestert = ZonedDateTime.now().minusDays(1))

        val resultat = service.sisteVedtakMedAvkorting(listOf(innvilgelse, opphoer))

        resultat.behandlingId shouldBe innvilgelse.behandlingId
    }

    @Test
    fun `sisteVedtakMedAvkorting returnerer nyeste naar flere ikke-opphoor vedtak`() {
        val gammel = vedtak(VedtakType.INNVILGELSE, datoAttestert = ZonedDateTime.now().minusDays(10))
        val ny = vedtak(VedtakType.ENDRING, datoAttestert = ZonedDateTime.now().minusDays(1))

        val resultat = service.sisteVedtakMedAvkorting(listOf(gammel, ny))

        resultat.behandlingId shouldBe ny.behandlingId
    }

    @Test
    fun `sisteVedtakMedAvkorting kaster hvis alle vedtak er OPPHOER`() {
        val vedtakListe = listOf(vedtak(VedtakType.OPPHOER))

        shouldThrow<NoSuchElementException> {
            service.sisteVedtakMedAvkorting(vedtakListe)
        }
    }

    // vedtakMedGjeldendeOpphoer

    @Test
    fun `vedtakMedGjeldendeOpphoer returnerer null naar ingen opphoor`() {
        val vedtakListe = listOf(vedtak(VedtakType.INNVILGELSE))

        service.vedtakMedGjeldendeOpphoer(vedtakListe) shouldBe null
    }

    @Test
    fun `vedtakMedGjeldendeOpphoer returnerer OPPHOER-vedtaket naar siste er OPPHOER`() {
        val innvilgelse = vedtak(VedtakType.INNVILGELSE, datoAttestert = ZonedDateTime.now().minusDays(2))
        val opphoer = vedtak(VedtakType.OPPHOER, datoAttestert = ZonedDateTime.now().minusDays(1))

        val resultat = service.vedtakMedGjeldendeOpphoer(listOf(innvilgelse, opphoer))

        resultat?.behandlingId shouldBe opphoer.behandlingId
    }

    @Test
    fun `vedtakMedGjeldendeOpphoer returnerer avkorting-vedtaket naar det har opphoerFraOgMed satt`() {
        val medOpphoer =
            vedtak(
                VedtakType.INNVILGELSE,
                datoAttestert = ZonedDateTime.now().minusDays(1),
                opphoerFraOgMed = YearMonth.of(2024, 6),
            )

        val resultat = service.vedtakMedGjeldendeOpphoer(listOf(medOpphoer))

        resultat?.behandlingId shouldBe medOpphoer.behandlingId
    }
}
