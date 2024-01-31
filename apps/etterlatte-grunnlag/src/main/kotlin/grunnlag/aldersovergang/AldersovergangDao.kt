package no.nav.etterlatte.grunnlag.aldersovergang

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.transaction
import java.time.YearMonth
import javax.sql.DataSource

class AldersovergangDao(private val datasource: DataSource) : Transactions<AldersovergangDao> {
    override fun <R> inTransaction(block: AldersovergangDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession? = null,
    ): List<String> {
        val sql =
            """
            select g.sak_id as sak_id from grunnlagshendelse g
                     inner join grunnlagshendelse g2 on g.sak_id = g2.sak_id and g.fnr = g2.fnr and g.hendelsenummer != g2.hendelsenummer
            where g.opplysning_type = 'FOEDSELSDATO'
            and g2.opplysning_type = 'SOEKER_PDL_V1'
            and TO_DATE(g.opplysning, '\"YYYY-MM-DD\"') BETWEEN :start AND :slutt
            """.trimIndent()

        return tx.session {
            hentListe(
                sql,
                {
                    mapOf(
                        "start" to maaned.atDay(1),
                        "slutt" to maaned.atEndOfMonth(),
                    )
                },
            ) {
                it.string("sak_id")
            }
        }
    }
}
