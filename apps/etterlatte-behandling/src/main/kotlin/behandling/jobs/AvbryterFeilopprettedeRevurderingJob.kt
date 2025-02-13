package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

/**
 * Det ble opprettet aktivitetspliktrevurderinger for 12-månederskravet for feil måneder i starten av februar.
 * Disse skal avbrytes.
 *
 * Siden revurderinger er noe vi har sendt til statistikken er det best å avbryte de via service-laget
 * for å få riktig håndtering.
 */
class AvbryterFeilopprettedeRevurderingJob(
    private val behandlingService: BehandlingService,
    private val avbrytRevurderingerDao: AvbrytRevurderingerDao,
    private val isLeader: () -> Boolean,
    dataSource: DataSource,
    val sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val jobbNavn = this::class.java.simpleName
    private val logger = LoggerFactory.getLogger(AvbryterFeilopprettedeRevurderingJob::class.java)
    private var jobContext: Context =
        Context(
            Self(AvbrytRevurderinger::class.java.simpleName),
            DatabaseContext(dataSource),
            sakTilgangDao,
            HardkodaSystembruker.aktivitetsplikt,
        )

    override fun schedule(): Timer =
        fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            period = Duration.of(3, ChronoUnit.HOURS).toMillis(),
            loggerInfo = LoggerInfo(logger = logger, sikkerLogg = null, loggTilSikkerLogg = false),
        ) {
            AvbrytRevurderinger(behandlingService, avbrytRevurderingerDao, isLeader).run(jobContext)
        }
}

class AvbrytRevurderinger(
    private val behandlingService: BehandlingService,
    private val avbrytRevurderingerDao: AvbrytRevurderingerDao,
    private val isLeader: () -> Boolean,
) {
    private val logger = LoggerFactory.getLogger(AvbrytRevurderinger::class.java)

    fun run(context: Context) {
        if (isLeader()) {
            Kontekst.set(context)
            logger.info("pod er leader, kjører jobb for å avbryte revurderinger")
            avbrytRevurderinger()
        } else {
            logger.info("pod er ikke leader, kjører ikke jobb for å avbryte revurderinger")
        }
    }

    private fun avbrytRevurderinger() {
        val behandlingerSomSkalAvbrytes = inTransaction { avbrytRevurderingerDao.hentAktuelleRevurderinger() }
        if (behandlingerSomSkalAvbrytes.isEmpty()) {
            logger.info("Ingen behandlinger å avbryte, kjører ikke jobb")
            return
        }

        val antallAvbrutte =
            behandlingerSomSkalAvbrytes
                .map {
                    inTransaction {
                        val behandling = behandlingService.hentBehandling(it) ?: return@inTransaction false
                        if (!behandling.status.kanAvbrytes()) {
                            return@inTransaction false
                        }
                        behandlingService.avbrytBehandling(
                            behandling.id,
                            HardkodaSystembruker.oppgave,
                            AarsakTilAvbrytelse.AVBRUTT_PAA_GRUNN_AV_FEIL,
                            "Automatisk avbrutt på grunn av feil opprettet behandling",
                        )
                        true
                    }
                }.count { it }
        logger.info("Avbrøt $antallAvbrutte av ${behandlingerSomSkalAvbrytes.size} behandlinger.")
    }
}

class AvbrytRevurderingerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger = LoggerFactory.getLogger(AvbrytRevurderingerDao::class.java)

    fun hentAktuelleRevurderinger(): List<UUID> =
        connectionAutoclosing.hentConnection { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    select b.id
                    from oppgave o
                             inner join behandling b on o.referanse = b.id::text
                    where o.type = 'REVURDERING'
                      and b.revurdering_aarsak = 'AKTIVITETSPLIKT'
                      and opprettet >= '2025-02-1'
                      and merknad = 'Vurdering av aktivitetsplikt ved 12 måneder'
                      and opprettet <= '2025-02-04';
                    """.trimIndent(),
                )
            return@hentConnection statement
                .executeQuery()
                .toList {
                    getUUID("id")
                }.also { logger.info("fant ${it.size} revurderinger som skal avbrytes") }
        }
}
