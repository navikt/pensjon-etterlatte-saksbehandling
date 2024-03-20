package no.nav.etterlatte.libs.common.person

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

private val fnrRegex = Regex("\\b\\d{11}\\b")

class FnrCoverConverter : MessageConverter() {
    override fun convert(event: ILoggingEvent): String {
        val message = event.formattedMessage
        if (message != null) {
            val allWords = message.split(" ")
            return allWords.map { redact(it) }.joinToString(" ")
        } else {
            return event.formattedMessage
        }
    }
}

fun redact(string: String): String =
    string.replace(fnrRegex) {
        if (Folkeregisteridentifikator.isValid(it.value)) {
            it.value.maskerFnr()
        } else {
            it.value
        }
    }
