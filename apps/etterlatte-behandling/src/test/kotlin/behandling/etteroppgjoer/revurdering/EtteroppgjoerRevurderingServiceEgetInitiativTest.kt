package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelser
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month.JANUARY
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerRevurderingServiceEgetInitiativTest {
    private val sakId = SakId(1L)
    private val inntektsaar = 2024

    private val etteroppgjoerService = mockk<EtteroppgjoerService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val etteroppgjoerForbehandlingService = mockk<EtteroppgjoerForbehandlingService>()

    private val service =
        spyk(
            EtteroppgjoerRevurderingService(
                behandlingService = behandlingService,
                etteroppgjoerService = etteroppgjoerService,
                etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
                grunnlagService = mockk<GrunnlagService>(),
                revurderingService = mockk<RevurderingService>(),
                vilkaarsvurderingService = mockk<VilkaarsvurderingService>(),
                trygdetidKlient = mockk<TrygdetidKlient>(),
                beregningKlient = mockk<BeregningKlient>(),
                etteroppgjoerDataService = mockk<EtteroppgjoerDataService>(),
            ),
        )

    @BeforeEach
    fun setup() {
        mockkStatic("no.nav.etterlatte.ContekstKt")
        every { inTransaction(any<() -> Any?>()) } answers { firstArg<() -> Any?>().invoke() }
    }

    @AfterEach
    fun teardown() {
        unmockkStatic("no.nav.etterlatte.ContekstKt")
    }

    private fun etteroppgjoer(
        status: EtteroppgjoerStatus,
        sisteFerdigstilteForbehandling: UUID? = UUID.randomUUID(),
    ) = Etteroppgjoer(
        sakId = sakId,
        inntektsaar = inntektsaar,
        status = status,
        sisteFerdigstilteForbehandling = sisteFerdigstilteForbehandling,
    )

    @Test
    fun `kaster feil naar etteroppgjoeret ikke er iverksatt`() {
        every { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar) } returns
            etteroppgjoer(EtteroppgjoerStatus.VENTER_PAA_SVAR)

        assertThrows<IkkeTillattException> {
            service.omgjoerEtteroppgjoerRevurderingEgetInitiativ(sakId, inntektsaar, simpleSaksbehandler())
        }

        verify(exactly = 0) {
            etteroppgjoerService.oppdaterEtteroppgjoerStatus(any(), any(), any(), any())
        }
    }

    @Test
    fun `setter status OMGJOERING og oppretter revurdering fra sisteFerdigstilteForbehandling`() {
        val forbehandlingId = UUID.randomUUID()
        val etteroppgjoer = etteroppgjoer(EtteroppgjoerStatus.FERDIGSTILT, forbehandlingId)
        every { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar) } returns
            etteroppgjoer

        val revurderingForbehandlingId = UUID.randomUUID()
        val revurderingForbehandling =
            mockk<EtteroppgjoerForbehandling> {
                every { id } returns revurderingForbehandlingId
                every { aar } returns inntektsaar
            }
        val revurdering =
            revurdering(
                status = BehandlingStatus.IVERKSATT,
                revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                relatertBehandlingId = revurderingForbehandlingId,
                sistEndret = YearMonth.of(inntektsaar + 1, JANUARY).atDay(20).atStartOfDay(),
                opprinnelse = BehandlingOpprinnelse.SAKSBEHANDLER,
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
            )
        every { etteroppgjoerForbehandlingService.hentForbehandling(revurderingForbehandlingId) } returns revurderingForbehandling
        val nyEoRevurdering = mockk<Revurdering>()
        val opprinnelseSlot = slot<BehandlingOpprinnelse>()
        val omgjoerForbehandlingIdSlot = slot<UUID>()
        every {
            service.opprettEtteroppgjoerRevurdering(
                sakId = sakId,
                inntektsaar = inntektsaar,
                opprinnelse = capture(opprinnelseSlot),
                omgjoerForbehandlingId = capture(omgjoerForbehandlingIdSlot),
                brukerTokenInfo = any(),
            )
        } returns nyEoRevurdering
        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(revurdering)

        val resultat =
            service.omgjoerEtteroppgjoerRevurderingEgetInitiativ(sakId, inntektsaar, simpleSaksbehandler())

        resultat shouldBe nyEoRevurdering
        opprinnelseSlot.captured shouldBe BehandlingOpprinnelse.SAKSBEHANDLER
        omgjoerForbehandlingIdSlot.captured shouldBe forbehandlingId
        verify(exactly = 1) {
            etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sakId,
                inntektsaar,
                EtteroppgjoerStatus.OMGJOERING,
                EtteroppgjoerHendelser.OMGJOERING,
            )
        }
    }
}
