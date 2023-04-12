package no.nav.etterlatte.behandling

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.util.*

interface BehandlingStatusService {
    fun settOpprettet(behandlingId: UUID, dryRun: Boolean = true)
    fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean = true)
    fun settBeregnet(behandlingId: UUID, dryRun: Boolean = true)
    fun sjekkOmKanFatteVedtak(behandlingId: UUID)
    fun settFattetVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse)
    fun sjekkOmKanAttestere(behandlingId: UUID)
    fun settAttestertVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse)
    fun sjekkOmKanReturnereVedtak(behandlingId: UUID)
    fun settReturnertVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse)
    fun settIverksattVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse)
    fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(): SakIDListe
}

class BehandlingStatusServiceImpl constructor(
    private val behandlingDao: BehandlingDao,
    private val behandlingService: GenerellBehandlingService
) : BehandlingStatusService {
    override fun settOpprettet(behandlingId: UUID, dryRun: Boolean) {
        val behandling = hentBehandling(behandlingId).tilOpprettet()

        if (!dryRun) {
            inTransaction {
                behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
            }
        }
    }

    override fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean) {
        val behandling = hentBehandling(behandlingId).tilVilkaarsvurdert()

        if (!dryRun) {
            inTransaction {
                behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
            }
        }
    }

    override fun settBeregnet(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilBeregnet().lagreEndring(dryRun)
    }

    override fun sjekkOmKanFatteVedtak(behandlingId: UUID) {
        hentBehandling(behandlingId).tilFattetVedtak()
    }

    override fun settFattetVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse) {
        val behandling = hentBehandling(behandlingId)
        inTransaction {
            lagreNyBehandlingStatus(behandling.tilFattetVedtak())
            registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.FATTET)
        }
    }

    override fun sjekkOmKanAttestere(behandlingId: UUID) {
        hentBehandling(behandlingId).tilAttestert()
    }

    override fun settAttestertVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse) {
        val behandling = hentBehandling(behandlingId)
        inTransaction {
            lagreNyBehandlingStatus(behandling.tilAttestert())
            registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
        }
    }

    override fun sjekkOmKanReturnereVedtak(behandlingId: UUID) {
        hentBehandling(behandlingId).tilReturnert()
    }

    override fun settReturnertVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse) {
        val behandling = hentBehandling(behandlingId)
        inTransaction {
            lagreNyBehandlingStatus(behandling.tilReturnert())
            registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.UNDERKJENT)
        }
    }

    override fun settIverksattVedtak(behandlingId: UUID, vedtakHendelse: VedtakHendelse) {
        val behandling = hentBehandling(behandlingId)
        inTransaction {
            lagreNyBehandlingStatus(behandling.tilIverksatt(), Tidspunkt.now().toLocalDatetimeUTC())
            registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.IVERKSATT)
        }
    }

    override fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning() = inTransaction {
        behandlingDao.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning()
    }

    fun registrerVedtakHendelse(behandlingId: UUID, vedtakHendelse: VedtakHendelse, hendelseType: HendelseType) {
        behandlingService.registrerVedtakHendelse(
            behandlingId,
            vedtakHendelse,
            hendelseType
        )
    }

    private fun Behandling.lagreEndring(dryRun: Boolean) {
        if (dryRun) return
        inTransaction {
            lagreNyBehandlingStatus(this)
        }
    }

    private fun lagreNyBehandlingStatus(behandling: Behandling, sistEndret: LocalDateTime) =
        behandlingDao.lagreStatus(behandling.id, behandling.status, sistEndret)

    private fun lagreNyBehandlingStatus(behandling: Behandling) =
        behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)

    private fun hentBehandling(behandlingId: UUID): Behandling = inTransaction {
        behandlingDao.hentBehandling(behandlingId)
            ?: throw NotFoundException("Fant ikke behandling med id=$behandlingId")
    }
}