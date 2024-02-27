package no.nav.etterlatte.rivers

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.varselbrev.VarselbrevResponse
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.migrering.OpprettVarselbrevForGjenopprettaRiver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettVarselbrevForGjenopprettaRiverTest {
    private val varselbrevService = mockk<VarselbrevService>()

    private val brevId: Long = 2
    private val saksnr = 41L

    private val brevhaandterer = mockk<FerdigstillJournalfoerOgDistribuerBrev>()

    private val behandlingKlient = mockk<BehandlingKlient>()

    private val featureToggleService = mockk<FeatureToggleService>()

    private val opprettBrevRapid =
        TestRapid().apply {
            OpprettVarselbrevForGjenopprettaRiver(
                this,
                varselbrevService,
                brevhaandterer,
                behandlingKlient,
                featureToggleService,
            )
        }

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun before() = clearMocks(varselbrevService)

    @AfterEach
    fun after() = confirmVerified(varselbrevService)

    @Test
    fun `oppretter for gjenoppretting nytt brev, genererer pdf, journalfører, distribuerer og setter paa vent`() {
        val brev = opprettBrev()
        val response = VarselbrevResponse(brev, mockk(), Brevkoder.BP_VARSEL)

        coEvery { varselbrevService.opprettVarselbrev(any(), behandlingId, any()) } returns response
        coEvery { varselbrevService.ferdigstillOgGenererPDF(any(), any(), any()) } returns Pdf(ByteArray(0))

        coEvery { featureToggleService.isEnabled(any(), any()) } returns true
        coEvery { brevhaandterer.journalfoerOgDistribuer(any(), any(), any(), any()) } just runs

        opprettBrevRapid.apply { sendTestMessage(opprettMelding(Vedtaksloesning.GJENOPPRETTA).toJson()) }

        coVerify {
            varselbrevService.opprettVarselbrev(saksnr, behandlingId, any())
            varselbrevService.ferdigstillOgGenererPDF(brev.id, any(), any())
            brevhaandterer.journalfoerOgDistribuer(Brevkoder.BP_VARSEL, saksnr, brev.id, any())
        }
    }

    @Test
    fun `plukker ikke opp sak med opphav i Gjenny`() {
        val melding = opprettMelding(Vedtaksloesning.GJENNY)
        opprettBrevRapid.apply { sendTestMessage(melding.toJson()) }
        coVerify(exactly = 0) { varselbrevService.opprettVarselbrev(any(), any(), any()) }
    }

    private fun opprettBrev() =
        Brev(
            brevId,
            saksnr,
            behandlingId,
            "tittel",
            Spraak.NB,
            BrevProsessType.AUTOMATISK,
            "fnr",
            Status.FERDIGSTILT,
            Tidspunkt.now(),
            Tidspunkt.now(),
            mottaker = mockk(),
            brevtype = Brevtype.VARSEL,
        )

    private fun opprettMelding(kilde: Vedtaksloesning) =
        JsonMessage.newMessage(
            mapOf<String, Any>(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                Migreringshendelser.BEREGNET_FERDIG.lagParMedEventNameKey(),
                KILDE_KEY to kilde,
                SAK_ID_KEY to saksnr,
                BEHANDLING_ID_KEY to behandlingId,
                OPPGAVE_KEY to UUID.randomUUID(),
            ),
        )
}
