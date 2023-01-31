package no.nav.etterlatte

import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DistribuerBrevTest {
    private val distribusjonService = mockk<DistribusjonServiceImpl>(relaxed = true)
    private val brevService = mockk<BrevService>()

    private val inspector = TestRapid().apply { DistribuerBrev(this, distribusjonService, brevService) }

    private val brevId = 100L
    private val journalpostId = "11111"
    private val adresse = Adresse("Fornavn", "Etternavn", "testveien 13", "0123", "Oslo", "Norge")

    @BeforeEach
    fun before() = clearMocks(distribusjonService)

    @AfterEach
    fun after() = confirmVerified(distribusjonService)

    @Test
    fun `Gyldig melding skal sende journalpost til distribusjon`() {
        val melding = JsonMessage.newMessage(
            mapOf(
                eventNameKey to BrevEventTypes.JOURNALFOERT.toString(),
                correlationIdKey to UUID.randomUUID().toString(),
                "brevId" to brevId,
                "journalpostId" to journalpostId,
                "distribusjonType" to DistribusjonsType.VEDTAK.toString(),
                "mottakerAdresse" to adresse.toJson()
            )
        )

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) {
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