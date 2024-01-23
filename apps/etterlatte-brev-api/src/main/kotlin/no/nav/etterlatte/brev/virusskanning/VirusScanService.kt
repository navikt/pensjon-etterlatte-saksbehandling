package no.nav.etterlatte.brev.virusskanning

import net.logstash.logback.argument.StructuredArguments
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Base64

class VirusScanService(
    private val clamAvClient: ClamAvClient,
) {
    private val sikkerlogg = sikkerlogger()

    suspend fun vedleggContainsVirus(
        vedlegg: List<Vedlegg>,
        loggingMeta: LoggingMeta,
    ): Boolean {
        val vedleggOver300MegaByte =
            vedlegg.filter {
                fileSizeLagerThan300MegaBytes(
                    Base64.getMimeDecoder().decode(it.content.content),
                    loggingMeta,
                )
            }

        if (vedleggOver300MegaByte.isNotEmpty()) {
            logVedleggOver300MegaByteMetric(vedleggOver300MegaByte, loggingMeta)
        }

        val vedleggUnder300MegaByte =
            vedlegg.filter {
                !fileSizeLagerThan300MegaBytes(
                    Base64.getMimeDecoder().decode(it.content.content),
                    loggingMeta,
                )
            }

        return if (vedleggUnder300MegaByte.isEmpty()) {
            false
        } else {
            log.info(
                "Scanning vedlegg for virus, numbers of vedlegg: ${vedleggUnder300MegaByte.size}" +
                    ", {}",
                StructuredArguments.fields(loggingMeta),
            )
            sikkerlogg.info(
                "Scanning vedlegg for virus: vedlegg: ${objectMapper.writeValueAsString(vedleggUnder300MegaByte)} " +
                    ", {}",
                StructuredArguments.fields(loggingMeta),
            )

            val scanResultMayContainVirus =
                clamAvClient.virusScanVedlegg(vedleggUnder300MegaByte).filter {
                    it.Result != Status.OK
                }
            scanResultMayContainVirus.map {
                log.warn(
                    "Vedlegg may contain virus, filename: ${it.Filename}, {}",
                    StructuredArguments.fields(loggingMeta),
                )
            }
            scanResultMayContainVirus.isNotEmpty()
        }
    }

    private fun logVedleggOver300MegaByteMetric(
        vedlegg: List<Vedlegg>,
        loggingMeta: LoggingMeta,
    ) {
        vedlegg.forEach {
            log.info(
                "Vedlegg is over 300 megabyte: ${it.description}, {}",
                StructuredArguments.fields(loggingMeta),
            )
        }
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

data class LoggingMeta(val id: String)
