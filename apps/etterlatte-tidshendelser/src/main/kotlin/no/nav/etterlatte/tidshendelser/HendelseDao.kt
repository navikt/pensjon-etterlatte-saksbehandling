package no.nav.etterlatte.tidshendelser

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import org.slf4j.LoggerFactory
import java.time.YearMonth
import javax.sql.DataSource

class HendelseDao(private val datasource: DataSource) : Transactions<HendelseDao> {
    private val logger = LoggerFactory.getLogger(HendelseDao::class.java)

    override fun <R> inTransaction(block: HendelseDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    fun hentJobb(id: Int): HendelserJobb {
        return datasource.transaction { tx ->
            queryOf("SELECT * FROM jobb WHERE id = :id", mapOf("id" to id))
                .let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asSingle) }
                ?: throw NoSuchElementException("Fant ikke jobb med id $id")
        }
    }

    fun finnAktuellJobb(): List<HendelserJobb> {
        return datasource.transaction { tx ->
            queryOf(
                """
                SELECT * FROM jobb 
                WHERE status = :status
                AND kjoeredato = CURRENT_DATE
                ORDER BY id asc
                LIMIT 1
                """.trimIndent(),
                mapOf("status" to JobbStatus.NY.name),
            )
                .let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asList) }
        }
    }

    fun hentJobber(jobbIDs: List<Int>): List<HendelserJobb> {
        return datasource.transaction { tx ->
            queryOf(
                """
                SELECT * FROM jobb WHERE id = ANY(?)
                """.trimIndent(),
                jobbIDs.toTypedArray(),
            )
                .let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asList) }
        }
    }

    fun oppdaterJobbstatusStartet(hendelserJobb: HendelserJobb) {
        oppdaterJobbstatus(hendelserJobb, JobbStatus.STARTET)
    }

    fun oppdaterJobbstatusFerdig(hendelserJobb: HendelserJobb) {
        oppdaterJobbstatus(hendelserJobb, JobbStatus.FERDIG)
    }

    private fun oppdaterJobbstatus(
        hendelserJobb: HendelserJobb,
        status: JobbStatus,
    ) {
        datasource.transaction {
            queryOf(
                """
                UPDATE jobb 
                SET status = :status, 
                    endret = now(),
                    versjon = versjon + 1
                WHERE id = :id
                AND versjon = :versjon
                """.trimIndent(),
                mapOf(
                    "status" to status.name,
                    "id" to hendelserJobb.id,
                    "versjon" to hendelserJobb.versjon,
                ),
            )
                .let { query -> it.run(query.asUpdate) }
                .also {
                    if (it == 0) {
                        throw ConcurrentModificationException(
                            "Kunne ikke oppdatere jobb [id=${hendelserJobb.id}, versjon=${hendelserJobb.versjon}]",
                        )
                    }
                }
        }
    }

    fun opprettHendelserForSaker(
        jobbId: Int,
        saksIDer: List<Long>,
        steg: Steg,
    ) {
        val values =
            saksIDer.map { sakId ->
                mapOf(
                    "jobbId" to jobbId,
                    "sakId" to sakId,
                    "steg" to steg.name,
                )
            }

        datasource.transaction { tx ->
            val result =
                tx.batchPreparedNamedStatement(
                    """
                    INSERT INTO hendelse (jobb_id, sak_id, steg)
                    VALUES (:jobbId, :sakId, :steg)
                    """.trimIndent(),
                    values,
                )
            logger.info("Opprettet ${result.size} hendelser for jobb $jobbId")
        }
    }

    fun hentHendelserForJobb(jobbId: Int): List<Hendelse> {
        return datasource.transaction {
            queryOf(
                """
                SELECT * FROM hendelse WHERE jobb_id = :jobbId
                """.trimIndent(),
                mapOf("jobbId" to jobbId),
            )
                .let { query -> it.run(query.map { row -> row.toHendelse() }.asList) }
                .sortedBy { it.sakId }
        }
    }

    fun oppdaterHendelseStatus(
        hendelse: Hendelse,
        status: HendelseStatus,
    ) {
        datasource.transaction {
            queryOf(
                """
                UPDATE hendelse 
                SET status = :status, 
                    endret = now(),
                    versjon = versjon + 1
                WHERE id = :id
                """.trimIndent(),
                mapOf("status" to status.name, "id" to hendelse.id),
            )
                .let { query -> it.run(query.asUpdate) }
        }
    }

    fun oppdaterHendelseForSteg(
        hendelseId: String,
        steg: String,
        info: Any? = null,
    ) {
        datasource.transaction {
            queryOf(
                """
                UPDATE hendelse 
                SET steg = :steg,
                    endret = now(),
                    versjon = versjon + 1
                WHERE id = :id
                AND   versjon = :versjon
                """.trimIndent(),
                mapOf(
                    "id" to hendelseId,
                    "steg" to steg,
                ),
            )
                .let { query -> it.run(query.asUpdate) }
        }
    }

    fun pollHendelser(limit: Int = 5): List<Hendelse> {
        return datasource.transaction {
            queryOf(
                """
                SELECT * FROM hendelse 
                WHERE status = :status 
                ORDER BY opprettet asc
                LIMIT $limit
                """.trimIndent(),
                mapOf("status" to "NY"),
            )
                .let { query -> it.run(query.map { row -> row.toHendelse() }.asList) }
        }
    }

    private fun Row.toHendelse() =
        Hendelse(
            id = uuid("id"),
            jobbId = int("jobb_id"),
            sakId = int("sak_id"),
            opprettet = tidspunkt("opprettet").toLocalDatetimeUTC(),
            endret = tidspunkt("endret").toLocalDatetimeUTC(),
            versjon = int("versjon"),
            status = HendelseStatus.valueOf(string("status")),
            steg = string("steg"),
            info = anyOrNull("info"),
        )

    private fun Row.toHendelserJobb() =
        HendelserJobb(
            id = int("id"),
            opprettet = tidspunkt("opprettet").toLocalDatetimeUTC(),
            endret = tidspunkt("endret").toLocalDatetimeUTC(),
            versjon = int("versjon"),
            type = JobbType.valueOf(string("type")),
            kjoeredato = localDate("kjoeredato"),
            behandlingsmaaned = YearMonth.parse(string("behandlingsmaaned")),
            dryrun = boolean("dryrun"),
            status = JobbStatus.valueOf(string("status")),
        )
}
