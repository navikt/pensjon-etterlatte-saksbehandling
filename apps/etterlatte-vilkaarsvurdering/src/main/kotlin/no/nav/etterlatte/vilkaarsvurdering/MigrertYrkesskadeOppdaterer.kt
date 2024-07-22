package no.nav.etterlatte.no.nav.etterlatte.vilkaarsvurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID

class MigrertYrkesskadeJob(
    private val oppdaterer: MigrertYrkesskadeOppdaterer,
    private val erLeader: () -> Boolean,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer =
        fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = Duration.ofMinutes(2).toMillis(),
            period = Duration.ofDays(1).toMillis(),
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
        ) {
            if (erLeader()) {
                runBlocking {
                    oppdaterer.settSakPaaAlleMigrerteYrkesskadefordeler(Systembruker.automatiskJobb)
                }
            }
        }
}

class MigrertYrkesskadeOppdaterer(
    val behandlingKlient: BehandlingKlient,
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun settSakPaaAlleMigrerteYrkesskadefordeler(bruker: BrukerTokenInfo) =
        vilkaarsvurderingRepository.hentAlleMigrertYrkesskadefordel().forEach { settSak(it, bruker) }

    private suspend fun settSak(
        behandlingID: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val sak = behandlingKlient.hentBehandling(behandlingID, bruker).sak
        logger.info("Kopler sak $sak til behandling $behandlingID for migrert yrkesskade")
        vilkaarsvurderingRepository.settSakPaaMigrertYrkesskadefordel(behandlingID, sak)
        logger.info("Ferdig med å kople sak $sak til behandling $behandlingID for migrert yrkesskade")
    }
}
