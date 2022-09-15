package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.ManueltOpphoer
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelse
import java.util.*

interface RevurderingService {
    fun hentRevurdering(behandling: UUID): Revurdering?
    fun hentRevurderinger(): List<Revurdering>
    fun startRevurdering(
        forrigeBehandling: Behandling,
        pdlHendelse: PdlHendelse,
        revurderingAarsak: RevurderingAarsak
    ): Revurdering

    fun slettRevurderingISak(sakId: Long)
}

class RealRevurderingService(
    private val behandlinger: BehandlingDao,
    private val revurderingFactory: RevurderingFactory,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>
) : RevurderingService {

    override fun slettRevurderingISak(sakId: Long) {
        return inTransaction {
            behandlinger.slettRevurderingerISak(sakId)
        }
    }

    override fun hentRevurdering(behandling: UUID): Revurdering {
        return inTransaction {
            revurderingFactory.hentRevurdering(behandling).serialiserbarUtgave()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun hentRevurderinger(): List<Revurdering> {
        return inTransaction {
            behandlinger.alleBehandlingerAvType(BehandlingType.REVURDERING) as List<Revurdering>
        }
    }

    override fun startRevurdering(
        forrigeBehandling: Behandling,
        pdlHendelse: PdlHendelse,
        revurderingAarsak: RevurderingAarsak
    ): Revurdering {
        return inTransaction {
            when (forrigeBehandling) {
                is Foerstegangsbehandling -> revurderingFactory.opprettRevurdering(
                    forrigeBehandling.sak,
                    forrigeBehandling.persongalleri,
                    revurderingAarsak
                )
                is Revurdering -> revurderingFactory.opprettRevurdering(
                    forrigeBehandling.sak,
                    forrigeBehandling.persongalleri,
                    revurderingAarsak
                )
                is ManueltOpphoer -> revurderingFactory.opprettRevurdering(
                    forrigeBehandling.sak,
                    forrigeBehandling.persongalleri,
                    revurderingAarsak
                )
                else -> throw Exception(
                    "Forrige behandling er av en type som gjoer at revurdering ikke kan" +
                        "opprettes: ${forrigeBehandling.javaClass.kotlin}"
                )
            }
        }
            .also {
                runBlocking {
                    behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
                }
            }.serialiserbarUtgave()
    }
}