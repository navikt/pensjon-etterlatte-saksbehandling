package no.nav.etterlatte.migrering

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import java.util.*
import javax.sql.DataSource

data class Pesyskopling(
    val pesysId: PesysId,
    val behandlingId: UUID
)

internal class PesysRepository(private val dataSource: DataSource) : Transactions<PesysRepository> {

    override fun <R> inTransaction(block: PesysRepository.(TransactionalSession) -> R): R {
        return dataSource.transaction {
            this.block(it)
        }
    }

    fun lagrePesyssak(pesyssak: Pesyssak, tx: TransactionalSession? = null) =
        tx.session {
            opprett(
                "INSERT INTO pesyssak(id,sak,status) VALUES(:id,:sak::jsonb,:status)",
                mapOf("id" to pesyssak.id, "sak" to pesyssak.toJson(), "status" to Migreringsstatus.HENTA.name),
                "Lagra pesyssak ${pesyssak.id} i migreringsbasen"
            )
        }

    fun oppdaterStatus(id: PesysId, status: Migreringsstatus, tx: TransactionalSession? = null) = tx.session {
        oppdater(
            "UPDATE pesyssak SET status=:status WHERE id=:id",
            mapOf("id" to id.id, "status" to status.name),
            "Markerte $id med status $status"
        )
    }

    fun hentStatus(id: Long, tx: TransactionalSession? = null) = tx.session {
        hent(
            "SELECT status from pesyssak WHERE id = :id",
            mapOf("id" to id)
        ) {
            Migreringsstatus.valueOf(it.string("status"))
        }
    }

    fun lagreKoplingTilBehandling(behandlingId: UUID, pesysId: PesysId, tx: TransactionalSession? = null) {
        tx.session {
            opprett(
                "INSERT INTO pesyskopling(behandling_id,pesys_id) VALUES(:behandling_id,:pesys_id)",
                mapOf("behandling_id" to behandlingId, "pesys_id" to pesysId.id),
                "Lagra koplinga mellom behandling $behandlingId og pesyssak $pesysId i migreringsbasen"
            )
        }
    }

    fun hentPesysId(behandlingId: UUID, tx: TransactionalSession? = null) = tx.session {
        hent(
            "SELECT pesys_id, behandling_id from pesyskopling WHERE behandling_id = :behandling_id",
            mapOf("behandling_id" to behandlingId)
        ) {
            Pesyskopling(
                PesysId(it.long("pesys_id")),
                it.uuid("behandling_id")
            )
        }
    }
}