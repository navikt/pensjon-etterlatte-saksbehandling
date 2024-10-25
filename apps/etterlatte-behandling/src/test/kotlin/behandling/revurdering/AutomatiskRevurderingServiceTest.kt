package behandling.revurdering

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.OmregningAvSakUnderSamordning
import no.nav.etterlatte.behandling.revurdering.OmregningKreverLoependeVedtak
import no.nav.etterlatte.behandling.revurdering.OmregningOverstyrtBeregning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.nyKontekstMedBruker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class AutomatiskRevurderingServiceTest {
    val forrigeBehandlingId = UUID.randomUUID()

    val vedtakKlient = mockk<VedtakKlient>()

    val beregningKlient =
        mockk<BeregningKlient> {
            coEvery { harOverstyrt(any(), any()) } returns false
        }

    val service =
        AutomatiskRevurderingService(
            revurderingService =
                mockk {
                    every {
                        opprettRevurdering(
                            sakId = any(),
                            persongalleri = any(),
                            forrigeBehandling = any(),
                            mottattDato = any(),
                            prosessType = any(),
                            kilde = any(),
                            revurderingAarsak = any(),
                            virkningstidspunkt = any(),
                            utlandstilknytning = any(),
                            boddEllerArbeidetUtlandet = any(),
                            begrunnelse = any(),
                            saksbehandlerIdent = any(),
                            frist = any(),
                            opphoerFraOgMed = any(),
                        )
                    } returns
                        mockk {
                            every { leggInnGrunnlag } returns {}
                            every { opprettOgTildelOppgave } returns {}
                            every { sendMeldingForHendelse } returns {}
                            every { behandlingId() } returns UUID.randomUUID()
                            every { sakType() } returns mockk()
                        }
                },
            behandlingService =
                mockk {
                    every { hentBehandling(any()) } returns
                        mockk<Behandling> {
                            every { id } returns forrigeBehandlingId
                            every { utlandstilknytning } returns mockk()
                            every { boddEllerArbeidetUtlandet } returns mockk()
                            every { opphoerFraOgMed } returns mockk()
                        }
                },
            grunnlagService =
                mockk {
                    coEvery { hentPersongalleri(any()) } returns mockk()
                },
            vedtakKlient = vedtakKlient,
            beregningKlient = beregningKlient,
        )

    val systembruker = mockk<Systembruker>()
    val user =
        mockk<SystemUser> {
            every { brukerTokenInfo } returns systembruker
        }

    @BeforeEach
    fun setup() {
        nyKontekstMedBruker(user.also { every { it.name() } returns this::class.java.simpleName })
    }

    @Test
    fun `Opprettelse under feil`() {
        val vedtak =
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = false,
                dato = LocalDate.now(),
                sisteLoependeBehandlingId = forrigeBehandlingId,
            )
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns vedtak

        val request =
            AutomatiskRevurderingRequest(
                randomSakId(),
                LocalDate.now(),
                Revurderingaarsak.OMREGNING,
            )
        runBlocking {
            service.oppprettRevurderingOgOppfoelging(request, systembruker)
        }
    }

    @Test
    fun `Omregning skal feile hvis ikke løpende ytelse`() {
        val vedtak =
            LoependeYtelseDTO(
                erLoepende = false,
                underSamordning = false,
                dato = LocalDate.now(),
                sisteLoependeBehandlingId = forrigeBehandlingId,
            )
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns vedtak

        val request =
            AutomatiskRevurderingRequest(
                randomSakId(),
                LocalDate.now(),
                Revurderingaarsak.OMREGNING,
            )
        assertThrows<OmregningKreverLoependeVedtak> {
            runBlocking {
                service.oppprettRevurderingOgOppfoelging(request, systembruker)
            }
        }
    }

    @Test
    fun `Omregning skal feile hvis under samordning`() {
        val vedtak =
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = true,
                dato = LocalDate.now(),
                sisteLoependeBehandlingId = forrigeBehandlingId,
            )
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns vedtak

        val request =
            AutomatiskRevurderingRequest(
                randomSakId(),
                LocalDate.now(),
                Revurderingaarsak.OMREGNING,
            )
        assertThrows<OmregningAvSakUnderSamordning> {
            runBlocking {
                service.oppprettRevurderingOgOppfoelging(request, systembruker)
            }
        }
    }

    @Test
    fun `Omregning skal feile hvis aktiv overstyrt beregning`() {
        val vedtak =
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = false,
                dato = LocalDate.now(),
                sisteLoependeBehandlingId = forrigeBehandlingId,
            )
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns vedtak
        coEvery { beregningKlient.harOverstyrt(any(), any()) } returns true

        val request =
            AutomatiskRevurderingRequest(
                randomSakId(),
                LocalDate.now(),
                Revurderingaarsak.OMREGNING,
            )
        assertThrows<OmregningOverstyrtBeregning> {
            runBlocking {
                service.oppprettRevurderingOgOppfoelging(request, systembruker)
            }
        }
    }
}
