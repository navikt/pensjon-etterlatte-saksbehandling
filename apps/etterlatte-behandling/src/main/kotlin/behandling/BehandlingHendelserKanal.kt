package no.nav.etterlatte.behandling

import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import java.util.*

data class BehandlingHendelserKanal(val kanal: SendChannel<Triple<UUID, BehandlingHendelseType, BehandlingType>>) {
    suspend fun send(element: Triple<UUID, BehandlingHendelseType, BehandlingType>) = kanal.send(element)
    fun close(cause: Throwable? = null): Boolean = kanal.close(cause)
}