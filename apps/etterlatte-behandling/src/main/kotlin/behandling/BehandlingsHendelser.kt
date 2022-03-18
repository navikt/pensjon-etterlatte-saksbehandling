package no.nav.etterlatte.behandling

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.*
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import java.util.*
import javax.sql.DataSource

enum class BehandlingHendelseType{
    OPPRETTET, GRUNNLAGENDRET, AVBRUTT
}

class BehandlingsHendelser(
    private val rapid: KafkaProdusent<String, String>,
    private val behandlinger: BehandlingFactory,
    private val datasource: DataSource
) {
    private val kanal: Channel<Pair<UUID, BehandlingHendelseType>> = Channel(Channel.UNLIMITED)
    val nyHendelse: SendChannel<Pair<UUID, BehandlingHendelseType>> get() = kanal

    fun start(){
        GlobalScope.launch {
            withContext(Dispatchers.Default + Kontekst.asContextElement(value = Context(Self("hendelsespubliserer"), DatabaseContext(datasource))
                )
            ) {
                for(hendelse in kanal){
                    rapid.publiser(hendelse.first.toString(),
                        JsonMessage(objectMapper.writeValueAsString(inTransaction { behandlinger.hent(hendelse.first)}.serialiserbarUtgave())).also {
                            it["@event"] = "BEHANDLING:${hendelse.second.name}"
                        }.toJson()
                    )
                }
            }
            Kontekst.remove()
        }
    }
}