package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.sync.Mutex
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.trygdetid.avtale.AvtaleRepository
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

data class AvvikAaSjekke(
    val trygdetidId: UUID,
    val behandlingId: UUID,
    val sakId: SakId,
)

class TrygdetidAvvikRepository(
    private val dataSource: DataSource,
) {
    fun hentIkkeSjekketTrygdetid(): AvvikAaSjekke? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT trygdetid_id, behandling_id, sak_id from trygdetid_avvik where status = 'IKKE_SJEKKET' limit 1
                    """.trimIndent(),
            ).let { query ->
                session.run(
                    query
                        .map { row ->
                            val trygdetidId = row.uuid("trygdetid_id")
                            val behandlingId = row.uuid("behandling_id")
                            val sakId = SakId(row.long("sak_id"))

                            AvvikAaSjekke(trygdetidId = trygdetidId, behandlingId = behandlingId, sakId = sakId)
                        }.asSingle,
                )
            }
        }

    fun oppdaterAvvikResultat(
        behandlingId: UUID,
        trygdetidId: UUID,
        avvik: TrygdetidAvvik?,
    ) {
        val avvikStatus =
            when (avvik) {
                null -> "SJEKKET_INGEN_AVVIK"
                else -> "SJEKKET_HAR_AVVIK"
            }

        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    UPDATE trygdetid_avvik SET status = :status, avvik = (:avvik)::jsonb 
                    WHERE trygdetid_id = :trygdetidId AND behandling_id = :behandlingId 
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "status" to avvikStatus,
                        "avvik" to avvik?.toJson(),
                        "trygdetidId" to trygdetidId,
                        "behandlingId" to behandlingId,
                    ),
            ).let { query ->
                session.run(query.asUpdate)
            }
        }
    }
}

enum class SjekkAvvikToggle(
    private val toggle: String,
) : FeatureToggle {
    AVVIK_JOBB_ENABLED("trygdetid-sjekk-avvik-job"),
    ;

    override fun key(): String = toggle
}

class SjekkAvvikJobb(
    val sjekkAvvikService: SjekkAvvikService,
    val leaderElection: LeaderElection,
    val featureToggleService: FeatureToggleService,
    val periode: Duration,
    val initialDelaySeconds: Long,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(SjekkAvvikJobb::class.java)
    private val jobbKjoererMutex = Mutex()

    override fun schedule(): Timer {
        logger.info(
            "Setter opp periodisk jobb for sjekk av trygdetid til å kjøre med periode $periode, " +
                "etter $initialDelaySeconds sekunder fra oppstart",
        )

        return fixedRateCancellableTimer(
            name = SjekkAvvikJobb::class.simpleName,
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
        ) {
            if (leaderElection.isLeader() &&
                featureToggleService.isEnabled(
                    SjekkAvvikToggle.AVVIK_JOBB_ENABLED,
                    false,
                )
            ) {
                if (jobbKjoererMutex.tryLock()) {
                    try {
                        var antallSjekket = 0
                        while (true) {
                            if (antallSjekket % 100 == 0 &&
                                !featureToggleService.isEnabled(
                                    SjekkAvvikToggle.AVVIK_JOBB_ENABLED,
                                    false,
                                )
                            ) {
                                break
                            }
                            val harSjekketEtAvvik = sjekkAvvikService.sjekkTrygdetid()
                            if (!harSjekketEtAvvik) {
                                break
                            }
                            antallSjekket++
                        }
                    } catch (e: Exception) {
                        logger.warn("Jobb for sjekking av trygdetid feilet", e)
                    } finally {
                        jobbKjoererMutex.unlock()
                    }
                } else {
                    logger.info("Jobb for sjekking av trygdetid kjører allerede")
                }
            }
        }
    }
}

class SjekkAvvikService(
    private val avvikRepository: TrygdetidAvvikRepository,
    private val trygdetidRepository: TrygdetidRepository,
    private val avtaleRepository: AvtaleRepository,
) {
    fun sjekkTrygdetid(): Boolean {
        val trygdetidAaSjekke = avvikRepository.hentIkkeSjekketTrygdetid() ?: return false

        val avvik = finnAvvik(trygdetidAaSjekke.behandlingId, trygdetidAaSjekke.trygdetidId)
        avvikRepository.oppdaterAvvikResultat(trygdetidAaSjekke.behandlingId, trygdetidAaSjekke.trygdetidId, avvik)
        return true
    }

    private fun finnAvvik(
        behandlingId: UUID,
        trygdetidId: UUID,
    ): TrygdetidAvvik? {
        val trygdetid = trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId) ?: return null
        if (trygdetid.beregnetTrygdetid == null) {
            return null
        }
        val nordiskKonvensjon = avtaleRepository.hentAvtale(behandlingId)?.nordiskTrygdeAvtale == JaNei.JA
        val foedselsdato =
            toLocalDate(trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FOEDSELSDATO })
                ?: return null
        val doedsdato =
            toLocalDate(trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.DOEDSDATO })
                ?: return null

        val nyttResultat =
            TrygdetidBeregningService
                .beregnTrygdetid(
                    trygdetidGrunnlag = trygdetid.trygdetidGrunnlag,
                    foedselsDato = foedselsdato,
                    doedsDato = doedsdato,
                    norskPoengaar = trygdetid.overstyrtNorskPoengaar,
                    yrkesskade = trygdetid.yrkesskade,
                    nordiskKonvensjon = nordiskKonvensjon,
                )?.resultat
                ?: throw InternfeilException("Har en beregnet trygdetid som ikke lar seg reberegne, trygdetidId = $trygdetidId")

        return TrygdetidAvvik.fraBeregninger(trygdetid.beregnetTrygdetid.resultat, nyttResultat)
    }
}

private fun toLocalDate(opplysningsgrunnlag: Opplysningsgrunnlag?): java.time.LocalDate? =
    opplysningsgrunnlag?.let {
        objectMapper.readValue(it.opplysning.toString())
    }

data class TrygdetidAvvik(
    val gammelBeregnet: DetaljertBeregnetTrygdetidResultat,
    val nyBeregnet: DetaljertBeregnetTrygdetidResultat,
) {
    companion object {
        fun fraBeregninger(
            gammelBeregnet: DetaljertBeregnetTrygdetidResultat,
            nyBeregnet: DetaljertBeregnetTrygdetidResultat,
        ): TrygdetidAvvik? {
            if (gammelBeregnet == nyBeregnet) {
                return null
            }
            return TrygdetidAvvik(
                gammelBeregnet = gammelBeregnet,
                nyBeregnet = nyBeregnet,
            )
        }
    }
}
