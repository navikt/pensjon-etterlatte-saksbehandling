package no.nav.etterlatte.migrering

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.transaction
import java.util.*
import javax.sql.DataSource

internal class PesysRepository(private val dataSource: DataSource) : Transactions<PesysRepository> {

    override fun <R> inTransaction(block: PesysRepository.(TransactionalSession) -> R): R {
        return dataSource.transaction {
            this.block(it)
        }
    }

    fun hentSaker(tx: TransactionalSession? = null): List<Pesyssak> = tx.session {
        hentListe(
            "SELECT sak from pesyssak WHERE migrert is false"
        ) {
            tilPesyssak(it.string("sak"))
        }
    }

    private fun tilPesyssak(sak: String) = objectMapper.readValue(sak, Pesyssak::class.java)

    fun lagrePesyssak(pesyssak: Pesyssak, tx: TransactionalSession? = null) =
        tx.session {
            opprett(
                "INSERT INTO pesyssak(id,sak,migrert) VALUES(:id,:sak::jsonb,:migrert::boolean)",
                mapOf("id" to UUID.randomUUID(), "sak" to pesyssak.toJson(), "migrert" to false),
                "Lagra pesyssak ${pesyssak.pesysId} i migreringsbasen"
            )
        }

    fun settSakMigrert(id: UUID, tx: TransactionalSession? = null) = tx.session {
        oppdater(
            "UPDATE pesyssak SET migrert=:migrert WHERE id=:id",
            mapOf("id" to id, "migrert" to true),
            "Markerte $id som migrert"
        )
    }
}