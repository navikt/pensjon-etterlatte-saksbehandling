package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.checkInternFeil
import no.nav.etterlatte.libs.common.sak.DisttribuertEllerIverksatt
import no.nav.etterlatte.libs.common.sak.KjoeringDistEllerIverksattRequest
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.toList

class OmregningDao(
    private val connection: ConnectionAutoclosing,
) {
    fun hentSakerTilOmregning(
        kjoering: String,
        antall: Int,
        ekskluderteSaker: List<SakId>,
    ): List<Pair<SakId, KjoeringStatus>> =
        connection.hentConnection { connection ->
            with(connection) {
                val harEkskluderteSaker = ekskluderteSaker.isNotEmpty()

                val statement =
                    prepareStatement(
                        """
                        WITH siste_kjoeringer AS (
                            SELECT sak_id, MAX(tidspunkt) as max_tid
                            FROM omregningskjoering
                            GROUP BY sak_id
                        )
                        SELECT o.sak_id, o.status  
                        FROM omregningskjoering o
                        JOIN siste_kjoeringer s
                        ON o.sak_id = s.sak_id
                        WHERE kjoering = ? AND status IN ('KLAR', 'FEILA') AND s.max_tid = o.tidspunkt
                        ${if (harEkskluderteSaker) "AND o.sak_id <> ALL (?)" else ""}
                        LIMIT ?
                        """.trimIndent(),
                    )

                statement.setString(1, kjoering)
                if (harEkskluderteSaker) {
                    statement.setArray(2, createArrayOf("bigint", ekskluderteSaker.map { it.sakId }.toTypedArray()))
                }
                statement.setInt(if (harEkskluderteSaker) 3 else 2, antall)

                statement.executeQuery().toList {
                    Pair(
                        SakId(getLong("sak_id")),
                        KjoeringStatus.valueOf(getString("status")),
                    )
                }
            }
        }

    fun oppdaterKjoering(request: KjoeringRequest) {
        connection.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO omregningskjoering (kjoering, status, sak_id, begrunnelse, corr_id, feilende_steg)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setString(1, request.kjoering)
                statement.setString(2, request.status.name)
                statement.setSakId(3, request.sakId)
                statement.setString(4, request.begrunnelse)
                statement.setString(5, request.corrId)
                statement.setString(6, request.feilendeSteg)
                statement.executeUpdate().also {
                    checkInternFeil(it > 0) {
                        "Kunne ikke oppdaterKjoering for id sakid ${request.sakId}"
                    }
                }
            }
        }
    }

    fun lagreKjoering(request: LagreKjoeringRequest) {
        connection.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO omregningskjoering (kjoering, status, sak_id, beregning_beloep_foer, 
                        beregning_beloep_etter, beregning_g_foer, beregning_g_etter, 
                        beregning_brukt_omregningsfaktor, avkorting_foer, avkorting_etter, vedtak_beloep)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setString(1, request.kjoering)
                statement.setString(2, request.status.name)
                statement.setSakId(3, request.sakId)
                statement.setBigDecimal(4, request.beregningBeloepFoer)
                statement.setBigDecimal(5, request.beregningBeloepEtter)
                statement.setBigDecimal(6, request.beregningGFoer)
                statement.setBigDecimal(7, request.beregningGEtter)
                statement.setBigDecimal(8, request.beregningBruktOmregningsfaktor)
                statement.setBigDecimal(9, request.avkortingFoer)
                statement.setBigDecimal(10, request.avkortingEtter)
                statement.setBigDecimal(11, request.vedtakBeloep)
                statement.executeUpdate().also {
                    checkInternFeil(it > 0) {
                        "Kunne ikke lagreKjoering for id sakid ${request.sakId}"
                    }
                }
            }
        }
    }

    fun lagreDistribuertBrevEllerIverksattBehandlinga(
        request: KjoeringDistEllerIverksattRequest,
        status: KjoeringStatus,
    ) {
        connection.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO omregningskjoering (
                        kjoering, sak_id, status,
                        ${
                            when (request.distEllerIverksatt) {
                                DisttribuertEllerIverksatt.IVERKSATT -> "iverkksatt_behandling"
                                DisttribuertEllerIverksatt.DISTRIBUERT -> "distribuert_brev"
                            }
                        }
                        )
                        VALUES (?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setString(1, request.kjoering)
                statement.setSakId(2, request.sakId)
                statement.setString(3, status.name)
                statement.setBoolean(4, true)
                statement.executeUpdate().also {
                    checkInternFeil(it > 0) {
                        "Kunne ikke lagreKjoering for id sakid ${request.sakId}"
                    }
                }
            }
        }
    }

    fun hentNyligsteLinjeForKjoering(
        kjoering: String,
        sakId: SakId,
    ): Pair<Long, KjoeringStatus> =
        connection.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        SELECT kjoering, status, sak_id  
                        FROM omregningskjoering 
                        WHERE kjoering = ? AND sak_id = ?
                        ORDER by tidspunkt DESC 
                        LIMIT 1
                        """.trimIndent(),
                    )
                statement.setString(1, kjoering)
                statement.setLong(2, sakId.sakId)
                statement.executeQuery().single {
                    Pair(
                        getLong("sak_id"),
                        KjoeringStatus.valueOf(getString("status")),
                    )
                }
            }
        }
}
