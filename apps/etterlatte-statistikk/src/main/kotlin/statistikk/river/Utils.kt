package no.nav.etterlatte.statistikk.river

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.tekniskTidKey
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val defaultLog = LoggerFactory.getLogger("parseTekniskTid")
fun parseTekniskTid(packet: JsonMessage, logger: Logger = defaultLog): LocalDateTime {
    val pakkeTid = packet[tekniskTidKey].textValue()
    if (pakkeTid != null) {
        return LocalDateTime.parse(pakkeTid)
    }
    logger.warn("Ingen teknisk tid på pakken med hendelse ${packet.eventName}, fallbacker til now()")
    return LocalDateTime.now()
}