package no.nav.etterlatte.statistikk.river

import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

private val defaultLog = LoggerFactory.getLogger("parseTekniskTid")

fun parseTekniskTid(
    packet: JsonMessage,
    logger: Logger = defaultLog,
) = parseTekniskTid(packet[TEKNISK_TID_KEY].textValue(), packet.eventName, logger)

internal fun parseTekniskTid(
    pakkeTid: String?,
    eventName: String,
    logger: Logger = defaultLog,
): LocalDateTime {
    if (pakkeTid != null) {
        try {
            return LocalDateTime.parse(pakkeTid)
        } catch (e: Exception) {
            try {
                return LocalDateTime.ofInstant(ZonedDateTime.parse(pakkeTid).toInstant(), ZoneOffset.UTC)
            } catch (e2: Exception) {
                e2.addSuppressed(e)
                logger.warn("Kunne ikke parse teknisk tid på hendelse $eventName, på grunn av feil", e2)
            }
        }
    }
    logger.warn("Ingen teknisk tid på pakken med hendelse $eventName, fallbacker til now()")
    return Tidspunkt.now().toLocalDatetimeUTC()
}
