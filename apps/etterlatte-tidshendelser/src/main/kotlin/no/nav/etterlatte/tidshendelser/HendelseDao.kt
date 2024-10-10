package no.nav.etterlatte.tidshendelser

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class HendelseDao(
    private val datasource: DataSource,
) : Transactions<HendelseDao> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun <R> inTransaction(block: HendelseDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    fun hentJobb(id: Int): HendelserJobb =
        datasource.transaction { tx ->
            queryOf("SELECT * FROM jobb WHERE id = :id", mapOf("id" to id))
                .let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asSingle) }
                ?: throw NoSuchElementException("Fant ikke jobb med id $id")
        }

    fun finnAktuellJobb(): List<HendelserJobb> =
        datasource.transaction { tx ->
            queryOf(
                """
                SELECT * FROM jobb 
                WHERE status = :status
                AND kjoeredato = CURRENT_DATE
                ORDER BY id asc
                LIMIT 1
                """.trimIndent(),
                mapOf("status" to JobbStatus.NY.name),
            ).let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asList) }
        }

    fun hentJobber(jobbIDs: List<Int>): List<HendelserJobb> =
        datasource.transaction { tx ->
            queryOf(
                """
                SELECT * FROM jobb WHERE id = ANY(?)
                """.trimIndent(),
                jobbIDs.toTypedArray(),
            ).let { query -> tx.run(query.map { row -> row.toHendelserJobb() }.asList) }
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
            ).let { query -> it.run(query.asUpdate) }
                .also {
                    if (it == 0) {
                        throw ConcurrentModificationException(
                            "Kunne ikke oppdatere jobb [id=${hendelserJobb.id}, versjon=${hendelserJobb.versjon}]",
                        )
                    }
                }
        }
    }

    fun ferdigstillJobbHvisAlleHendelserErFerdige(hendelseId: UUID) {
        datasource.transaction {
            queryOf(
                """
                UPDATE jobb j
                SET status  = 'FERDIG',
                    endret  = CURRENT_TIMESTAMP,
                    versjon = j.versjon + 1
                WHERE j.id = (SELECT jobb_id FROM hendelse h WHERE h.id = :hendelseId)
                AND NOT EXISTS (SELECT 1
                      FROM hendelse h
                      WHERE h.jobb_id = j.id
                        AND h.status <> 'FERDIG'
                )
                """.trimIndent(),
                mapOf(
                    "hendelseId" to hendelseId,
                ),
            ).let { query -> it.run(query.asUpdate) }
        }
    }

    fun opprettHendelserForSaker(
        jobbId: Int,
        saksIDer: List<SakId>,
        steg: Steg,
    ) {
        val values =
            saksIDer.map { sakId ->
                mapOf(
                    "jobbId" to jobbId,
                    "sakId" to sakId.sakId,
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

    fun hentHendelserForJobb(jobbId: Int): List<Hendelse> =
        datasource.transaction {
            queryOf(
                """
                SELECT * FROM hendelse WHERE jobb_id = :jobbId
                """.trimIndent(),
                mapOf("jobbId" to jobbId),
            ).let { query -> it.run(query.map { row -> row.toHendelse() }.asList) }
                .sortedBy { it.sakId }
        }

    fun oppdaterHendelseStatus(
        hendelseId: UUID,
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
                mapOf("status" to status.name, "id" to hendelseId),
            ).let { query -> it.run(query.asUpdate) }
        }
    }

    fun oppdaterHendelseForSteg(
        hendelseId: UUID,
        steg: String,
        info: String,
    ) {
        datasource.transaction {
            queryOf(
                """
                UPDATE hendelse 
                SET steg = :steg,
                    info = COALESCE(info, '[]'::JSONB) || :ny_info::JSONB,
                    endret = now(),
                    versjon = versjon + 1
                WHERE id = :id
                """.trimIndent(),
                mapOf(
                    "id" to hendelseId,
                    "ny_info" to info,
                    "steg" to steg,
                ),
            ).let { query -> it.run(query.asUpdate) }
        }
    }

    fun tilbakestillJobSomIkkeStartetSkikkelig(jobbId: Int) {
        datasource.transaction {
            val identifiserteOppgaver =
                queryOf(
                    """
                    SELECT count(*)
                    FROM hendelse 
                    WHERE jobb_id = :jobbId
                    """.trimIndent(),
                    mapOf("jobbId" to jobbId),
                ).let { query -> it.run(query.map { it.int(1) }.asSingle) }

            if (identifiserteOppgaver != 0) {
                throw InternfeilException(
                    "Kan ikke tilbakestille jobb med id=$jobbId, siden det er $identifiserteOppgaver " +
                        "som er laget for jobben allerede",
                )
            }

            val antallOppdaterteRader =
                queryOf(
                    """
                    UPDATE jobb 
                    SET status = :statusNy
                    WHERE id = :id
                    """.trimIndent(),
                    mapOf("statusNy" to JobbStatus.NY.name, "id" to jobbId),
                ).let { query -> it.run(query.asUpdate) }

            if (antallOppdaterteRader != 1) {
                throw InternfeilException(
                    "Prøvde å tilbakestille statusen til jobb med id=$jobbId, men oppdaterte i " +
                        "stedet $antallOppdaterteRader.",
                )
            }
        }
    }

    fun settHarLoependeYtelse(
        hendelseId: UUID,
        loependeYtelse: Boolean,
    ) {
        datasource.transaction {
            queryOf(
                """
                UPDATE hendelse 
                SET loepende_ytelse = :loependeYtelse,
                    endret = now(),
                    versjon = versjon + 1
                WHERE id = :id
                """.trimIndent(),
                mapOf(
                    "id" to hendelseId,
                    "loependeYtelse" to loependeYtelse,
                ),
            ).let { query -> it.run(query.asUpdate) }
        }
    }

    fun pollHendelser(limit: Int = 5): List<Hendelse> =
        datasource.transaction {
            queryOf(
                """
                SELECT * FROM hendelse 
                WHERE (
                    status = 'NY' OR (
                        status = 'FEILET' AND versjon < 3
                    )
                )
                ORDER BY opprettet asc
                LIMIT $limit
                """.trimIndent(),
            ).let { query -> it.run(query.map { row -> row.toHendelse() }.asList) }
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
            loependeYtelse = boolean("loepende_ytelse"),
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
