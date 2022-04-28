package no.nav.etterlatte

import io.ktor.client.HttpClient
import model.FattetVedtak
import org.slf4j.LoggerFactory

class BrevService(private val klient: HttpClient, private val url: String) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun opprettBrev(vedtak: FattetVedtak): String = "Hei jeg er et ${vedtak.type}'s-brev!".also {
        logger.info("Brev opprettet!")
    }
}
