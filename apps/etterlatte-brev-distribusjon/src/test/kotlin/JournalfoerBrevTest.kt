package no.nav.etterlatte.journalpost

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.JournalfoerBrev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.*

class JournalfoerBrevTest {
    private val inspector = TestRapid().apply { JournalfoerBrev(this, JournalpostServiceMock()) }

    @Test
    fun `Skal journalfoere brevet og svare bekreftende`() {
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
                eventNameKey to BrevEventTypes.FERDIGSTILT.toString(),
                "brevId" to distribusjonMelding.brevId,
                correlationIdKey to UUID.randomUUID().toString(),
                "payload" to distribusjonMelding.toJson()
            )
        )
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        inspector.message(0).get(eventNameKey).asText() shouldBe BrevEventTypes.JOURNALFOERT.toString()
        inspector.message(0).get("brevId").asLong() shouldBe distribusjonMelding.brevId
        objectMapper.readValue<JournalpostResponse>(inspector.message(0).get("journalpostResponse").asText()).let {
            it.journalpostId shouldNotBe null
            it.journalpoststatus shouldBe "OK"
            it.journalpostferdigstilt shouldBe true
        }
    }
}