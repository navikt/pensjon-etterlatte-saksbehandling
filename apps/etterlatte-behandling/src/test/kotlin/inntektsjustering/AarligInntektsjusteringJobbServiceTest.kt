package no.nav.etterlatte.inntektsjustering

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class AarligInntektsjusteringJobbServiceTest {
    private val omregningService: OmregningService = mockk()
    private val sakService: SakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val grunnlagService: GrunnlagService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val vedtakKlient: VedtakKlient = mockk()
    private val beregningKlient: BeregningKlient = mockk()
    private val pdlTjenesterKlient: PdlTjenesterKlient = mockk()
    private val rapid: KafkaProdusent<String, String> = mockk()

    val service =
        AarligInntektsjusteringJobbService(
            omregningService,
            sakService,
            behandlingService,
            oppgaveService,
            grunnlagService,
            vedtakKlient,
            beregningKlient,
            pdlTjenesterKlient,
            rapid,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()

        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtdelseDto()
        coEvery { beregningKlient.sakHarInntektForAar(any(), any(), any()) } returns false
        every { sakService.finnSak(SakId(123L)) } returns gyldigSak
        coEvery { pdlTjenesterKlient.hentPdlIdentifikator(any()) } returns
            PdlIdentifikator.FolkeregisterIdent(
                Folkeregisteridentifikator.of(fnrGyldigSak),
            )
        every { rapid.publiser(any(), any()) } returns Pair(1, 1L)
    }

    @Test
    fun `starter jobb for gyldig sak`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        runBlocking {
            service.startAarligInntektsjustering(request)
        }

        verify {
            rapid.publiser(
                "aarlig-inntektsjustering-123",
                withArg {
                    // TODO
                },
            )
        }
    }

    @Test
    fun `Sak som ikke er loepende skal ferdigstilles`() {
        // TODO
    }

    @Test
    fun `Sak som allerede har inntekt neste aar skal ferdigstilles`() {
        // TODO
    }

    @Test
    fun `Sak hvor ident har endret seg skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        val nyttFnr = Folkeregisteridentifikator.of("22511075258")
        coEvery { pdlTjenesterKlient.hentPdlIdentifikator(any()) } returns PdlIdentifikator.FolkeregisterIdent(nyttFnr)

        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk()
        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjustering(request)
        }

        verify {
            oppgaveService.opprettOppgave(
                "123",
                SakId(123L),
                null,
                OppgaveType.REVURDERING,
                merknad = "",
            )
        }
        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.FERDIGSTILT
                        sakId shouldBe SakId(123L)
                    }
                },
            )
        }
    }

    @Test
    fun `et eller annet feilhaandtering`() {
        // TODO
    }

    companion object {
        val fnrGyldigSak = "10418305857"
        val gyldigSak =
            Sak(
                fnrGyldigSak,
                SakType.OMSTILLINGSSTOENAD,
                SakId(123L),
                Enhetsnummer("1234"),
            )

        fun loependeYtdelseDto() =
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = false,
                dato = LocalDate.of(2024, 6, 1),
            )
    }
}
