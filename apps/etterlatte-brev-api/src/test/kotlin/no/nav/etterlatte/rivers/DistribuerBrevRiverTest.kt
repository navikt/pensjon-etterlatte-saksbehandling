package no.nav.etterlatte.rivers

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DistribuerBrevRiverTest {
    private val brevService = mockk<VedtaksbrevService>()
    private val brevdistribuerer = mockk<Brevdistribuerer>(relaxed = true)

    private val inspector = TestRapid().apply { DistribuerBrevRiver(this, brevdistribuerer) }

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
    fun after() = confirmVerified(brevdistribuerer, brevService)

    @Test
    fun `Gyldig melding skal sende journalpost til distribusjon`() {
        every { brevService.hentBrev(any()) } returns
            mockk {
                every { id } returns brevId
                every { mottaker } returns Mottaker("navn", foedselsnummer = mockk(), adresse = adresse)
            }

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    BrevHendelseType.JOURNALFOERT.lagParMedEventNameKey(),
                    CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                    BREV_ID_KEY to brevId,
                    "journalpostId" to journalpostId,
                    "distribusjonType" to DistribusjonsType.VEDTAK.toString(),
                    "mottakerAdresse" to adresse,
                ),
            )

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) {
            brevdistribuerer.distribuer(brevId, DistribusjonsType.VEDTAK, journalpostId)
        }
    }
}
