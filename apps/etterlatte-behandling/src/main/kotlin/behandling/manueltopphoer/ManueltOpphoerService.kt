package no.nav.etterlatte.behandling.manueltopphoer

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.ManueltOpphoer
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.hendelse.registrerVedtakHendelseFelles
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

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
        begrunnelse: String?,
        behandlingDao: BehandlingDao
    )

    fun hentManueltOpphoerInTransaction(behandling: UUID): ManueltOpphoer?
}

class RealManueltOpphoerService(
    private val behandlinger: BehandlingDao,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>,
    private val hendelser: HendelseDao
) : ManueltOpphoerService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun hentManueltOpphoer(behandling: UUID): ManueltOpphoer? =
        behandlinger.hentBehandling(behandling, BehandlingType.MANUELT_OPPHOER) as ManueltOpphoer?

    override fun hentManueltOpphoerInTransaction(behandling: UUID): ManueltOpphoer? =
        inTransaction { behandlinger.hentBehandling(behandling, BehandlingType.MANUELT_OPPHOER) as ManueltOpphoer? }

    override fun opprettManueltOpphoer(
        opphoerRequest: ManueltOpphoerRequest
    ): ManueltOpphoer? {
        return inTransaction {
            val forrigeBehandling =
                behandlinger.alleBehandingerISak(opphoerRequest.sak).`siste ikke-avbrutte behandling`()

            when (forrigeBehandling) {
                is Foerstegangsbehandling -> ManueltOpphoer(
                    sak = forrigeBehandling.sak,
                    persongalleri = forrigeBehandling.persongalleri,
                    opphoerAarsaker = opphoerRequest.opphoerAarsaker,
                    fritekstAarsak = opphoerRequest.fritekstAarsak
                )

                is Revurdering -> ManueltOpphoer(
                    sak = forrigeBehandling.sak,
                    persongalleri = forrigeBehandling.persongalleri,
                    opphoerAarsaker = opphoerRequest.opphoerAarsaker,
                    fritekstAarsak = opphoerRequest.fritekstAarsak
                )

                is ManueltOpphoer -> {
                    logger.error("Kan ikke manuelt opphoere et manuelt opphoer.")
                    null
                }

                else -> {
                    logger.error(
                        "En forrige ikke-avbrutt behandling for sak ${opphoerRequest.sak} eksisterer ikke eller " +
                            "er av en type som gjør at manuelt opphoer ikke kan opprettes: " +
                            "${forrigeBehandling?.javaClass?.kotlin}"
                    )
                    null
                }
            }?.let {
                behandlinger.opprettManueltOpphoer(it).also { lagretManueltOpphoer ->
                    hendelser.behandlingOpprettet(lagretManueltOpphoer)
                }
            }
        }?.also { lagretManueltOpphoer ->
            runBlocking {
                behandlingHendelser.send(lagretManueltOpphoer.id to BehandlingHendelseType.OPPRETTET)
            }
            logger.info("Manuelt opphoer er opprettet.")
        }
    }

    override fun registrerVedtakHendelse(
        behandling: UUID,
        vedtakId: Long,
        hendelse: HendelseType,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        begrunnelse: String?,
        behandlingDao: BehandlingDao
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
                hendelser = hendelser,
                behandlingDao = behandlingDao
            )
        }
    }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`() =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }
}