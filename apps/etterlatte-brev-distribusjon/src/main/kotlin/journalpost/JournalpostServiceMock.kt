package no.nav.etterlatte.journalpost

import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import org.slf4j.LoggerFactory
import java.util.*

class JournalpostServiceMock : JournalpostService {
    private val logger = LoggerFactory.getLogger(JournalpostServiceMock::class.java)

    override fun journalfoer(melding: DistribusjonMelding): JournalpostResponse {
        logger.info("Oppretter journalpost for brev med id=${melding.brevId}")

        return JournalpostResponse(UUID.randomUUID().toString(), "OK", null, true, emptyList())
    }
}