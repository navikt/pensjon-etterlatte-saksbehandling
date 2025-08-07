package no.nav.etterlatte.rapidsandrivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.common.rapidsandrivers.feilmelding
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

val feilhaandteringLogger = LoggerFactory.getLogger("feilhaandtering-kafka")
val sikkerLogg: Logger = sikkerlogger()

internal fun withRetryOgFeilhaandtering(
    packet: JsonMessage,
    kontekst: Kontekst,
    context: MessageContext,
    feilendeSteg: String,
    block: () -> Unit,
) {
    try {
        runBlocking {
            retryOgPakkUt(
                times = kontekst.retries,
                vent = { Thread.sleep(Duration.ofSeconds(1L)) },
            ) {
                block()
            }
        }
    } catch (e: Exception) {
        val feilmelding = "Håndtering av melding ${packet.id} feila på steg $feilendeSteg."
        val feilmeldingSikkerlogg = "$feilmelding med body ${packet.toJson()}"
        when (kontekst) {
            Kontekst.OMREGNING, Kontekst.AARLIG_INNTEKTSJUSTERING -> {
                feilhaandteringLogger.warn(feilmelding, e)
                sikkerLogg.warn(feilmeldingSikkerlogg, e)
            }

            else -> {
                feilhaandteringLogger.error(feilmelding, e)
                sikkerLogg.error(feilmeldingSikkerlogg, e)
            }
        }

        publiserFeilamelding(packet, feilendeSteg, kontekst, e, context)
        feilhaandteringLogger.warn("Fikk feil, sendte ut på feilkø, returnerer nå failure-result")
        return
    }
}

private fun publiserFeilamelding(
    packet: JsonMessage,
    feilendeSteg: String,
    kontekst: Kontekst,
    e: Exception,
    context: MessageContext,
) {
    try {
        packet.setEventNameForHendelseType(EventNames.FEILA)
        packet.feilendeSteg = feilendeSteg
        packet[KONTEKST_KEY] = kontekst.name
        packet.feilmelding = e.stackTraceToString()
        context.publish(packet.toJson())
        feilhaandteringLogger.info("Publiserte feila-melding")
    } catch (e2: Exception) {
        feilhaandteringLogger.warn("Feil under feilhåndtering for ${packet.id}", e2)
    }
}
