package no.nav.etterlatte.rivers

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class DistribuerBrevRiverTest {
    private val brevApiKlient = mockk<BrevapiKlient>()

    private val inspector = TestRapid().apply { DistribuerBrevRiver(this, brevApiKlient) }

    private val brevId = 100L
    private val journalpostId = "11111"
    private val adresse =
        Adresse(
            adresseType = "Fornavn Etternavn",
            adresselinje1 = "testveien 13",
            postnummer = "0123",
            poststed = "Oslo",
            land = "Norge",
            landkode = "NOR",
        )

    @BeforeEach
    fun before() = clearAllMocks()

    @AfterEach
    fun after() = confirmVerified(brevApiKlient)

    @Test
    fun `Gyldig melding skal sende journalpost til distribusjon`() {
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()
        val test =
            mockk<VedtakInnholdDto.VedtakBehandlingDto> {
                every { behandling.id } returns behandlingId
                every { virkningstidspunkt } returns YearMonth.now()
                every { behandling.revurderingsaarsak } returns null
                every { behandling.revurderingInfo } returns null
                every { behandling.type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { utbetalingsperioder } returns emptyList()
                every { opphoerFraOgMed } returns mockk()
            }
        val vedtakdto =
            VedtakDto(
                2L,
                behandlingId,
                VedtakStatus.ATTESTERT,
                VedtakSak("ident", SakType.BARNEPENSJON, sakId),
                VedtakType.INNVILGELSE,
                null,
                null,
                test,
            )
        val melding =
            JsonMessage.newMessage(
                mapOf(
                    BrevHendelseType.JOURNALFOERT.lagParMedEventNameKey(),
                    CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                    BREV_ID_KEY to brevId,
                    "journalpostId" to journalpostId,
                    "distribusjonType" to DistribusjonsType.VEDTAK.toString(),
                    "mottakerAdresse" to adresse,
                    "vedtak" to vedtakdto,
                ),
            )

        coEvery { brevApiKlient.distribuer(brevId, any(), sakId, journalpostId) } returns BestillingsIdDto("bestilt")

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        coVerify(exactly = 1) {
            brevApiKlient.distribuer(brevId, DistribusjonsType.VEDTAK, journalpostIdInn = journalpostId, sakId = sakId)
        }
    }
}
