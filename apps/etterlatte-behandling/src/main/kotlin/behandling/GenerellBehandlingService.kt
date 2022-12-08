package no.nav.etterlatte.behandling

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.util.*

interface GenerellBehandlingService {

    fun hentBehandlinger(): List<Behandling>
    fun hentBehandling(behandling: UUID): Behandling?
    fun hentBehandlingstype(behandling: UUID): BehandlingType?
    fun hentBehandlingerISak(sakid: Long): List<Behandling>
    fun slettBehandlingerISak(sak: Long)
    fun avbrytBehandling(behandling: UUID, saksbehandler: String)
    fun grunnlagISakEndret(sak: Long)
    fun registrerVedtakHendelse(
        behandling: UUID,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    )

    fun hentHendelserIBehandling(behandling: UUID): List<LagretHendelse>
    fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling>
    fun alleSakIderForSoekerMedFnr(fnr: String): List<Long>
    fun hentDetaljertBehandling(behandlingsId: UUID): DetaljertBehandling?
    fun hentSakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>>
}

class RealGenerellBehandlingService(
    val behandlinger: BehandlingDao,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val revurderingFactory: RevurderingFactory,
    private val hendelser: HendelseDao,
    private val manueltOpphoerService: ManueltOpphoerService
) : GenerellBehandlingService {

    val logger = LoggerFactory.getLogger(this::class.java)

    override fun hentBehandlinger(): List<Behandling> {
        return inTransaction { behandlinger.alleBehandlinger() }
    }

    override fun hentBehandling(behandling: UUID): Behandling? {
        return inTransaction {
            behandlinger.hentBehandlingType(behandling)?.let { behandlinger.hentBehandling(behandling, it) }
        }
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

    override fun avbrytBehandling(behandlingId: UUID, saksbehandler: String) {
        inTransaction {
            val behandling = behandlinger.hentBehandling(behandlingId)
                ?: throw BehandlingNotFoundException("Fant ikke behandling med id=$behandlingId som skulle avbrytes")
            if (!behandling.status.kanAvbrytes()) {
                throw IllegalStateException("Kan ikke avbryte en behandling med status ${behandling.status}")
            }
            behandlinger.avbrytBehandling(behandlingId).also {
                hendelser.behandlingAvbrutt(behandling, saksbehandler)
                runBlocking {
                    behandlingHendelser.send(behandlingId to BehandlingHendelseType.AVBRUTT)
                }
            }
        }
    }

    override fun grunnlagISakEndret(sak: Long) {
        inTransaction {
            behandlinger.alleAktiveBehandlingerISak(sak)
        }.also {
            runBlocking {
                it.forEach {
                    behandlingHendelser.send(it.id to BehandlingHendelseType.GRUNNLAGENDRET)
                }
            }
        }
    }

    override fun hentDetaljertBehandling(behandlingsId: UUID): DetaljertBehandling? {
        return hentBehandling(behandlingsId)?.toDetaljertBehandling()
    }

    override fun registrerVedtakHendelse(
        behandling: UUID,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    ) {
        inTransaction {
            behandlinger.hentBehandlingType(behandling)?.let {
                when (it) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> {
                        foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling).registrerVedtakHendelse(
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
                    BehandlingType.MANUELT_OPPHOER -> {
                        manueltOpphoerService.registrerVedtakHendelse(
                            behandling,
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

    override fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling> {
        return inTransaction {
            behandlinger.alleBehandlingerForSoekerMedFnr(fnr)
        }
    }

    override fun alleSakIderForSoekerMedFnr(fnr: String): List<Long> {
        return inTransaction {
            behandlinger.alleSakIderMedUavbruttBehandlingForSoekerMedFnr(fnr)
        }
    }

    override fun hentSakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>> {
        return inTransaction {
            behandlinger.sakerOgRollerMedFnrIPersongalleri(fnr)
        }
    }
}