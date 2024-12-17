package no.nav.etterlatte.libs.common.person

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/*
    Denne brukes i logback.xml for å anonymisere fnr som randomly havner i loggene
 */
private val fnrRegex = Regex("\\b\\d{11}\\b")

class FnrCoverConverter : MessageConverter() {
    override fun convert(event: ILoggingEvent): String {
        val message = event.formattedMessage
        if (message != null) {
            val tmpmsg = super.convert(event)
            val allWords = tmpmsg.split(" ")
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

fun main() {
    println(Folkeregisteridentifikator.isValid("26478321391"))
}
