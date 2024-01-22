package no.nav.etterlatte.grunnlag.omregning

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.transaction
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

class OmregningDao(private val datasource: DataSource) : Transactions<OmregningDao> {
    override fun <R> inTransaction(block: OmregningDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession? = null,
    ): List<String> {
        val aktuellMaaned = maaned.atDay(1).format(formatter).let { Integer.parseInt(it) }
        val nesteMaaned = maaned.plusMonths(1).atDay(1).format(formatter).let { Integer.parseInt(it) }
        val sql =
            """
            select g.sak_id as sak_id from grunnlagshendelse g
                     inner join grunnlagshendelse g2 on g.sak_id = g2.sak_id and g.fnr = g2.fnr and g.hendelsenummer != g2.hendelsenummer
            where g.opplysning_type = 'FOEDSELSDATO'
            and g2.opplysning_type = 'SOEKER_PDL_V1'
            and replace(replace(g.opplysning, '"', ''), '-', '') :: INTEGER >= :aktuellMaaned
            and replace(replace(g.opplysning, '"', ''), '-', '') :: INTEGER < :nesteMaaned 
            """.trimIndent()

        return tx.session {
            hentListe(sql, { mapOf("aktuellMaaned" to aktuellMaaned, "nesteMaaned" to nesteMaaned) }) {
                it.string("sak_id")
            }
        }
    }
}
