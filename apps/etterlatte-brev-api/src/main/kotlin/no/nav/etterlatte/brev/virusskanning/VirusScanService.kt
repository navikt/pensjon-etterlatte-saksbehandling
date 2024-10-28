package no.nav.etterlatte.brev.virusskanning

import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val MAKS_FILSTOERRELSE_BREV = 2 * 1024 * 1024

private val log: Logger = LoggerFactory.getLogger(VirusScanService::class.java)

class VirusScanService(
    private val clamAvClient: ClamAvClient,
) {
    suspend fun filHarVirus(request: VirusScanRequest): Boolean {
        log.info("Skanner fil for virus: ${request.tittel}")
        if (filErForStor(request.fil)) {
            log.info("Fil er for stor: ${request.tittel}")
            return true
        }

        return clamAvClient
            .skann(ClamAVRequest(filnavn = request.tittel, fil = request.fil))
            .onEach { log.warn("Status for virussjekk for ${it.Filename}: ${it.Result} ") }
            .any { it.Result != Status.OK }
    }
}

fun filErForStor(file: ByteArray) = file.size > (MAKS_FILSTOERRELSE_BREV).also { log.info("Fila er ${file.size} bytes") }

data class VirusScanRequest(
    val tittel: String,
    val fil: ByteArray,
)
