package no.nav.etterlatte.behandling.foerstegangsbehandling

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.ManueltOpphoer
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.YearMonth
import java.util.*

interface FoerstegangsbehandlingService {
    fun hentBehandling(behandling: UUID): Foerstegangsbehandling?
    fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling
    fun hentFoerstegangsbehandlinger(): List<Foerstegangsbehandling>
    fun startFoerstegangsbehandling(
        sak: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): Foerstegangsbehandling

    fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat)
    fun fastsettVirkningstidspunkt(behandlingId: UUID, dato: YearMonth, ident: String): Virkningstidspunkt
    fun settKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode)
}

class RealFoerstegangsbehandlingService(
    private val behandlinger: BehandlingDao,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>
) : FoerstegangsbehandlingService {
    private val logger = LoggerFactory.getLogger(RealFoerstegangsbehandlingService::class.java)

    override fun hentBehandling(behandling: UUID): Foerstegangsbehandling {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling).serialiserbarUtgave()
        }
    }

    override fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling).serialiserbarUtgave()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun hentFoerstegangsbehandlinger(): List<Foerstegangsbehandling> {
        return inTransaction {
            behandlinger.alleBehandlingerAvType(BehandlingType.FØRSTEGANGSBEHANDLING) as List<Foerstegangsbehandling>
        }
    }

    override fun startFoerstegangsbehandling(
        sak: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): Foerstegangsbehandling {
        logger.info("Starter en behandling")
        return inTransaction {
            foerstegangsbehandlingFactory.opprettFoerstegangsbehandling(
                sak,
                mottattDato,
                persongalleri
            )
        }.also {
            runBlocking {
                behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
            }
        }.serialiserbarUtgave()
    }

    override fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling)
                .lagreGyldighetprøving(gyldighetsproeving)
        }
    }

    override fun fastsettVirkningstidspunkt(behandlingId: UUID, dato: YearMonth, ident: String): Virkningstidspunkt {
        val behandling = inTransaction {
            behandlinger.hentBehandling(behandlingId) ?: throw RuntimeException("Fant ikke behandling")
        }

        when (behandling) {
            is Foerstegangsbehandling -> {
                behandling.oppdaterVirkningstidspunkt(dato, Grunnlagsopplysning.Saksbehandler(ident, Instant.now()))
                val virkningstidspunkt = behandling.hentVirkningstidspunkt()!!

                inTransaction {
                    behandlinger.lagreNyttVirkningstidspunkt(
                        behandling.id,
                        virkningstidspunkt
                    )
                }

                return virkningstidspunkt
            }

            is ManueltOpphoer -> throw RuntimeException(
                "Kan ikke fastsette virkningstidspunkt for ${ManueltOpphoer::class.java.simpleName}"
            ) // TODO ai: Hvordan håndtere error cases?
            is Revurdering -> throw RuntimeException(
                "Kan ikke fastsette virkningstidspunkt for ${Revurdering::class.java.simpleName}"
            )
        }
    }

    override fun settKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        inTransaction {
            behandlinger.lagreKommerBarnetTilgode(
                behandlingId,
                kommerBarnetTilgode
            )
        }
    }
}