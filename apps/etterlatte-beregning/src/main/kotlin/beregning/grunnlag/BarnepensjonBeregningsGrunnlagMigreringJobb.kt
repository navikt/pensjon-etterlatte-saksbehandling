package no.nav.etterlatte.beregning.grunnlag

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentSoeskenjustering
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.concurrent.thread

private enum class JobFeatureToggle(private val key: String) : FeatureToggle {
    JobToggle("pensjon-etterlatte.barnepensjon-beregningsgrunnlag-for-beregning-fra-beregning");

    override fun key() = key
}

class BarnepensjonBeregningsGrunnlagMigreringJobb(
    private val leaderElection: LeaderElection,
    private val dataSource: DataSource,
    private val featureToggleService: FeatureToggleService
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val config: Config = ConfigFactory.load()

    private val klient = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("grunnlag.azure.scope")
    )

    private val resourceUrl = config.getString("grunnlag.resource.url")

    fun schedule() {
        thread {
            // Don't start until pod is up and running/initial delay
            TimeUnit.SECONDS.sleep(120)
            run()
        }
    }

    private fun run() {
        if (!featureToggleService.isEnabled(JobFeatureToggle.JobToggle, false)) {
            return
        }

        val correlationId = UUID.randomUUID().toString()

        if (leaderElection.isLeader()) {
            withLogContext(correlationId) { log.info("Starter jobb ${this::class.simpleName}") }

            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->

                    if (trengsMigrering(tx)) {
                        withLogContext(correlationId) {
                            log.info("Migrering trengs")

                            hentAlleBeregninger(tx).forEach { runBlocking { process(correlationId, it, tx) } }

                            log.info("Migrering ferdig")
                        }
                    }
                }
            }
        }
    }

    private fun hentAlleBeregninger(tx: TransactionalSession) = tx.run(
        queryOf(
            """
                SELECT behandlingid, max(grunnlagVersjon) AS grunnlagVersjon, sakId
                FROM beregningsperiode
                GROUP BY (behandlingid, sakid)
            """.trimIndent()
        ).map {
            BeregningForMigrering(
                it.uuid("behandlingId"),
                it.long("grunnlagVersjon"),
                it.long("sakId")
            )
        }.asList
    )

    private fun trengsMigrering(tx: TransactionalSession) = (
        tx.run(
            queryOf("SELECT COUNT(*) AS migratedCount FROM bp_beregningsgrunnlag").map {
                it.long("migratedCount")
            }.asSingle
        ) ?: 0
        ) == 0L

    private suspend fun process(correlationId: String, beregning: BeregningForMigrering, tx: TransactionalSession) {
        log.info("Migrering av beregning $beregning")

        val response = klient.get(
            "$resourceUrl/api/grunnlag/${beregning.sakId}/versjon/${beregning.grunnlagVersjon}"
        ) {
            header(X_CORRELATION_ID, correlationId)
        }

        if (response.status.isSuccess()) {
            val grunnlag = response.body<Grunnlag>().mapSoeskenJustering()

            grunnlag?.let {
                tx.run(
                    queryOf(
                        statement = lagreGrunnlagQuery,
                        paramMap = mapOf<String, Any>(
                            "behandlings_id" to beregning.behandlingId,
                            "soesken_med_i_beregning" to objectMapper.writeValueAsString(
                                it.verdi.beregningsgrunnlag
                            ),
                            "kilde" to (it.kilde as Grunnlagsopplysning.Saksbehandler).toJson()
                        )
                    ).asUpdate
                )
            }
        } else {
            log.error("Migrering av beregning $beregning feilet med ${response.status} ${response.body<String>()}")

            throw ResponseException(response, "Ukjent feil fra grunnlag api")
        }
    }

    companion object {
        val lagreGrunnlagQuery = """
            INSERT INTO bp_beregningsgrunnlag(behandlings_id, soesken_med_i_beregning, kilde)
            VALUES(
                :behandlings_id,
                :soesken_med_i_beregning,
                :kilde
            )
        """.trimMargin()
    }
}

private data class BeregningForMigrering(
    val behandlingId: UUID,
    val grunnlagVersjon: Long,
    val sakId: Long
)

fun Grunnlag.mapSoeskenJustering() = with(this.sak) {
    this.hentSoeskenjustering()
}