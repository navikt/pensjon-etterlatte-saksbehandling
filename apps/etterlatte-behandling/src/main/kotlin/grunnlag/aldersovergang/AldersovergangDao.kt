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

interface IAldersovergangDao {
    fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession? = null,
    ): List<String>

    fun hentSakerHvorDoedsfallForekomIGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession? = null,
    ): List<String>

    fun hentFoedselsdato(
        sakId: SakId,
        opplysningType: Opplysningstype,
        tx: TransactionalSession? = null,
    ): LocalDate?
}

class AldersovergangDao(
    private val datasource: DataSource,
) : IAldersovergangDao,
    Transactions<AldersovergangDao> {
    override fun <R> inTransaction(block: AldersovergangDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    override fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession?,
    ): List<String> {
        val sql =
            """
            WITH siste_foedselsdato AS (
                SELECT DISTINCT ON (sak_id, fnr) sak_id, fnr, opplysning
                FROM grunnlagshendelse
                WHERE opplysning_type = 'FOEDSELSDATO'
                ORDER BY sak_id, fnr, hendelsenummer DESC
            )
            SELECT DISTINCT f.sak_id
            FROM siste_foedselsdato f
            WHERE EXISTS (
                SELECT 1 FROM grunnlagshendelse g2
                WHERE g2.sak_id = f.sak_id AND g2.fnr = f.fnr AND g2.opplysning_type = 'SOEKER_PDL_V1'
            )
            AND TO_DATE(f.opplysning, '\"YYYY-MM-DD\"') BETWEEN :start AND :slutt
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

    override fun hentSakerHvorDoedsfallForekomIGittMaaned(
        maaned: YearMonth,
        tx: TransactionalSession?,
    ): List<String> {
        val sql =
            """
            WITH siste_doedsdato AS (
                SELECT DISTINCT ON (sak_id, fnr) sak_id, fnr, opplysning
                FROM grunnlagshendelse
                WHERE opplysning_type = 'DOEDSDATO'
                ORDER BY sak_id, fnr, hendelsenummer DESC
            )
            SELECT DISTINCT f.sak_id
            FROM siste_doedsdato f
            WHERE EXISTS (
                SELECT 1 FROM grunnlagshendelse g2
                WHERE g2.sak_id = f.sak_id AND g2.fnr = f.fnr AND g2.opplysning_type = 'AVDOED_PDL_V1'
            )
            AND TO_DATE(f.opplysning, '\"YYYY-MM-DD\"') BETWEEN :start AND :slutt
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

    override fun hentFoedselsdato(
        sakId: SakId,
        opplysningType: Opplysningstype,
        tx: TransactionalSession?,
    ): LocalDate? {
        val sql =
            """
            WITH siste_foedselsdato AS (
                SELECT DISTINCT ON (sak_id, fnr) sak_id, fnr, opplysning
                FROM grunnlagshendelse
                WHERE opplysning_type = 'FOEDSELSDATO'
                AND sak_id = :sak_id
                ORDER BY sak_id, fnr, hendelsenummer DESC
            )
            SELECT TO_DATE(f.opplysning, '\"YYYY-MM-DD\"') AS foedselsdato
            FROM siste_foedselsdato f
            WHERE EXISTS (
                SELECT 1 FROM grunnlagshendelse g2
                WHERE g2.sak_id = f.sak_id AND g2.fnr = f.fnr AND g2.opplysning_type = :opplysningType
            )
            """.trimIndent()
        return tx.session {
            hent(sql, mapOf("sak_id" to sakId.sakId, "opplysningType" to opplysningType.name)) {
                it.localDate("foedselsdato")
            }
        }
    }
}
