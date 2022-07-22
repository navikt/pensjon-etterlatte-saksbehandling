package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.JsonMessage
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

interface IDodsmeldinger {
    fun personErDod(ident: String, doedsdato: String?)
}

class Dodsmeldinger(config: AppConfig) : IDodsmeldinger {

    val producer = KafkaProducer<String, String>(config.producerConfig())
    val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        Runtime.getRuntime().addShutdownHook(Thread { producer.close() })

    }

    override fun personErDod(ident: String, doedsdato: String?) {
        logger.info("Poster at person $ident er død")
        producer.send(ProducerRecord("etterlatte.dodsmelding",
            UUID.randomUUID().toString(), JsonMessage("{}", MessageProblems("{}"))
            .apply {
                set(eventNameKey, "person_dod")
                set("avdod_ident", ident)
                doedsdato?.also {
                    set("avdod_doedsdato", it)
                }
            }
            .toJson()))
    }

}


class DodsmeldingerRapid(private val context: RapidsConnection) : IDodsmeldinger {
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun personErDod(ident: String, doedsdato: String?) {
        logger.info("Poster at person $ident er død")
        context.publish(UUID.randomUUID().toString(), JsonMessage("{}", MessageProblems("{}"))
            .apply {
                set(eventNameKey, "person_dod")
                set("avdod_ident", ident)
                doedsdato?.also {
                    set("avdod_doedsdato", it)
                }
            }
            .toJson())
    }

}
