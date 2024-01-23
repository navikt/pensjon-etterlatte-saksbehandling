package no.nav.etterlatte.brev.virusskanning

import net.logstash.logback.argument.StructuredArguments
import no.nav.etterlatte.brev.BrevFraOpplastningRequest
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VirusScanService(
    private val clamAvClient: ClamAvClient,
) {
    private val sikkerlogg = sikkerlogger()

    suspend fun vedleggContainsVirus(
        request: VirusScanRequest,
        loggingMeta: LoggingMeta,
    ): Boolean {
        val vedleggOver300MegaByte = fileSizeLagerThan300MegaBytes(request.fil, loggingMeta)

        if (vedleggOver300MegaByte) {
            logVedleggOver300MegaByteMetric(request.tittel(), loggingMeta)
        }

        log.info("Scanning vedlegg for virus, {}", StructuredArguments.fields(loggingMeta))
        sikkerlogg.info(
            "Scanning vedlegg for virus: vedlegg: ${objectMapper.writeValueAsString(request)}, {}",
            StructuredArguments.fields(loggingMeta),
        )

        val scanResultMayContainVirus = clamAvClient.virusScanVedlegg(request).filter { it.Result != Status.OK }
        scanResultMayContainVirus.forEach {
            log.warn(
                "Vedlegg may contain virus, filename: ${it.Filename}, {}",
                StructuredArguments.fields(loggingMeta),
            )
        }
        return scanResultMayContainVirus.isNotEmpty()
    }

    private fun logVedleggOver300MegaByteMetric(
        tittel: String,
        loggingMeta: LoggingMeta,
    ) {
        log.info("Vedlegg is over 300 megabyte: $tittel, {}", StructuredArguments.fields(loggingMeta))
    }
}

val log: Logger = LoggerFactory.getLogger(VirusScanService::class.java)

fun fileSizeLagerThan300MegaBytes(
    file: ByteArray,
    loggingMeta: LoggingMeta,
): Boolean {
    val filesizeMegaBytes = (file.size / 1024) / 1024
    log.info("File size MB is: $filesizeMegaBytes, {}", StructuredArguments.fields(loggingMeta))
    return (file.size / 1024) / 1024 > 300
}

data class LoggingMeta(val request: BrevFraOpplastningRequest)

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
