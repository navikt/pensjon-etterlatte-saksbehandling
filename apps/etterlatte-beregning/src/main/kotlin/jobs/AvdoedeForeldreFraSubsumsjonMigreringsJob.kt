package no.nav.etterlatte.jobs

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

@Suppress("UNCHECKED_CAST")
class AvdoedeForeldreFraSubsumsjonMigreringsJob(
    private val dataSource: DataSource,
    private val beregningRepository: BeregningRepository,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private val finnKandidaterBeregningsGrunnlag =
        """
        SELECT
            behandlings_id
        FROM beregningsgrunnlag
        WHERE beregnings_metode_flere_avdoede IS NOT NULL
        """.trimIndent()

    private val oppdaterBeregningsPeriode =
        """
        UPDATE beregningsperiode
        SET avdoede_foreldre = :avdoedeForeldre::JSONB
        WHERE id = :id
        """.trimIndent()

    private fun Map<String, Any?>.erRegelNode() = this.containsKey("regel") && this.containsKey("verdi")

    private fun Map<String, Any?>.toRegel() = this["regel"] as Map<String, Any?>

    private fun Map<String, Any?>.toVerdi() = this["verdi"] as List<String>

    private fun Map<String, Any?>.erBeskrivelseNode() = this.containsKey("beskrivelse")

    private fun Map<String, Any?>.erAvdoedeForeldreNode() = this["beskrivelse"] as String == "Finner om søker har to avdøde foreldre"

    private fun Map<String, Any?>.noder() = this["noder"] as List<Map<String, Any?>>

    private fun processSubsumsjonsNode(subsumsjonsNode: Map<String, Any?>): List<String> {
        if (subsumsjonsNode.erRegelNode()) {
            val regel = subsumsjonsNode.toRegel()

            return if (regel.erBeskrivelseNode() && regel.erAvdoedeForeldreNode()) {
                subsumsjonsNode.toVerdi()
            } else {
                subsumsjonsNode.noder().flatMap { node ->
                    processSubsumsjonsNode(node)
                }
            }
        }
        return emptyList()
    }

    private fun finnBeregningsGrunnlagMedFlereAvdoede() =
        dataSource
            .transaction { tx ->
                queryOf(
                    statement = finnKandidaterBeregningsGrunnlag,
                ).let { query ->
                    tx.run(query.map { it.uuid("behandlings_id") }.asList)
                }
            }.also { logger.info("Fant ${it.size} beregningsgrunnlag med flere avøded") }

    private fun oppdaterBeregningsPeriodeMedAvdoedeForeldre(
        id: UUID,
        avdoedeForeldre: List<String>,
    ) {
        dataSource.transaction { tx ->
            queryOf(
                statement = oppdaterBeregningsPeriode,
                paramMap = mapOf("id" to id, "avdoedeForeldre" to avdoedeForeldre.toJson()),
            ).let { query ->
                tx.run(query.asUpdate)
            }
        }
    }

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                finnBeregningsGrunnlagMedFlereAvdoede().forEach { behandlingsId ->
                    logger.info("Nehandling $behandlingsId")

                    beregningRepository.hent(behandlingsId)?.let { beregning ->
                        logger.info(
                            "Behandling $behandlingsId for beregning ${beregning.beregningId} med ${beregning.beregningsperioder.size} perioder",
                        )

                        beregning.beregningsperioder
                            .filter { it.id != null } // Må vite hvilken vi skal oppdater
                            .filter { it.avdodeForeldre.isNullOrEmpty() } // Ta vekk allerede satt
                            .filter { it.regelResultat != null } // Ikke noe å sette fra
                            .forEach { periode ->
                                periode.regelResultat?.let { json ->
                                    // Les med readValue - gir en nested kotlin map
                                    val subsumsjon: Map<String, Any?> = objectMapper.readValue(json.toJson())

                                    logger.info("Skal prosessere subsumsjon for beregningsperiode ${periode.id}")

                                    val avdoedeForeldre =
                                        processSubsumsjonsNode(
                                            subsumsjon["resultat"] as Map<String, Any?>,
                                        ).toSet().toList()

                                    if (avdoedeForeldre.isNotEmpty()) {
                                        logger.info("Fant ${avdoedeForeldre.size} avdøde for beregningsperiode ${periode.id}")
                                        oppdaterBeregningsPeriodeMedAvdoedeForeldre(periode.id!!, avdoedeForeldre)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}
