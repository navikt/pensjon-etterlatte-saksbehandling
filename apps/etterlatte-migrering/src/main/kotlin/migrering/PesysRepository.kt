package no.nav.etterlatte.migrering

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.migrering.Migreringsstatus.BREVUTSENDING_OK
import no.nav.etterlatte.migrering.Migreringsstatus.FERDIG
import no.nav.etterlatte.migrering.Migreringsstatus.UTBETALING_OK
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import java.util.UUID
import javax.sql.DataSource

data class Pesyskopling(
    val pesysId: PesysId,
    val behandlingId: UUID,
    val sakId: SakId,
)

internal class PesysRepository(
    private val dataSource: DataSource,
) : Transactions<PesysRepository> {
    override fun <R> inTransaction(block: PesysRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun lagreManuellMigrering(
        pesysId: Long,
        tx: TransactionalSession? = null,
    ) = tx.session {
        opprett(
            "INSERT INTO pesyssak(id,sak,status) VALUES(:id,:sak::jsonb,:status)" +
                " ON CONFLICT(id) DO UPDATE SET sak=excluded.sak, status=excluded.status",
            mapOf("id" to pesysId, "status" to Migreringsstatus.UNDER_MIGRERING_MANUELT.name, "sak" to "{}".toJson()),
            "Lagra pesyssak $pesysId i migreringsbasen manuelt",
        )
    }

    fun oppdaterStatus(
        id: PesysId,
        nyStatus: Migreringsstatus,
        tx: TransactionalSession? = null,
    ) = tx.session {
        val gammelStatus = hentStatus(id.id, tx)

        val oppdatertStatus =
            when (gammelStatus) {
                UTBETALING_OK -> {
                    if (nyStatus == BREVUTSENDING_OK) {
                        FERDIG
                    } else {
                        nyStatus
                    }
                }

                BREVUTSENDING_OK -> {
                    if (nyStatus == UTBETALING_OK) {
                        FERDIG
                    } else {
                        nyStatus
                    }
                }

                else -> nyStatus
            }

        oppdater(
            "UPDATE pesyssak SET status=:status WHERE id=:id",
            mapOf("id" to id.id, "status" to oppdatertStatus.name),
            "Markerte $id med status $oppdatertStatus",
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
        sakId: SakId,
        tx: TransactionalSession? = null,
    ) {
        tx.session {
            opprett(
                "INSERT INTO pesyskopling(behandling_id,pesys_id,sak_id) VALUES(:behandling_id,:pesys_id,:sak_id)" +
                    " ON CONFLICT(pesys_id) DO UPDATE SET behandling_id = :behandling_id, sak_id = :sak_id",
                mapOf("behandling_id" to behandlingId, "pesys_id" to pesysId.id, "sak_id" to sakId.sakId),
                "Lagra koplinga mellom sak $sakId, behandling $behandlingId og pesyssak $pesysId i migreringsbasen",
            )
        }
    }

    fun hentPesysId(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ) = tx.session {
        hent(
            "SELECT pesys_id, behandling_id, sak_id from pesyskopling WHERE behandling_id = :behandling_id",
            mapOf("behandling_id" to behandlingId),
        ) {
            Pesyskopling(
                PesysId(it.long("pesys_id")),
                it.uuid("behandling_id"),
                SakId(it.long("sak_id")),
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
            "SELECT ${Pesyskoplingtabell.BEHANDLING_ID}, ${Pesyskoplingtabell.SAK_ID}" +
                " FROM ${Pesyskoplingtabell.TABELLNAVN} " +
                "WHERE ${Pesyskoplingtabell.PESYS_ID} = :${Pesyskoplingtabell.PESYS_ID}",
            mapOf(Pesyskoplingtabell.PESYS_ID to pesysId.id),
        ) {
            Pesyskopling(
                pesysId,
                it.uuid(Pesyskoplingtabell.BEHANDLING_ID),
                SakId(it.long(Pesyskoplingtabell.SAK_ID)),
            )
        }
    }
}

private object Pesyskoplingtabell {
    const val TABELLNAVN = "pesyskopling"
    const val PESYS_ID = "pesys_id"
    const val BEHANDLING_ID = "behandling_id"
    const val SAK_ID = "sak_id"
}

private object Feilkjoering {
    const val ID = "id"
    const val PESYS_ID = "pesys_id"
    const val REQUEST = "request"
    const val FEILMELDING = "feilmelding"
    const val FEILENDE_STEG = "feilendeSteg"
}
