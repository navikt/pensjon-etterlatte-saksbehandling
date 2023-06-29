package no.nav.etterlatte.behandling.manueltopphoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.filterBehandlingerForEnheter
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface ManueltOpphoerService {
    fun hentManueltOpphoer(behandlingId: UUID): ManueltOpphoer?
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

    fun hentManueltOpphoerInTransaction(behandlingId: UUID): ManueltOpphoer?

    fun hentManueltOpphoerOgAlleIverksatteBehandlingerISak(behandlingId: UUID): Pair<ManueltOpphoer, List<Behandling>>?
}

class RealManueltOpphoerService(
    private val behandlingDao: BehandlingDao,
    private val behandlingHendelser: BehandlingHendelserKanal,
    private val hendelseDao: HendelseDao,
    private val featureToggleService: FeatureToggleService
) : ManueltOpphoerService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun hentManueltOpphoer(behandlingId: UUID): ManueltOpphoer? =
        (behandlingDao.hentBehandling(behandlingId) as? ManueltOpphoer?).sjekkEnhet()

    override fun hentManueltOpphoerInTransaction(behandlingId: UUID): ManueltOpphoer? =
        inTransaction { hentManueltOpphoer(behandlingId) }

    override fun hentManueltOpphoerOgAlleIverksatteBehandlingerISak(
        behandlingId: UUID
    ): Pair<ManueltOpphoer, List<Behandling>>? =
        inTransaction {
            hentManueltOpphoer(behandlingId)?.let { opphoer ->
                val andreBehandlinger = behandlingDao.alleBehandlingerISak(opphoer.sak.id)
                    .filter { it.id != behandlingId && it.status == BehandlingStatus.IVERKSATT }
                opphoer to andreBehandlinger
            }
        }

    override fun opprettManueltOpphoer(
        opphoerRequest: ManueltOpphoerRequest
    ): ManueltOpphoer? {
        return inTransaction {
            val alleBehandlingerISak = behandlingDao.alleBehandlingerISak(opphoerRequest.sak).filterForEnheter()
            val forrigeBehandling = alleBehandlingerISak.sisteIkkeAvbrutteBehandling()
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
                    virkningstidspunkt = virkningstidspunkt,
                    kilde = Vedtaksloesning.GJENNY
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
                behandlingDao.opprettBehandling(it)
                hendelseDao.behandlingOpprettet(it.toBehandlingOpprettet())
                it.id
            }?.let { id ->
                (behandlingDao.hentBehandling(id) as ManueltOpphoer).sjekkEnhet()
            }
        }?.also { lagretManueltOpphoer ->
            runBlocking {
                behandlingHendelser.send(
                    Triple(lagretManueltOpphoer.id, BehandlingHendelseType.OPPRETTET, lagretManueltOpphoer.type)
                )
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
                hendelser = hendelseDao
            )
        }
    }

    private fun List<Behandling>.sisteIkkeAvbrutteBehandling(): Behandling? =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }

    private fun List<Behandling>.tidligsteIverksatteVirkningstidspunkt(): Virkningstidspunkt? =
        this.filter { it.status == BehandlingStatus.IVERKSATT }
            .mapNotNull { it.virkningstidspunkt }
            .minByOrNull { it.dato }

    private fun <T : Behandling> List<T>.filterForEnheter() = this.filterBehandlingerForEnheter(
        featureToggleService,
        Kontekst.get().AppUser
    )
    private fun ManueltOpphoer?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterForEnheter().firstOrNull()
    }
}