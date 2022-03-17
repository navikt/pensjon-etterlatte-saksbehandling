package no.nav.etterlatte.vedtaksoversetter


import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

data class Vedtak(
    val sakId: Long,
    val saksbehandlerId: Long,
    val beregningsperioder: List<Beregningsperiode>,
    val aktor: String,
    val aktorFoedselsdato: LocalDate,
)

data class Beregningsperiode(
    val behandlingsId: Long
)


internal class Vedtaksoversetter(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(Vedtaksoversetter::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("@vedtak") }
            validate { it.rejectKey("@vedtak_oversatt") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                // TODO finne relevante felter i vedtak
                val vedtak: Vedtak = deserialize(packet["@vedtak"].textValue())

                // TODO finne ut hvordan oppdrag skal bygges opp
                val oppdrag: Oppdrag = oppdragFraVedtak(vedtak)

                // TODO send oppdrag til MQ-tjeneste - krever tilgang til tjeneste som ligger onprem
                sendOppdrag(oppdrag)

                logger.info("")
                context.publish(packet.apply { this["@vedtak_oversatt"] = true }.toJson())
            } catch (e: Exception) {
                logger.error("Uhåndtert feilsituasjon: ${e.message}", e)
            }
        }

    private fun sendOppdrag(oppdrag: Oppdrag) {

    }

    private fun oppdragFraVedtak(vedtak: Vedtak): Oppdrag {
        val oppdrag110 = Oppdrag110().apply {
            kodeAksjon = "1" // 3 = simulering
            kodeEndring = "NY" // Alltid NY for førstegangsinnvilgelse
            kodeFagomraade = "EY" // TODO må legges inn hos økonomi
            fagsystemId = vedtak.sakId.toString()
            utbetFrekvens = "MND"
            oppdragGjelderId = vedtak.aktor
            datoOppdragGjelderFom = vedtak.aktorFoedselsdato.toXMLDate()
            saksbehId = vedtak.saksbehandlerId.toString()

            oppdragsEnhet120.add(
                OppdragsEnhet120().apply {
                    enhet = ""
                    typeEnhet = ""
                    datoEnhetFom = LocalDate.now().toXMLDate()
                }
            )

            oppdragsLinje150.addAll(
                vedtak.beregningsperioder.map {
                    OppdragsLinje150().apply {
                        henvisning = it.behandlingsId.toString()
                    }
                }
            )
        }

        return Oppdrag().apply {
            this.oppdrag110 = oppdrag110
        }
    }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

    fun LocalDate.toXMLDate(): XMLGregorianCalendar {
        return DatatypeFactory.newInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(atStartOfDay(ZoneId.systemDefault())))
    }

}
