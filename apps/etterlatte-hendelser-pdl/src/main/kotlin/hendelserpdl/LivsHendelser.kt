package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.JsonMessage
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

interface ILivsHendelser {
    fun personErDod(fnr: String, doedsdato: String?, endringstype: Endringstype)
    fun personUtflyttingFraNorge(
        fnr: String,
        tilflyttingsLand: String?,
        tilflyttingsstedIUtlandet: String?,
        utflyttingsdato: String?,
        endringstype: Endringstype
    )
}

class LivsHendelser(config: AppConfig) : ILivsHendelser {

    val producer = KafkaProducer<String, String>(config.producerConfig())
    val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        Runtime.getRuntime().addShutdownHook(Thread { producer.close() })
    }

    override fun personErDod(fnr: String, doedsdato: String?, endringstype: Endringstype) {
        logger.info("Poster at person $fnr er doed")
        val avdoedDoedsdato = doedsdato.parseDato(fnr, logger)
        val doedshendelse = Doedshendelse(
            avdoedFnr = fnr,
            doedsdato = avdoedDoedsdato,
            endringstype = endringstype
        )
        producer.send(
            ProducerRecord(
                topic,
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

    override fun personUtflyttingFraNorge(
        fnr: String,
        tilflyttingsLand: String?,
        tilflyttingsstedIUtlandet: String?,
        utflyttingsdato: String?,
        endringstype: Endringstype
    ) {
        logger.info("Poster at person $fnr har flyttet til utlandet")
        val utflyttingsdato = utflyttingsdato.parseDato(fnr, logger)
        val utflyttingsHendelse = UtflyttingsHendelse(
            fnr = fnr,
            tilflyttingsLand = tilflyttingsLand,
            tilflyttingsstedIUtlandet = tilflyttingsstedIUtlandet,
            utflyttingsdato = utflyttingsdato,
            endringstype = endringstype

        )
        producer.send(
            ProducerRecord(
                topic,
                UUID.randomUUID().toString(),
                JsonMessage("{}", MessageProblems("{}"))
                    .apply {
                        set(eventNameKey, "PDL:PERSONHENDELSE")
                        set("hendelse", "UTFLYTTING_FRA_NORGE")
                        set("hendelse_data", utflyttingsHendelse)
                    }.toJson()

            )
        )
    }

    companion object {
        const val topic = "etterlatte.dodsmelding"
    }
}

class LivsHendelserRapid(private val context: RapidsConnection) : ILivsHendelser {
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun personErDod(fnr: String, doedsdato: String?, endringstype: Endringstype) {
        logger.info("Poster at person $fnr er doed")
        val avdoedDoedsdato = doedsdato.parseDato(fnr, logger)
        val doedshendelse = Doedshendelse(avdoedFnr = fnr, doedsdato = avdoedDoedsdato, endringstype = endringstype)
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

    override fun personUtflyttingFraNorge(
        fnr: String,
        tilflyttingsLand: String?,
        tilflyttingsstedIUtlandet: String?,
        utflyttingsdato: String?,
        endringstype: Endringstype
    ) {
        logger.info("Poster at person $fnr har flyttet til utlandet")
        val utflyttingsdato = utflyttingsdato.parseDato(fnr, logger)
        val utflyttingsHendelse = UtflyttingsHendelse(
            fnr = fnr,
            tilflyttingsLand = tilflyttingsLand,
            tilflyttingsstedIUtlandet = tilflyttingsstedIUtlandet,
            utflyttingsdato = utflyttingsdato,
            endringstype = endringstype

        )
        context.publish(
            UUID.randomUUID().toString(),
            JsonMessage("{}", MessageProblems("{}"))
                .apply {
                    set(eventNameKey, "PDL:PERSONHENDELSE")
                    set("hendelse", "UTFLYTTING_FRA_NORGE_V1")
                    set("hendelse_data", utflyttingsHendelse)
                }.toJson()

        )
    }
}

fun String?.parseDato(fnr: String, logger: Logger): LocalDate? = try {
    this?.let { LocalDate.parse(it) }
} catch (e: Exception) {
    logger.warn(
        "Kunne ikke parse doedsdato for person med ident $fnr. " +
            "Verdien for doedsdato er: $this. Vi bruker null som dødsdato.",
        e
    )
    null
}