package no.nav.etterlatte.tidshendelser

import kotliquery.queryOf
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.tidshendelser.JobbType
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

class JobbTestdata(
    private val dataSource: DataSource,
    private val hendelseDao: HendelseDao,
) {
    fun opprettJobb(
        type: JobbType,
        behandlingsmaaned: YearMonth,
        kjoeredato: LocalDate = LocalDate.now(),
    ): HendelserJobb {
        val id =
            dataSource.transaction(true) {
                it.run(
                    queryOf(
                        """
                        INSERT INTO jobb (type, kjoeredato, behandlingsmaaned)
                        VALUES (?, ?, ?)
                        """.trimIndent(),
                        type.name,
                        kjoeredato,
                        behandlingsmaaned.toString(),
                    ).asUpdateAndReturnGeneratedKey,
                )
            }

        return hendelseDao.hentJobb(id!!.toInt())
    }

    fun slettAlleJobber() {
        dataSource.transaction { it.run(queryOf("DELETE FROM jobb").asExecute) }
    }
}
