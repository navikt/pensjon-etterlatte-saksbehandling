package no.nav.etterlatte.behandling

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*


interface GenerellBehandlingService {

    fun hentBehandlinger(): List<Behandling>
    fun hentBehandlingstype(behandling: UUID): BehandlingType?
    fun hentBehandlingerISak(sakid: Long): List<Behandling>
    fun slettBehandlingerISak(sak: Long)
    fun avbrytBehandling(behandling: UUID)
    fun grunnlagISakEndret(sak: Long)
    fun registrerVedtakHendelse(
        behandling: UUID,
        vedtakId: Long,
        hendelse: String,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    )
    fun hentHendelserIBehandling(behandling: UUID): List<LagretHendelse>
}

class RealGenerellBehandlingService(
    val behandlinger: BehandlingDao,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val revurderingFactory: RevurderingFactory,
    private val hendelser: HendelseDao
) : GenerellBehandlingService {

    override fun hentBehandlinger(): List<Behandling> {
        return inTransaction { behandlinger.alleBehandlinger() }
    }

    override fun hentBehandlingstype(behandling: UUID): BehandlingType? {
        return inTransaction { behandlinger.hentBehandlingType(behandling) }
    }

    override fun hentBehandlingerISak(sakid: Long): List<Behandling> {
        return inTransaction {
            behandlinger.alleBehandingerISak(sakid)
        }
    }

    override fun slettBehandlingerISak(sak: Long) {
        inTransaction {
            println("Sletter alle behandlinger i sak: $sak")
            behandlinger.slettBehandlingerISak(sak)
        }
    }

    override fun avbrytBehandling(behandling: UUID) {
        inTransaction {
            behandlinger.hentBehandlingType(behandling)?.let {
                when (it) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> {
                        foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling).avbrytBehandling()
                    }
                    BehandlingType.REVURDERING -> {
                        revurderingFactory.hentRevurdering(behandling).avbrytBehandling()
                    }
                }.also {
                    runBlocking {
                        behandlingHendelser.send(behandling to BehandlingHendelseType.AVBRUTT)
                    }
                }
            }
        }
    }

    override fun grunnlagISakEndret(sak: Long) {
        inTransaction {
            behandlinger.alleBehandingerISak(sak)
        }.also {
            runBlocking {
                it.forEach {
                    behandlingHendelser.send(it.id to BehandlingHendelseType.GRUNNLAGENDRET)
                }
            }
        }
    }

    override fun registrerVedtakHendelse(
        behandling: UUID,
        vedtakId: Long,
        hendelse: String,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) {
        inTransaction {
            behandlinger.hentBehandlingType(behandling)?.let {
                when (it) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> {
                        foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling)
                            .registrerVedtakHendelse(
                                vedtakId,
                                hendelse,
                                inntruffet,
                                saksbehandler,
                                kommentar,
                                begrunnelse
                            )
                    }
                    BehandlingType.REVURDERING -> {
                        revurderingFactory.hentRevurdering(behandling).registrerVedtakHendelse(
                            vedtakId,
                            hendelse,
                            inntruffet,
                            saksbehandler,
                            kommentar,
                            begrunnelse
                        )
                    }
                }
            }
        }
    }

    override fun hentHendelserIBehandling(behandlingId: UUID): List<LagretHendelse> {
        return inTransaction {
        hendelser.finnHendelserIBehandling(behandlingId)
        }
    }

}

