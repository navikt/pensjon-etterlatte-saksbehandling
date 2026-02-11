package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.adhoc

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.vedtaksvurdering.InnvilgetPeriode
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class IkkeInnvilgedePerioderJob(
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val vedtaksvurderingRepository: VedtaksvurderingRepository,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val periode: Duration,
    private val featureToggleService: FeatureToggleService,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med service=${vedtaksvurderingService::class.simpleName} og periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            if (erLeader() && featureToggleService.isEnabled(skalTelleHullToggle, false)) {
                logger.info("$jobbNavn starter vurdering av alle saker som har fattede vedtak")
                sakerMedFattedeVedtak()
                    .mapNotNull { sakId ->
                        val perioder = hentInnvilgedePerioder(sakId)
                        if (perioder.size > 1) {
                            Pair(sakId, perioder)
                        } else {
                            null
                        }
                    }.forEach { (sakId, perioder) ->
                        logger.info("Fant sak med hull: $sakId: ${perioderString(perioder)}")
                    }
                logger.info("$jobbNavn ferdig med å hente saker med hull i innvilgede perioder")
            }
        }
    }

    private fun hentInnvilgedePerioder(sakId: SakId): List<InnvilgetPeriode> {
        val innvilgedePerioder: List<InnvilgetPeriode> =
            try {
                vedtaksvurderingService.hentInnvilgedePerioder(sakId)
            } catch (e: Exception) {
                logger.error("Feilet på sak $sakId", e)
                emptyList()
            }
        return innvilgedePerioder
    }

    private fun sakerMedFattedeVedtak(): List<SakId> {
        val saker: List<SakId> =
            vedtaksvurderingRepository.inTransaction { tx ->
                tx.session {
                    hentListe(
                        queryString = "select distinct sakid from vedtak where saksbehandlerid is not null",
                        converter = { SakId(it.long("sakid")) },
                    )
                }
            }
        return saker
    }

    private fun perioderString(perioder: List<InnvilgetPeriode>): String =
        perioder.joinToString(", ") { periode ->
            "${periode.periode.fom}-${periode.periode.tom}"
        }

    private val skalTelleHullToggle =
        object : FeatureToggle {
            override fun key(): String = "skal-telle-hull-i-innvilgede-perioder"
        }
}
