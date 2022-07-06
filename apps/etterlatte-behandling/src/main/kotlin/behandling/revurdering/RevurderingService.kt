package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import java.util.*


interface RevurderingService {
    fun hentRevurdering(behandling: UUID): Revurdering?
    fun hentRevurderinger(): List<Revurdering>
    fun startRevurdering(sak: Long, persongalleri: Persongalleri, mottattDato: String): Revurdering
}

class RealRevurderingService(
    private val behandlinger: BehandlingDao,
    private val revurderingFactory: RevurderingFactory,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>
) : RevurderingService {

    override fun hentRevurdering(behandling: UUID): Revurdering {
        return inTransaction {
            revurderingFactory.hentRevurdering(behandling).serialiserbarUtgave()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun hentRevurderinger(): List<Revurdering> {
        return inTransaction {
            behandlinger.alleBehandlinger(BehandlingType.REVURDERING) as List<Revurdering>
        }
    }

    override fun startRevurdering(sak: Long, persongalleri: Persongalleri, mottattDato: String): Revurdering {
        return inTransaction {
            revurderingFactory.opprettRevurdering(sak, persongalleri, mottattDato)
        }
            .also {
                runBlocking {
                    behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
                }
            }.serialiserbarUtgave()
    }
}
