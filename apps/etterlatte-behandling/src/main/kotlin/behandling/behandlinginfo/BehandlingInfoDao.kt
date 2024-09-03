package no.nav.etterlatte.behandling.behandlinginfo

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.SQLJsonb
import no.nav.etterlatte.libs.database.SQLObject
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.slett
import java.sql.ResultSet
import java.util.UUID

class BehandlingInfoDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreBrevutfall(brevutfall: Brevutfall): Brevutfall =
        connectionAutoclosing
            .opprett(
                """
                    INSERT INTO behandling_info(behandling_id, brevutfall)
                    VALUES (?, ?)
                    ON CONFLICT (behandling_id) DO 
                    UPDATE SET brevutfall = excluded.brevutfall
                    """,
                listOf(
                    SQLObject(brevutfall.behandlingId),
                    SQLJsonb(brevutfall),
                ),
                { require(it == 1) },
            ).let {
                hentBrevutfall(brevutfall.behandlingId)
                    ?: throw InternfeilException("Feilet under lagring av brevutfall")
            }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? =
        connectionAutoclosing
            .hent(
                """
                    SELECT behandling_id, brevutfall 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID
                    """,
                listOf(SQLObject(behandlingId)),
            ) {
                toBrevutfall()
            }.singleOrNull()

    fun lagreEtterbetaling(etterbetaling: Etterbetaling): Etterbetaling =
        connectionAutoclosing
            .opprett(
                """
                    INSERT INTO behandling_info(behandling_id, etterbetaling)
                    VALUES (?, ?)
                    ON CONFLICT (behandling_id) DO 
                    UPDATE SET etterbetaling = excluded.etterbetaling
                    """,
                listOf(SQLObject(etterbetaling.behandlingId), SQLJsonb(etterbetaling)),
                { require(it == 1) },
            ).let {
                hentEtterbetaling(etterbetaling.behandlingId)
                    ?: throw InternfeilException("Feilet under lagring av etterbetaling")
            }

    fun slettEtterbetaling(behandlingId: UUID): Int =
        connectionAutoclosing.slett(
            """
                    UPDATE behandling_info SET etterbetaling = ?
                    WHERE behandling_id = ?
                    """,
            listOf(SQLJsonb(null), SQLObject(behandlingId)),
            { require(it == 1) },
        )

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? =
        connectionAutoclosing
            .hent(
                """
                    SELECT behandling_id, etterbetaling 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID AND etterbetaling IS NOT NULL
                    """,
                listOf(SQLObject(behandlingId)),
            ) {
                toEtterbetaling()
            }.singleOrNull()

    private fun ResultSet.toBrevutfall(): Brevutfall = this.getString("brevutfall").let { objectMapper.readValue(it) }

    private fun ResultSet.toEtterbetaling(): Etterbetaling = this.getString("etterbetaling").let { objectMapper.readValue(it) }
}
