package no.nav.etterlatte.brev.virusskanning

import no.nav.etterlatte.brev.BrevFraOpplastningRequest
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VirusScanService(private val clamAvClient: ClamAvClient) {
    private val sikkerlogg = sikkerlogger()

    suspend fun vedleggContainsVirus(request: VirusScanRequest): Boolean {
        val vedleggOver300MegaByte = fileSizeLagerThan300MegaBytes(request.fil)

        if (vedleggOver300MegaByte) {
            log.info("Vedlegg is over 300 megabyte: ${request.tittel()}")
        }

        log.info("Scanning vedlegg for virus: ${request.tittel()}")
        sikkerlogg.info("Scanning vedlegg for virus: $request")

        return clamAvClient.virusScanVedlegg(request)
            .onEach { log.warn("Status for virussjekk for ${it.Filename}: ${it.Result} ") }
            .any { it.Result != Status.OK }
    }
}

val log: Logger = LoggerFactory.getLogger(VirusScanService::class.java)

fun fileSizeLagerThan300MegaBytes(file: ByteArray): Boolean {
    val filesizeMegaBytes = (file.size / 1024) / 1024
    log.info("File size MB is: $filesizeMegaBytes, file size is ${file.size}")
    return filesizeMegaBytes > 300
}

data class VirusScanRequest(val meta: BrevFraOpplastningRequest, val fil: ByteArray) {
    fun tittel() = meta.innhold.tittel

    fun filnavn() = "${meta.sak.id}-${meta.innhold.tittel}-${Tidspunkt.now()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirusScanRequest

        if (meta != other.meta) return false
        return fil.contentEquals(other.fil)
    }

    override fun hashCode(): Int {
        var result = meta.hashCode()
        result = 31 * result + fil.contentHashCode()
        return result
    }
}
