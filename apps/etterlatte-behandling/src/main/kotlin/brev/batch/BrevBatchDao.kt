package no.nav.etterlatte.brev.batch

import no.nav.etterlatte.brev.BrevBatchOversikt
import no.nav.etterlatte.brev.BrevBatchRequest
import no.nav.etterlatte.brev.BrevBatchSakResultat
import no.nav.etterlatte.brev.BrevBatchSakStatus
import no.nav.etterlatte.brev.BrevBatchSaker
import no.nav.etterlatte.brev.BrevBatchStatus
import no.nav.etterlatte.brev.BrevBatchSteg
import no.nav.etterlatte.brev.StrukturertInformasjonsbrevRequest
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

data class BrevBatchArbeid(
    val resultat: BrevBatchSakResultat,
    val request: BrevBatchRequest,
    val snapshot: StrukturertInformasjonsbrevRequest?,
    val eier: UUID,
)

class BrevBatchDao(
    private val dataSource: DataSource,
) {
    fun opprett(
        innsender: String,
        noekkel: UUID,
        request: BrevBatchRequest,
        fingeravtrykk: String,
    ): UUID =
        transaksjon { connection ->
            val id = UUID.randomUUID()
            val antall =
                connection
                    .prepareStatement(
                        """
                        INSERT INTO brev_batch(id, innsender, idempotensnoekkel, fingeravtrykk, request)
                        VALUES (?, ?, ?, ?, ?) ON CONFLICT (innsender, idempotensnoekkel) DO NOTHING
                        """.trimIndent(),
                    ).use {
                        it.setObject(1, id)
                        it.setString(2, innsender)
                        it.setObject(3, noekkel)
                        it.setString(4, fingeravtrykk)
                        it.setString(5, request.toJson())
                        it.executeUpdate()
                    }
            if (antall == 0) {
                connection
                    .prepareStatement(
                        "SELECT id, fingeravtrykk FROM brev_batch WHERE innsender = ? AND idempotensnoekkel = ?",
                    ).use {
                        it.setString(1, innsender)
                        it.setObject(2, noekkel)
                        it.executeQuery().use { rs ->
                            check(rs.next())
                            if (rs.getString("fingeravtrykk") != fingeravtrykk) {
                                throw ForespoerselException(409, "BREV_BATCH_KONFLIKT", "Nøkkelen er allerede brukt med annet innhold")
                            }
                            rs.getObject("id", UUID::class.java)
                        }
                    }
            } else {
                connection
                    .prepareStatement(
                        "INSERT INTO brev_batch_sak(utsendings_id, batch_id, sak_id) VALUES (?, ?, ?)",
                    ).use { stmt ->
                        request.sakIder.forEach {
                            stmt.setObject(1, UUID.randomUUID())
                            stmt.setObject(2, id)
                            stmt.setLong(3, it.sakId)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                id
            }
        }

    fun hent(
        batchId: UUID,
        innsender: String,
    ): BrevBatchOversikt? =
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT b.opprettet, COUNT(*) AS antall,
                      COUNT(*) FILTER (WHERE s.status = 'FERDIG') AS ferdige,
                      COUNT(*) FILTER (WHERE s.status = 'FEILA') AS feila,
                      COUNT(*) FILTER (WHERE s.status = 'AVVENTER_AVKLARING') AS avklaring,
                      COUNT(*) FILTER (WHERE s.status IN ('KLAR', 'UNDER_ARBEID')) AS ventende
                    FROM brev_batch b JOIN brev_batch_sak s ON s.batch_id = b.id
                    WHERE b.id = ? AND b.innsender = ?
                    GROUP BY b.id
                    """.trimIndent(),
                ).use {
                    it.setObject(1, batchId)
                    it.setString(2, innsender)
                    it.executeQuery().use { rs ->
                        if (!rs.next()) {
                            null
                        } else {
                            val ventende = rs.getInt("ventende")
                            val avklaring = rs.getInt("avklaring")
                            val feila = rs.getInt("feila")
                            BrevBatchOversikt(
                                batchId,
                                when {
                                    ventende > 0 -> BrevBatchStatus.UNDER_ARBEID
                                    avklaring > 0 -> BrevBatchStatus.AVVENTER_AVKLARING
                                    feila > 0 -> BrevBatchStatus.FERDIG_MED_FEIL
                                    else -> BrevBatchStatus.FERDIG
                                },
                                rs.getInt("antall"),
                                rs.getInt("ferdige"),
                                feila,
                                avklaring,
                                ventende,
                                rs.getTimestamp("opprettet").toInstant(),
                            )
                        }
                    }
                }
        }

    fun hentSaker(
        batchId: UUID,
        innsender: String,
        offset: Int,
        limit: Int,
    ): BrevBatchSaker? {
        val oversikt = hent(batchId, innsender) ?: return null
        val saker =
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT s.* FROM brev_batch_sak s JOIN brev_batch b ON s.batch_id = b.id
                        WHERE b.id = ? AND b.innsender = ? ORDER BY s.sak_id OFFSET ? LIMIT ?
                        """.trimIndent(),
                    ).use {
                        it.setObject(1, batchId)
                        it.setString(2, innsender)
                        it.setInt(3, offset)
                        it.setInt(4, limit)
                        it.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.resultat()) } }
                    }
            }
        return BrevBatchSaker(saker, offset, limit, oversikt.antallSaker)
    }

    fun reserver(): BrevBatchArbeid? =
        dataSource.connection.use { connection ->
            val eier = UUID.randomUUID()
            connection
                .prepareStatement(
                    """
                    WITH kandidat AS (
                      SELECT utsendings_id FROM brev_batch_sak
                      WHERE (status = 'KLAR' AND neste_forsoek <= clock_timestamp())
                        OR (status = 'UNDER_ARBEID' AND laas_til < clock_timestamp())
                      ORDER BY neste_forsoek, utsendings_id
                      LIMIT 1 FOR UPDATE SKIP LOCKED
                    ), reservert AS (
                      UPDATE brev_batch_sak s SET status = 'UNDER_ARBEID', laas_eier = ?,
                        laas_til = clock_timestamp() + INTERVAL '5 minutes', antall_forsoek = antall_forsoek + 1
                      FROM kandidat k WHERE s.utsendings_id = k.utsendings_id RETURNING s.*
                    )
                    SELECT r.*, b.request FROM reservert r JOIN brev_batch b ON b.id = r.batch_id
                    """.trimIndent(),
                ).use {
                    it.setObject(1, eier)
                    it.executeQuery().use { rs ->
                        if (!rs.next()) {
                            null
                        } else {
                            BrevBatchArbeid(
                                rs.resultat(),
                                deserialize(rs.getString("request")),
                                rs.getString("snapshot")?.let { json -> deserialize<StrukturertInformasjonsbrevRequest>(json) },
                                eier,
                            )
                        }
                    }
                }
        }

    fun oppdater(
        arbeid: BrevBatchArbeid,
        status: BrevBatchSakStatus,
        steg: BrevBatchSteg,
        brevId: Long? = arbeid.resultat.brevId,
        feilkode: String? = null,
        snapshot: StrukturertInformasjonsbrevRequest? = null,
        nesteForsoek: Instant = Instant.now(),
    ): Boolean =
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                    UPDATE brev_batch_sak SET status = ?, steg = ?, brev_id = COALESCE(?, brev_id),
                      feilkode = ?, snapshot = COALESCE(snapshot, ?), neste_forsoek = ?,
                      laas_eier = CASE WHEN ? = 'UNDER_ARBEID' THEN laas_eier ELSE NULL END,
                      laas_til = CASE WHEN ? = 'UNDER_ARBEID' THEN laas_til ELSE NULL END
                    WHERE utsendings_id = ? AND laas_eier = ? AND laas_til > clock_timestamp()
                      AND status = 'UNDER_ARBEID'
                    """.trimIndent(),
                ).use {
                    it.setString(1, status.name)
                    it.setString(2, steg.name)
                    it.setObject(3, brevId)
                    it.setString(4, feilkode)
                    it.setString(5, snapshot?.toJson())
                    it.setTimestamp(6, java.sql.Timestamp.from(nesteForsoek))
                    it.setString(7, status.name)
                    it.setString(8, status.name)
                    it.setObject(9, arbeid.resultat.utsendingsId)
                    it.setObject(10, arbeid.eier)
                    it.executeUpdate() == 1
                }
        }

    private fun ResultSet.resultat() =
        BrevBatchSakResultat(
            getObject("utsendings_id", UUID::class.java),
            SakId(getLong("sak_id")),
            BrevBatchSakStatus.valueOf(getString("status")),
            BrevBatchSteg.valueOf(getString("steg")),
            getObject("brev_id")?.let { getLong("brev_id") },
            getInt("antall_forsoek"),
            getString("feilkode"),
        )

    private fun <T> transaksjon(block: (Connection) -> T): T =
        dataSource.connection.use {
            it.autoCommit = false
            try {
                block(it).also { _ -> it.commit() }
            } catch (e: Throwable) {
                it.rollback()
                throw e
            }
        }
}
