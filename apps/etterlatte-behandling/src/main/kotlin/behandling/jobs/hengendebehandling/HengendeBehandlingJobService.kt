package no.nav.etterlatte.behandling.jobs.hengendebehandling

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit

class HengendeBehandlingJobService(
    private val dao: HengendeBehandlingDao,
    private val grenseForHengende: Duration = Duration.ofDays(5),
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        val sistEndretFoer = Tidspunkt.now().minus(grenseForHengende.toDays(), ChronoUnit.DAYS)

        listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.TIL_SAMORDNING).forEach { status ->
            val hengendeBehandlinger =
                inTransaction { dao.hentBehandlingerHengendeIStatus(status, sistEndretFoer) }

            if (hengendeBehandlinger.isNotEmpty()) {
                logger.error(
                    "Fant ${hengendeBehandlinger.size} behandling(er) som har stått fast i status $status i mer enn " +
                        "$grenseForHengende uten endring, dette kan tyde på at " +
                        "${if (status == BehandlingStatus.TIL_SAMORDNING) "samordningen" else "iverksettingen"} " +
                        "har stanset opp: " +
                        hengendeBehandlinger.joinToString(", ") {
                            "behandlingId=${it.behandlingId} sakId=${it.sakId.sakId} sistEndret=${it.sistEndret}"
                        },
                )
            }
        }
    }
}
