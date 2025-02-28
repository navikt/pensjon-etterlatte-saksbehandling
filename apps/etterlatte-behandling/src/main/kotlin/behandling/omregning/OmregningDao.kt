package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.DisttribuertEllerIverksatt
import no.nav.etterlatte.libs.common.sak.KjoeringDistEllerIverksattRequest
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull

class OmregningDao(
    private val connection: ConnectionAutoclosing,
) {
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
                    krev(it > 0) {
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
                    krev(it > 0) {
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
                    krev(it > 0) {
                        "Kunne ikke lagreKjoering for id sakid ${request.sakId}"
                    }
                }
            }
        }
    }

    fun hentNyligsteLinjeForKjoering(
        kjoering: String,
        sakId: SakId,
    ): Pair<Long, KjoeringStatus>? =
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
                statement.executeQuery().singleOrNull {
                    Pair(
                        getLong("sak_id"),
                        KjoeringStatus.valueOf(getString("status")),
                    )
                }
            }
        }
}
