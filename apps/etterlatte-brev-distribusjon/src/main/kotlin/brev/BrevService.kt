package no.nav.etterlatte.brev

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory

interface BrevService {
    suspend fun hentBrevInnhold(id: Long): ByteArray
}

class BrevServiceImpl(private val client: HttpClient, private val url: String) : BrevService {
    private val logger = LoggerFactory.getLogger(BrevServiceImpl::class.java)

    override suspend fun hentBrevInnhold(id: Long): ByteArray {
        logger.info("Forsøker å hente innhold for brev med id = $id")
        logger.info("Kaller brev api på: $url")

        try {
            return client.post("$url/$id/pdf") {
                header(X_CORRELATION_ID, getXCorrelationId())
            }.body()
        } catch (ex: Exception) {
            logger.error("Klarte ikke hente brevinnhold for brev med id = $id", ex)
            throw HentBrevInnholdException("Klarte ikke hente brevinnhold", ex)
        }
    }
}

class HentBrevInnholdException(msg: String, cause: Throwable) : Exception(msg, cause)