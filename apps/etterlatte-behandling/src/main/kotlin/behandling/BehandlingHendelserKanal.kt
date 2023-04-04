package no.nav.etterlatte.behandling

import kotlinx.coroutines.channels.SendChannel
import java.util.*

data class BehandlingHendelserKanal(val kanal: SendChannel<Pair<UUID, BehandlingHendelseType>>) {
    suspend fun send(element: Pair<UUID, BehandlingHendelseType>) = kanal.send(element)
    fun close(cause: Throwable? = null): Boolean = kanal.close()
}