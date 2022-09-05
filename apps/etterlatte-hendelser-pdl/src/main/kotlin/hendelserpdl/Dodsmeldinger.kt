package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.JsonMessage
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

interface IDodsmeldinger {
    fun personErDod(ident: String, doedsdato: String?, endringstype: Endringstype)
}

class Dodsmeldinger(config: AppConfig) : IDodsmeldinger {

    val producer = KafkaProducer<String, String>(config.producerConfig())
    val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        Runtime.getRuntime().addShutdownHook(Thread { producer.close() })
    }

    override fun personErDod(ident: String, doedsdato: String?, endringstype: Endringstype) {
        logger.info("Poster at person $ident er doed")
        val avdoedDoedsdato: LocalDate? = try {
            doedsdato?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke parse doedsdato for person med ident $ident. " +
                    "Verdien for doedsdato er: $doedsdato. Vi bruker null som dødsdato.",
                e
            )
            null
        }
        val doedshendelse = Doedshendelse(
            avdoedFnr = ident,
            doedsdato = avdoedDoedsdato,
            endringstype = endringstype
        )
        producer.send(
            ProducerRecord(
                "etterlatte.dodsmelding",
                UUID.randomUUID().toString(),
                JsonMessage("{}", MessageProblems("{}"))
                    .apply {
                        set(eventNameKey, "PDL:PERSONHENDELSE")
                        set("hendelse", "DOEDSFALL_V1")
                        set(
                            "hendelse_data",
                            doedshendelse
                        )
                    }
                    .toJson()
            )
        )
    }
}

class DodsmeldingerRapid(private val context: RapidsConnection) : IDodsmeldinger {
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun personErDod(ident: String, doedsdato: String?, endringstype: Endringstype) {
        logger.info("Poster at person $ident er doed")
        val avdoedDoedsdato: LocalDate? = try {
            doedsdato?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke parse doedsdato for person med ident $ident. " +
                    "Verdien for doedsdato er: $doedsdato. Vi bruker null som dødsdato.",
                e
            )
            null
        }
        val doedshendelse = Doedshendelse(avdoedFnr = ident, doedsdato = avdoedDoedsdato, endringstype = endringstype)
        context.publish(
            UUID.randomUUID().toString(),
            JsonMessage("{}", MessageProblems("{}"))
                .apply {
                    set(eventNameKey, "PDL:PERSONHENDELSE")
                    set("hendelse", "DOEDSFALL_V1")
                    set(
                        "hendelse_data",
                        doedshendelse
                    )
                }
                .toJson()
        )
    }
}