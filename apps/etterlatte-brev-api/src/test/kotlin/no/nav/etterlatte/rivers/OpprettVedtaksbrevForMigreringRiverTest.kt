package no.nav.etterlatte.rivers

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.etterlatte.rivers.migrering.OpprettVedtaksbrevForMigreringRiver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rapidsandrivers.HENDELSE_DATA_KEY
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class OpprettVedtaksbrevForMigreringRiverTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val opprettBrevRapid = TestRapid().apply { OpprettVedtaksbrevForMigreringRiver(this, vedtaksbrevService) }

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun before() = clearMocks(vedtaksbrevService)

    @AfterEach
    fun after() = confirmVerified(vedtaksbrevService)

    @Test
    fun `oppretter for migrering nytt brev, genererer pdf og sender videre`() {
        val vedtak = opprettVedtak()
        val migreringRequest = migreringRequest()
        val melding = opprettMelding(vedtak, migreringRequest, VedtakKafkaHendelseHendelseType.FATTET)
        val brev = opprettBrev()

        coEvery { vedtaksbrevService.opprettVedtaksbrev(any(), behandlingId, any(), any()) } returns brev
        coEvery { vedtaksbrevService.genererPdf(brev.id, any()) } returns mockk<Pdf>()
        coEvery { vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, any(), true) } just runs

        val inspektoer = opprettBrevRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        coVerify(exactly = 1) { vedtaksbrevService.opprettVedtaksbrev(any(), behandlingId, any(), any()) }
        coVerify(exactly = 1) { vedtaksbrevService.genererPdf(brev.id, any()) }
        coVerify(exactly = 1) { vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, any(), true) }

        val meldingSendt = inspektoer.message(0)
        assertEquals(VedtakKafkaHendelseHendelseType.FATTET.lagEventnameForType(), meldingSendt.get(EVENT_NAME_KEY).asText())
    }

    @Test
    fun `plukker ikke opp sak med opphav i Gjenny`() {
        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak, null)
        opprettBrev()

        val inspektoer = opprettBrevRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        assertEquals(0, inspektoer.size)
    }

    private fun opprettBrev() =
        Brev(
            1,
            41,
            behandlingId,
            "tittel",
            BrevProsessType.AUTOMATISK,
            "fnr",
            Status.FERDIGSTILT,
            Tidspunkt.now(),
            Tidspunkt.now(),
            mottaker = mockk(),
        )

    private fun opprettMelding(
        vedtak: VedtakDto,
        migreringRequest: MigreringRequest?,
        hendelse: VedtakKafkaHendelseHendelseType = VedtakKafkaHendelseHendelseType.FATTET,
    ): JsonMessage {
        val kilde =
            when (migreringRequest) {
                null -> Vedtaksloesning.GJENNY
                else -> Vedtaksloesning.PESYS
            }
        val messageKeys =
            mapOf(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                hendelse.lagParMedEventNameKey(),
                "vedtak" to vedtak,
                KILDE_KEY to kilde,
            )
        if (migreringRequest == null) {
            return JsonMessage.newMessage(messageKeys)
        }
        return JsonMessage.newMessage(messageKeys + mapOf(HENDELSE_DATA_KEY to migreringRequest))
    }

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING) =
        VedtakDto(
            id = 1L,
            status = VedtakStatus.ATTESTERT,
            behandlingId = behandlingId,
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            type = VedtakType.INNVILGELSE,
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now()),
            innhold =
                VedtakInnholdDto.VedtakBehandlingDto(
                    virkningstidspunkt = YearMonth.now(),
                    behandling = Behandling(behandlingType, behandlingId),
                    utbetalingsperioder = emptyList(),
                ),
        )
}

fun migreringRequest() =
    MigreringRequest(
        pesysId = PesysId(id = 2458),
        enhet = Enhet(nr = "congue"),
        soeker = SOEKER_FOEDSELSNUMMER,
        gjenlevendeForelder = null,
        avdoedForelder = listOf(),
        dodAvYrkesskade = false,
        foersteVirkningstidspunkt = YearMonth.of(2024, Month.JANUARY),
        beregning =
            Beregning(
                brutto = 4285,
                netto = 5734,
                anvendtTrygdetid = 5822,
                datoVirkFom = Tidspunkt.now(),
                g = 1521,
                prorataBroek = null,
                meta = null,
            ),
        trygdetid = Trygdetid(perioder = listOf()),
        flyktningStatus = false,
        spraak = Spraak.NB,
    )
