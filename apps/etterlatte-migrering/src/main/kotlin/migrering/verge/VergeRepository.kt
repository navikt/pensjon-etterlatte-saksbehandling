package no.nav.etterlatte.migrering.verge

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import javax.sql.DataSource

internal class VergeRepository(private val dataSource: DataSource) : Transactions<VergeRepository> {
    override fun <R> inTransaction(block: VergeRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun lagreVergeadresse(
        pensjonFullmakt: String,
        tx: TransactionalSession? = null,
        sak: String,
        pesysId: PesysId,
        pdl: String,
    ) = tx.session {
        opprett(
            """
            INSERT INTO vergeadresse(
            pesys_id,
            sak,
            pensjon_fullmakt,
            pdl
            )
            VALUES (
            :pesys_id,
            :sak::jsonb,
            :pensjon_fullmakt::jsonb,
            :pdl::jsonb
            )
            """.trimIndent(),
            mapOf(
                "pesys_id" to pesysId.id,
                "sak" to sak,
                "pensjon_fullmakt" to pensjonFullmakt,
                "pdl" to pdl,
            ),
            "",
        )
    }
}
