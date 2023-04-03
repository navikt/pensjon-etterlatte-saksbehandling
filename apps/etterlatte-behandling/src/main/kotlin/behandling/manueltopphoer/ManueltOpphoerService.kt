package no.nav.etterlatte.behandling.manueltopphoer

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface ManueltOpphoerService {
    fun hentManueltOpphoer(behandling: UUID): ManueltOpphoer?
    fun opprettManueltOpphoer(
        opphoerRequest: ManueltOpphoerRequest
    ): ManueltOpphoer?

    fun registrerVedtakHendelse(
        behandling: UUID,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?
    )

    fun hentManueltOpphoerInTransaction(behandling: UUID): ManueltOpphoer?

    fun hentManueltOpphoerOgAlleIverksatteBehandlingerISak(behandling: UUID): Pair<ManueltOpphoer, List<Behandling>>?
}

class RealManueltOpphoerService(
    private val behandlinger: BehandlingDao,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>,
    private val hendelser: HendelseDao
) : ManueltOpphoerService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun hentManueltOpphoer(behandling: UUID): ManueltOpphoer? =
        behandlinger.hentBehandling(behandling) as ManueltOpphoer?

    override fun hentManueltOpphoerInTransaction(behandling: UUID): ManueltOpphoer? =
        inTransaction { behandlinger.hentBehandling(behandling) as ManueltOpphoer? }

    override fun hentManueltOpphoerOgAlleIverksatteBehandlingerISak(
        behandling: UUID
    ): Pair<ManueltOpphoer, List<Behandling>>? =
        inTransaction {
            val opphoer = behandlinger.hentBehandling(behandling) as ManueltOpphoer?
                ?: return@inTransaction null
            val andreBehandlinger = behandlinger.alleBehandlingerISak(opphoer.sak.id)
                .filter { it.id != behandling && it.status == BehandlingStatus.IVERKSATT }
            opphoer to andreBehandlinger
        }

    override fun opprettManueltOpphoer(
        opphoerRequest: ManueltOpphoerRequest
    ): ManueltOpphoer? {
        return inTransaction {
            val alleBehandlingerISak = behandlinger.alleBehandlingerISak(opphoerRequest.sak)
            val forrigeBehandling = alleBehandlingerISak.`siste ikke-avbrutte behandling`()
            val virkningstidspunkt = alleBehandlingerISak.tidligsteIverksatteVirkningstidspunkt()

            if (virkningstidspunkt == null) {
                logger.warn(
                    "Saken med id=${opphoerRequest.sak} har ikke noe tidligste iverksatt virkningstidspunkt, " +
                        "så det er ingenting å opphøre."
                )
                return@inTransaction null
            }

            when (forrigeBehandling) {
                is Foerstegangsbehandling, is Revurdering -> OpprettBehandling(
                    type = BehandlingType.MANUELT_OPPHOER,
                    sakId = forrigeBehandling.sak.id,
                    status = BehandlingStatus.OPPRETTET,
                    persongalleri = forrigeBehandling.persongalleri,
                    opphoerAarsaker = opphoerRequest.opphoerAarsaker,
                    fritekstAarsak = opphoerRequest.fritekstAarsak,
                    virkningstidspunkt = virkningstidspunkt
                )

                is ManueltOpphoer -> {
                    logger.error("Kan ikke manuelt opphøre et manuelt opphør.")
                    null
                }

                null -> {
                    logger.error("En forrige ikke-avbrutt behandling for sak ${opphoerRequest.sak} eksisterer ikke")
                    null
                }
            }?.let {
                behandlinger.opprettBehandling(it)
                hendelser.behandlingOpprettet(it.toBehandlingOpprettet())
                it.id
            }?.let { id ->
                behandlinger.hentBehandling(id) as ManueltOpphoer
            }
        }?.also { lagretManueltOpphoer ->
            runBlocking {
                behandlingHendelser.send(lagretManueltOpphoer.id to BehandlingHendelseType.OPPRETTET)
            }
            logger.info("Manuelt opphør med id=${lagretManueltOpphoer.id} er opprettet.")
        }
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
        hentManueltOpphoer(behandling)?.let { manueltOpphoer ->
            registrerVedtakHendelseFelles(
                vedtakId = vedtakId,
                hendelse = hendelse,
                inntruffet = inntruffet,
                saksbehandler = saksbehandler,
                kommentar = kommentar,
                begrunnelse = begrunnelse,
                lagretBehandling = manueltOpphoer,
                hendelser = hendelser
            )
        }
    }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`(): Behandling? =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }

    private fun List<Behandling>.tidligsteIverksatteVirkningstidspunkt(): Virkningstidspunkt? =
        this.filter { it.status == BehandlingStatus.IVERKSATT }
            .mapNotNull { it.virkningstidspunkt }
            .minByOrNull { it.dato }
}