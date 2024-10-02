package no.nav.etterlatte

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

class BeregningsgrunnlagRyddeJob(
    private val beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
    private val beregningRepository: BeregningRepository,
    private val behandlingKlient: BehandlingKlient,
    private val ryddeDao: RyddeBeregningsgrunnlagDao,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger: Logger = LoggerFactory.getLogger(BeregningsgrunnlagRyddeJob::class.java)
    private val jobbnavn = this::class.simpleName

    override fun schedule(): Timer =
        fixedRateCancellableTimer(
            name = jobbnavn,
            initialDelay = initialDelay,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
        ) {
            RyddOppBeregningsgrunnlag(
                leaderElection = leaderElection,
                beregningsGrunnlagRepository = beregningsGrunnlagRepository,
                behandlingKlient = behandlingKlient,
                ryddeDao = ryddeDao,
                beregningRepository = beregningRepository,
            ).run()
        }

    class RyddOppBeregningsgrunnlag(
        private val leaderElection: LeaderElection,
        private val beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
        private val beregningRepository: BeregningRepository,
        private val behandlingKlient: BehandlingKlient,
        private val ryddeDao: RyddeBeregningsgrunnlagDao,
    ) {
        fun run() {
            if (leaderElection.isLeader()) {
                logger.info("Pod er leader, begynner å finne og rydde i beregninger uten grunnlag")
                repeat(10) {
                    try {
                        ryddGrunnlagIEnBehandling()
                    } catch (e: Exception) {
                        logger.warn("Kunne ikke fikse i beregningsgrunnlag for en sak, på grunn av feil", e)
                    }
                }
            }
        }

        private fun ryddGrunnlagIEnBehandling() {
            val behandlingSomMaaRyddes = ryddeDao.finnBehandlingMedBeregningUtenGrunnlag()
            if (behandlingSomMaaRyddes == null) {
                logger.warn("Vi har ryddet i alle sakene som har beregning uten beregningsgrunnlag")
                return
            }
            val (behandlingId, sakId) = behandlingSomMaaRyddes

            // sanity-check
            val beregningsgrunnlagForBehandlingViSkalFikse =
                beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId)
            if (beregningsgrunnlagForBehandlingViSkalFikse != null) {
                throw InternfeilException(
                    "Behandling som skal mangle beregningsgrunnlag (id=$behandlingId, sak=" +
                        "$sakId) har allerede et beregningsgrunnlag.",
                )
            }
            logger.info("Behandling med id=$behandlingId i sak $sakId mangler beregningsgrunnlag men har beregning")

            val behandlingerISak = behandlingKlient.hentBehandlingerISak(sakId, HardkodaSystembruker.ryddeBeregning)
            val beregningerISak = behandlingerISak.mapNotNull { beregningRepository.hent(it.id) }
            val beregningUtenGrunnlag =
                beregningerISak.find { it.behandlingId == behandlingId }
                    ?: throw InternfeilException(
                        "Behandling $behandlingId skal ha beregning uten beregningsgrunnlag, men vi finner ikke beregningen",
                    )

            // behandlingen vi har kopiert fra må være en iverksatt eller attestert behandling i saken, som var den
            // forrige behandlingen til denne
            val aktuelleBehandlinger =
                behandlingerISak
                    .filter { BehandlingStatus.iverksattEllerAttestert().contains(it.status) }
                    .map { it.id }

            // Beregningen på den vi har kopiert fra må være den siste gjort før denne,
            // på en attestert / iverksatt behandling
            val aktuellBeregning =
                beregningerISak
                    .filter { it.behandlingId in aktuelleBehandlinger }
                    .filter { it.behandlingId != behandlingId }
                    .filter { it.beregnetDato < beregningUtenGrunnlag.beregnetDato }
                    .maxByOrNull { it.beregnetDato }
                    ?: throw InternfeilException(
                        "Vi finner ikke en beregning i sak $sakId som er fra en attestert eller iverksatt " +
                            "behandling, og er beregnet før behandling $behandlingId som mangler " +
                            "beregningsgrunnlag. Enten er det en feil i logikken, eller vi har andre feil " +
                            "knyttet til manglende beregningsgrunnlag",
                    )

            // Vi kan kopiere inn grunnlaget fra den aktuelle beregeningen til behandlingen vi mangler grunnlag i
            // med oppdatert behandlingId
            val implisittBruktBeregningsgrunnlag =
                beregningsGrunnlagRepository.finnBeregningsGrunnlag(aktuellBeregning.behandlingId)
                    ?: throw InternfeilException(
                        "Den tidligere beregningen i sak $sakId som vi skal ha implisitt " +
                            "brukt beregningsgrunnlaget til mangler beregningsgrunnlag selv. Må sees på!",
                    )
            beregningsGrunnlagRepository.lagreBeregningsGrunnlag(
                implisittBruktBeregningsgrunnlag.copy(
                    behandlingId = behandlingId,
                ),
            )

            logger.info(
                "Kopierte implisitt brukt beregningsgrunnlag i beregningen for behandling med id=" +
                    "$behandlingId fra behandling ${aktuellBeregning.behandlingId} i sak $sakId",
            )
        }
    }
}

class RyddeBeregningsgrunnlagDao(
    private val dataSource: DataSource,
) {
    fun finnBehandlingMedBeregningUtenGrunnlag(): Pair<UUID, SakId>? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """
                    select distinct on (behandlingid) behandlingid, sakid
                    from beregningsperiode bp
                             left outer join beregningsgrunnlag bg on bp.behandlingid = bg.behandlings_id
                             left outer join overstyr_beregningsgrunnlag obg on bp.behandlingid = obg.behandlings_id
                    where bg.behandlings_id is null
                      and obg.behandlings_id is null
                      limit = 1
                    """.trimIndent(),
                ).map {
                    it.uuid("behandlingid") to it.long("sakId")
                }.asSingle,
            )
        }
}
