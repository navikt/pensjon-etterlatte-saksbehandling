package no.nav.etterlatte.grunnlag

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.*
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

enum class GrunnlagHendelserType {
    OPPRETTET, GRUNNLAGENDRET, AVBRUTT
}

class GrunnlagHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlag: GrunnlagFactory,
    private val datasource: DataSource
) : River.PacketListener {


    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ny_opplysning") }
            //TODO mer her
        }.register(this)
    }

    override fun onPacket(packet: no.nav.helse.rapids_rivers.JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            //TODO do something med meldinger
        }
/*
    fun start() {
        GlobalScope.launch {
            withContext(
                Dispatchers.Default + Kontekst.asContextElement(
                    value = Context(Self("hendelsespubliserer"), DatabaseContext(datasource))
                )
            ) {
                for (hendelse in kanal) {
                    rapid.publiser(hendelse.first.toString(),
                        JsonMessage(objectMapper.writeValueAsString(inTransaction { grunnlag.hent(hendelse.first) }.serialiserbarUtgave())).also {
                            it["@event"] = "BEHANDLING:${hendelse.second.name}"
                        }.toJson()
                    ).also {
                        logger.info("Posted event ${hendelse.second.name} for behandling ${hendelse.first} to partiton ${it.first}, offset ${it.second}")
                    }
                }
            }
            Kontekst.remove()
        }.invokeOnCompletion {
            rapid.close()
            if (it == null || it is CancellationException) {
                logger.info("BehandlingsHendelser finished")
            } else {
                logger.error("BehandlingsHendelser ended exeptionally", it)
            }
        }
    }
}

 */

private fun no.nav.helse.rapids_rivers.JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}

