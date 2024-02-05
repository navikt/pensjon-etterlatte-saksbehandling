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

    fun hentJobb(id: Long): HendelserJobb? {
        return datasource.transaction { tx ->
            queryOf("SELECT * FROM jobb WHERE id = :id", mapOf("id" to id))
                .let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asSingle) }
        }
    }

    fun opprettHendelserForSaker(
        jobbId: Int,
        saksIDer: List<Long>,
    ) {
        val values =
            saksIDer.map { sakId ->
                mapOf(
                    "jobbId" to jobbId,
                    "sakId" to sakId,
                )
            }

        datasource.transaction { tx ->
            val result =
                tx.batchPreparedNamedStatement(
                    """
                    INSERT INTO hendelse (jobb_id, sak_id)
                    VALUES (:jobbId, :sakId)
                    """.trimIndent(),
                    values,
                )
            logger.info("Opprettet ${result.size} hendelser for jobb $jobbId")
        }
    }

    fun hentHendelserForJobb(jobbId: Int): List<Hendelse> {
        return datasource.transaction {
            queryOf("SELECT * FROM hendelse WHERE jobb_id = :jobbId", mapOf("jobbId" to jobbId))
                .let { query -> it.run(query.map { row -> row.toHendelse() }.asList) }
                .sortedBy { it.sakId }
        }
    }

    fun oppdaterHendelse(hendelse: Hendelse) {
        // Oppdater i db
    }

    private fun Row.toHendelse() =
        Hendelse(
            id = uuid("id"),
            jobbId = int("jobb_id"),
            sakId = int("sak_id"),
            opprettet = tidspunkt("opprettet").toLocalDatetimeUTC(),
            endret = tidspunkt("endret").toLocalDatetimeUTC(),
            versjon = int("versjon"),
            status = string("status"),
            utfall = stringOrNull("utfall"),
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
            status = string("status"),
        )
}
