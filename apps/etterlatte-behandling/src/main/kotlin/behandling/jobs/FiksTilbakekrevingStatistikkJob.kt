package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingHendelserService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

enum class FiksToggles(
    private val key: String,
) : FeatureToggle {
    FIKS_TILBAKEKREVING_STATISTIKK("fiks-tilbakekreving-statistikk"),
    ;

    override fun key(): String = key
}

class FiksTilbakekrevingStatistikkJob(
    private val tilbakekrevinghendelser: TilbakekrevingHendelserService,
    private val behandlingService: BehandlingService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val featureToggleService: FeatureToggleService,
    val sakTilgangDao: SakTilgangDao,
    val dataSource: DataSource,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context =
        Context(
            Self(FiksTilbakekrevingStatistikkJob::class.java.simpleName),
            DatabaseContext(dataSource),
            sakTilgangDao,
            HardkodaSystembruker.uttrekk,
        )

    override fun schedule(): Timer {
        logger.info("Starter jobb $jobbNavn med interval $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                if (featureToggleService.isEnabled(FiksToggles.FIKS_TILBAKEKREVING_STATISTIKK, false)) {
                    Kontekst.set(
                        jobContext,
                    )

                    val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString("c5a70a61-2c28-42e7-ab68-f05ae07f6e3a"))

                    inTransaction {
                        if (tilbakekreving.status == TilbakekrevingStatus.ATTESTERT) {
                            tilbakekrevinghendelser.sendTilbakekreving(
                                statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
                                type = TilbakekrevingHendelseType.ATTESTERT,
                            )

                            logger.info("Statistikk for tilbakekreving oppdatert")
                        } else {
                            logger.info("Tilbakekreving hadde feil status ${tilbakekreving.status}")
                        }
                    }
                }
            }
        }
    }

    private fun tilbakekrevingForStatistikk(tilbakekreving: TilbakekrevingBehandling): StatistikkTilbakekrevingDto {
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(tilbakekreving.sak.id)?.type
        return StatistikkTilbakekrevingDto(
            tilbakekreving.id,
            tilbakekreving,
            Tidspunkt.now(),
            utlandstilknytningType,
        )
    }
}
