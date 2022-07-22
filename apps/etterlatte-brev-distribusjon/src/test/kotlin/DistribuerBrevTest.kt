import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.DistribuerBrev
import no.nav.etterlatte.distribusjon.DistribusjonServiceMock
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.*

class DistribuerBrevTest {
    private val inspector = TestRapid().apply { DistribuerBrev(this, DistribusjonServiceMock()) }

    @Test
    fun `Skal distribuere brevet og svare bekreftende`() {
        val brevId = 100L
        val journalpostResponse = JournalpostResponse("11111", journalpostferdigstilt = true)
        val distribusjonMelding = DistribusjonMelding(
            behandlingId = UUID.randomUUID().toString(),
            distribusjonType = DistribusjonsType.VEDTAK,
            brevId = 1000L,
            mottaker = Mottaker(foedselsnummer = Foedselsnummer.of("03108718357")),
            bruker = Bruker(id = "03108718357"),
            tittel = "Vi har innvilget din søknad om barnepensjon",
            brevKode = "XX.YY-ZZ",
            journalfoerendeEnhet = "1234"
        )
        val melding = JsonMessage.newMessage(
            mapOf(
                eventNameKey to BrevEventTypes.JOURNALFOERT.toString(),
                "brevId" to brevId,
                correlationIdKey to UUID.randomUUID().toString(),
                "journalpostResponse" to journalpostResponse.toJson(),
                "payload" to distribusjonMelding.toJson()
            ))
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        inspector.message(0).get(eventNameKey).asText() shouldBe BrevEventTypes.DISTRIBUERT.toString()
        inspector.message(0).get("brevId").asLong() shouldBe brevId
        inspector.message(0).get("bestillingId").asText() shouldNotBe null
    }
}
