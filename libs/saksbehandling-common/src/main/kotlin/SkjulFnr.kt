package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.core.JsonStreamContext
import net.logstash.logback.mask.ValueMasker

/*
    Denne brukes i logback.xml for å anonymisere fnr som randomly havner i loggene
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" >
            <jsonGeneratorDecorator class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
                    <valueMasker class="no.nav.etterlatte.libs.common.person.FnrMasker"/>
            </jsonGeneratorDecorator>
        </>
 */
private val fnrRegex = Regex("\\b\\d{11}\\b")

class FnrMasker : ValueMasker {
    override fun mask(
        context: JsonStreamContext,
        value: Any,
    ): Any {
        if (value is CharSequence) {
            return redact(value.toString())
        }
        return value
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
