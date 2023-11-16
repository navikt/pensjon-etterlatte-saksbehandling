package no.nav.etterlatte.statistikk.river

import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val defaultLog = LoggerFactory.getLogger("parseTekniskTid")

fun parseTekniskTid(
    packet: JsonMessage,
    logger: Logger = defaultLog,
): LocalDateTime {
    val pakkeTid = packet[TEKNISK_TID_KEY].textValue()
    if (pakkeTid != null) {
        try {
            return LocalDateTime.parse(pakkeTid)
        } catch (e: Exception) {
            logger.warn("Kunne ikke parse teknisk tid på hendelse ${packet.eventName}, på grunn av feil", e)
        }
    }
    logger.warn("Ingen teknisk tid på pakken med hendelse ${packet.eventName}, fallbacker til now()")
    return Tidspunkt.now().toLocalDatetimeUTC()
}
