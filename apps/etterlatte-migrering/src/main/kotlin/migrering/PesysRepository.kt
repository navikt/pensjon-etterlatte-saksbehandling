package no.nav.etterlatte.migrering

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

data class Pesyskopling(
    val pesysId: PesysId,
    val behandlingId: UUID,
)

internal class PesysRepository(private val dataSource: DataSource) : Transactions<PesysRepository> {
    override fun <R> inTransaction(block: PesysRepository.(TransactionalSession) -> R): R {
        return dataSource.transaction {
            this.block(it)
        }
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagrePesyssak(
        pesyssak: Pesyssak,
        tx: TransactionalSession? = null,
    ) = tx.session {
        opprett(
            "INSERT INTO pesyssak(id,sak,status) VALUES(:id,:sak::jsonb,:status)" +
                " ON CONFLICT(id) DO UPDATE SET sak=excluded.sak, status=excluded.status",
            mapOf("id" to pesyssak.id, "sak" to pesyssak.toJson(), "status" to Migreringsstatus.HENTA.name),
            "Lagra pesyssak ${pesyssak.id} i migreringsbasen",
        )
    }

    fun oppdaterStatus(
        id: PesysId,
        status: Migreringsstatus,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            "UPDATE pesyssak SET status=:status WHERE id=:id",
            mapOf("id" to id.id, "status" to status.name),
            "Markerte $id med status $status",
        )
    }

    fun hentStatus(
        id: Long,
        tx: TransactionalSession? = null,
    ) = tx.session {
        hent(
            "SELECT status from pesyssak WHERE id = :id",
            mapOf("id" to id),
        ) {
            Migreringsstatus.valueOf(it.string("status"))
        }
    }

    fun lagreKoplingTilBehandling(
        behandlingId: UUID,
        pesysId: PesysId,
        tx: TransactionalSession? = null,
    ) {
        if (hentKoplingTilBehandling(pesysId) == null) {
            tx.session {
                opprett(
                    "INSERT INTO pesyskopling(behandling_id,pesys_id) VALUES(:behandling_id,:pesys_id)" +
                        " ON CONFLICT(behandling_id,pesys_id) DO NOTHING",
                    mapOf("behandling_id" to behandlingId, "pesys_id" to pesysId.id),
                    "Lagra koplinga mellom behandling $behandlingId og pesyssak $pesysId i migreringsbasen",
                )
            }
        } else {
            logger.info("Fins allerede kopling i databasen mellom $behandlingId og $pesysId. Prøver ikke å opprette ny")
        }
    }

    fun hentPesysId(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ) = tx.session {
        hent(
            "SELECT pesys_id, behandling_id from pesyskopling WHERE behandling_id = :behandling_id",
            mapOf("behandling_id" to behandlingId),
        ) {
            Pesyskopling(
                PesysId(it.long("pesys_id")),
                it.uuid("behandling_id"),
            )
        }
    }

    fun lagreFeilkjoering(
        request: String,
        tx: TransactionalSession? = null,
        feilendeSteg: String,
        feil: String,
        pesysId: PesysId,
    ) = tx.session {
        opprett(
            """
                INSERT INTO feilkjoering(
                    ${Feilkjoering.ID}, 
                    ${Feilkjoering.PESYS_ID}, 
                    ${Feilkjoering.REQUEST}, 
                    ${Feilkjoering.FEILMELDING},
                    ${Feilkjoering.FEILENDE_STEG}
                )
                VALUES(
                    :${Feilkjoering.ID},
                    :${Feilkjoering.PESYS_ID},
                    :${Feilkjoering.REQUEST}::jsonb,
                    :${Feilkjoering.FEILMELDING}::jsonb,
                    :${Feilkjoering.FEILENDE_STEG}
                )""",
            mapOf(
                Feilkjoering.ID to UUID.randomUUID(),
                Feilkjoering.PESYS_ID to pesysId.id,
                Feilkjoering.REQUEST to request.toJson(),
                Feilkjoering.FEILMELDING to feil,
                Feilkjoering.FEILENDE_STEG to feilendeSteg,
            ),
            "Lagra feilkjøringsdata for $pesysId",
        )
    }

    fun hentKoplingTilBehandling(
        pesysId: PesysId,
        tx: TransactionalSession? = null,
    ) = tx.session {
        hent(
            "SELECT ${Pesyskoplingtabell.BEHANDLING_ID} FROM ${Pesyskoplingtabell.TABELLNAVN} " +
                "WHERE ${Pesyskoplingtabell.PESYS_ID} = :${Pesyskoplingtabell.PESYS_ID}",
            mapOf(Pesyskoplingtabell.PESYS_ID to pesysId.id),
        ) {
            Pesyskopling(pesysId, it.uuid(Pesyskoplingtabell.BEHANDLING_ID))
        }
    }
}

private object Pesyskoplingtabell {
    const val TABELLNAVN = "pesyskopling"
    const val PESYS_ID = "pesys_id"
    const val BEHANDLING_ID = "behandling_id"
}

private object Feilkjoering {
    const val ID = "id"
    const val PESYS_ID = "pesys_id"
    const val REQUEST = "request"
    const val FEILMELDING = "feilmelding"
    const val FEILENDE_STEG = "feilendeSteg"
}
