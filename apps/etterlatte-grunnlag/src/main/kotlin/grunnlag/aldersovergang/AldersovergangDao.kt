package no.nav.etterlatte.grunnlag.aldersovergang

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.transaction
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

class AldersovergangDao(
    private val datasource: DataSource,
) : Transactions<AldersovergangDao> {
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
            select distinct g.sak_id as sak_id from grunnlagshendelse g
                     inner join grunnlagshendelse g2 on g.sak_id = g2.sak_id and g.fnr = g2.fnr and g.hendelsenummer != g2.hendelsenummer
                     and g.hendelsenummer = (
                        select max(hendelsenummer) from grunnlagshendelse 
                        where sak_id = g.sak_id
                        and opplysning_type = 'FOEDSELSDATO' 
                        and fnr = g.fnr
                     )
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

    fun hentSakerHvorDoedsfallForekomIGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession? = null,
    ): List<String> {
        val sql =
            """
            SELECT DISTINCT g.sak_id
            FROM grunnlagshendelse g
            INNER JOIN grunnlagshendelse g2 
                ON g.sak_id = g2.sak_id 
                AND g.fnr = g2.fnr 
                AND g.hendelsenummer != g2.hendelsenummer
            WHERE g.hendelsenummer = (
                    SELECT MAX(hendelsenummer) 
                    FROM grunnlagshendelse
                    WHERE sak_id = g.sak_id
                    AND   opplysning_type = 'DOEDSDATO'
                    AND   fnr = g.fnr
            )
            AND g.opplysning_type = 'DOEDSDATO'
            AND g2.opplysning_type = 'AVDOED_PDL_V1'
            AND TO_DATE(g.opplysning, '\"YYYY-MM-DD\"') BETWEEN :start AND :slutt;
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

    fun hentFoedselsdato(
        sakId: SakId,
        opplysningType: Opplysningstype,
        tx: TransactionalSession? = null,
    ): LocalDate? {
        val sql =
            """
            select distinct TO_DATE(g.opplysning, '\"YYYY-MM-DD\"') as foedselsdato
             from grunnlagshendelse g
                     inner join grunnlagshendelse g2 on g.sak_id = g2.sak_id and g.fnr = g2.fnr and g.hendelsenummer != g2.hendelsenummer
                     and g.hendelsenummer = (
                        select max(hendelsenummer) from grunnlagshendelse 
                        where sak_id = g.sak_id
                        and opplysning_type = 'FOEDSELSDATO' 
                        and fnr = g.fnr
                     )
            where g.opplysning_type = 'FOEDSELSDATO'
            and g2.opplysning_type = :opplysningType
            and g.sak_id = :sak_id
            """.trimIndent()
        return tx.session {
            hent(sql, mapOf("sak_id" to sakId, "opplysningType" to opplysningType.name)) {
                it.localDate("foedselsdato")
            }
        }
    }
}
