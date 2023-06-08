package no.nav.etterlatte.rivers

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.BrevEventTypes
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class DistribuerBrevTest {
    private val brevService = mockk<VedtaksbrevService>()
    private val distribusjonService = mockk<DistribusjonServiceImpl>(relaxed = true)

    private val inspector = TestRapid().apply { DistribuerBrev(this, brevService, distribusjonService) }

    private val brevId = 100L
    private val journalpostId = "11111"
    private val adresse = Adresse(
        adresseType = "Fornavn Etternavn",
        adresselinje1 = "testveien 13",
        postnummer = "0123",
        poststed = "Oslo",
        land = "Norge",
        landkode = "NOR"
    )

    @BeforeEach
    fun before() = clearAllMocks()

    @AfterEach
    fun after() = confirmVerified(distribusjonService, brevService)

    @Test
    fun `Gyldig melding skal sende journalpost til distribusjon`() {
        every { brevService.hentBrev(any()) } returns mockk {
            every { id } returns brevId
            every { mottaker } returns Mottaker("navn", foedselsnummer = mockk(), adresse = adresse)
        }

        val melding = JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to BrevEventTypes.JOURNALFOERT.toString(),
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                "brevId" to brevId,
                "journalpostId" to journalpostId,
                "distribusjonType" to DistribusjonsType.VEDTAK.toString(),
                "mottakerAdresse" to adresse
            )
        )

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) {
            brevService.hentBrev(brevId)

            distribusjonService.distribuerJournalpost(
                brevId,
                journalpostId,
                DistribusjonsType.VEDTAK,
                DistribusjonsTidspunktType.KJERNETID,
                adresse
            )
        }
    }
}