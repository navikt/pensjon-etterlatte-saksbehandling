package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class GrunnlagsendringshendelseDao(val connection: () -> Connection) {

    fun opprettGrunnlagsendringshendelse(hendelse: Grunnlagsendringshendelse): Grunnlagsendringshendelse {
        with(connection()) {
            val stmt = prepareStatement(
                """
                INSERT INTO grunnlagsendringshendelse(id, sak_id, type, opprettet, data, status)
                VALUES(?, ?, ?, ?, to_json(?::json), ?)
                """.trimIndent()
            )
            with(hendelse) {
                stmt.setObject(1, id)
                stmt.setLong(2, sakId)
                stmt.setString(3, type.name)
                stmt.setTimestamp(4, Timestamp.from(opprettet.atZone(ZoneId.systemDefault()).toInstant()))
                hendelse.data?.let { stmt.setString(5, objectMapper.writeValueAsString(hendelse.data)) }
                stmt.setString(6, status.name)
            }
            stmt.executeUpdate()
        }.let {
            return hentGrunnlagsendringshendelse(hendelse.id)
                ?: throw Exception("Kunne ikke hente nettopp lagret Grunnlagsendringshendelse med id: ${hendelse.id}")
        }
    }

    fun hentGrunnlagsendringshendelse(id: UUID): Grunnlagsendringshendelse? {
        with(connection()) {
            val stmt = prepareStatement(
                """
                    SELECT id, sak_id, type, opprettet, data, status, behandling_id
                    FROM grunnlagsendringshendelse
                    WHERE id = ?
                """.trimIndent()
            )
            stmt.setObject(1, id)
            return stmt.executeQuery().singleOrNull { asGrunnlagsendringshendelse() }
        }
    }

    fun hentAlleGrunnlagsendringshendelser(): List<Grunnlagsendringshendelse> {
        with(connection()) {
            val stmt = prepareStatement(
                """
                    SELECT id, sak_id, type, opprettet, data, status, behandling_id
                    FROM grunnlagsendringshendelse
                """.trimIndent()
            )
            return stmt.executeQuery().toList { asGrunnlagsendringshendelse() }
        }
    }

    fun oppdaterGrunnlagsendringStatusForType(
        saker: List<Long>,
        foerStatus: GrunnlagsendringStatus,
        etterStatus: GrunnlagsendringStatus,
        type: GrunnlagsendringsType
    ) {
        with(connection()) {
            prepareStatement(
                """
                   UPDATE grunnlagsendringshendelse
                   SET status = ?
                   WHERE sak_id = ANY(?)
                   AND status = ?
                   AND type = ?
                """.trimIndent()
            ).use {
                it.setString(1, etterStatus.name)
                it.setArray(2, createArrayOf("bigint", saker.toTypedArray()))
                it.setString(3, foerStatus.name)
                it.setString(4, type.name)
                it.executeUpdate()
            }
        }
    }

    fun settBehandlingIdForTattMedIBehandling(
        sak: Long,
        behandlingId: UUID,
        type: GrunnlagsendringsType
    ) {
        with(connection()) {
            prepareStatement(
                """
                   UPDATE grunnlagsendringshendelse
                   SET behandling_id = ?
                   WHERE sak_id = ?
                   AND status = ?
                   AND type = ?
                """.trimIndent()
            ).use {
                it.setObject(1, behandlingId)
                it.setLong(2, sak)
                it.setString(3, GrunnlagsendringStatus.TATT_MED_I_BEHANDLING.name)
                it.setString(4, type.name)
                it.executeUpdate()
            }
        }
    }

    fun hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
        minutter: Long,
        type: GrunnlagsendringsType
    ): List<Grunnlagsendringshendelse> {
        with(connection()) {
            prepareStatement(
                """
                   SELECT id, sak_id, type, opprettet, data, status, behandling_id
                   FROM grunnlagsendringshendelse
                   WHERE opprettet <= ?
                   AND status = ?
                   AND type = ?
                """.trimIndent()
            ).use {
                it.setTimestamp(1, Timestamp.from(Instant.now().minus(minutter, ChronoUnit.MINUTES)))
                it.setString(2, GrunnlagsendringStatus.IKKE_VURDERT.name)
                it.setString(3, type.name)
                return it.executeQuery().toList { asGrunnlagsendringshendelse() }
            }
        }
    }

    fun hentGrunnlagsendringshendelserMedStatuserISak(
        sakId: Long,
        statuser: List<GrunnlagsendringStatus>
    ): List<Grunnlagsendringshendelse> {
        with(connection()) {
            prepareStatement(
                """
                    SELECT id, sak_id, type, opprettet, data, status, behandling_id
                    FROM grunnlagsendringshendelse
                    WHERE sak_id = ?
                    AND status = ANY(?)
                """.trimIndent()
            ).use {
                it.setLong(1, sakId)
                val statusArray = this.createArrayOf("text", statuser.toTypedArray())
                it.setArray(2, statusArray)
                return it.executeQuery().toList { asGrunnlagsendringshendelse() }
            }
        }
    }

    fun hentGyldigeGrunnlagsendringshendelserISak(
        sakId: Long
    ): List<Grunnlagsendringshendelse> = hentGrunnlagsendringshendelserMedStatuserISak(
        sakId,
        listOf(GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING)
    )

    private fun ResultSet.asGrunnlagsendringshendelse(): Grunnlagsendringshendelse {
        return when (val type = GrunnlagsendringsType.valueOf(getString("type"))) {
            GrunnlagsendringsType.SOEKER_DOED -> {
                Grunnlagsendringshendelse(
                    getObject("id") as UUID,
                    getLong("sak_id"),
                    type,
                    getTimestamp("opprettet").toLocalDateTime(),
                    objectMapper.readValue(getString("data"), Grunnlagsinformasjon.SoekerDoed::class.java),
                    GrunnlagsendringStatus.valueOf(getString("status")),
                    getObject("behandling_id")?.let { it as UUID }
                )
            }
        }
    }
}