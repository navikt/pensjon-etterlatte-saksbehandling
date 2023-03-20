package no.nav.etterlatte.behandling

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.event.BehandlingRiverKey
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

enum class BehandlingHendelseType {
    OPPRETTET, AVBRUTT
}

class BehandlingsHendelser(
    private val rapid: KafkaProdusent<String, String>,
    private val behandlingDao: BehandlingDao,
    private val datasource: DataSource
) {
    private val kanal: Channel<Pair<UUID, BehandlingHendelseType>> = Channel(Channel.UNLIMITED)
    val nyHendelse: SendChannel<Pair<UUID, BehandlingHendelseType>> get() = kanal

    private val logger: Logger = LoggerFactory.getLogger(BehandlingsHendelser::class.java)

    fun start() {
        GlobalScope.launch {
            withContext(
                Dispatchers.Default + Kontekst.asContextElement(
                    value = Context(Self("hendelsespubliserer"), DatabaseContext(datasource))
                )
            ) {
                for (hendelse in kanal) {
                    try {
                        handleEnHendelse(hendelse)
                    } catch (ex: Exception) {
                        logger.warn("Handtering av behandlingshendelse feilet", ex)
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

    private fun handleEnHendelse(hendelse: Pair<UUID, BehandlingHendelseType>) {
        inTransaction {
            val behandling = requireNotNull(behandlingDao.hentBehandling(hendelse.first))
            val correlationId = getCorrelationId()
            rapid.publiser(
                hendelse.first.toString(),
                JsonMessage.newMessage(
                    "BEHANDLING:${hendelse.second.name}",
                    mapOf(
                        CORRELATION_ID_KEY to correlationId,
                        BehandlingRiverKey.behandlingObjectKey to behandling,
                        BehandlingRiverKey.sakIdKey to behandling.sak.id,
                        BehandlingRiverKey.persongalleriKey to behandling.persongalleri
                    )
                ).toJson()
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event BEHANDLING:${hendelse.second.name} for behandling ${hendelse.first}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId"
                )
            }
        }
    }
}