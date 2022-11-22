package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
import no.nav.etterlatte.libs.common.behandling.KorrektIPDL
import no.nav.etterlatte.libs.common.behandling.Saksrolle
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
                INSERT INTO grunnlagsendringshendelse(id, sak_id, type, opprettet, data, status, hendelse_gjelder_rolle, korrekt_i_pdl)
                VALUES(?, ?, ?, ?, to_json(?::json), ?, ?, ?)
                """.trimIndent()
            )
            with(hendelse) {
                stmt.setObject(1, id)
                stmt.setLong(2, sakId)
                stmt.setString(3, type.name)
                stmt.setTimestamp(4, Timestamp.from(opprettet.atZone(ZoneId.systemDefault()).toInstant()))
                hendelse.data?.let { stmt.setString(5, objectMapper.writeValueAsString(hendelse.data)) }
                stmt.setString(6, status.name)
                stmt.setString(7, hendelseGjelderRolle.name)
                stmt.setString(8, korrektIPDL.name)
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
                    SELECT id, sak_id, type, opprettet, data, status, behandling_id, hendelse_gjelder_rolle, korrekt_i_pdl
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
                    SELECT id, sak_id, type, opprettet, data, status, behandling_id, hendelse_gjelder_rolle, korrekt_i_pdl
                    FROM grunnlagsendringshendelse
                """.trimIndent()
            )
            return stmt.executeQuery().toList { asGrunnlagsendringshendelse() }
        }
    }

    fun oppdaterGrunnlagsendringStatus(
        hendelseId: UUID,
        foerStatus: GrunnlagsendringStatus,
        etterStatus: GrunnlagsendringStatus,
        korrektIPDL: KorrektIPDL
    ) {
        with(connection()) {
            prepareStatement(
                """
                   UPDATE grunnlagsendringshendelse
                   SET status = ?,
                   korrekt_i_pdl = ?
                   WHERE id = ?
                   AND status = ?
                """.trimIndent()
            ).use {
                it.setString(1, etterStatus.name)
                it.setString(2, korrektIPDL.name)
                it.setObject(3, hendelseId)
                it.setString(4, foerStatus.name)
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
        minutter: Long
    ): List<Grunnlagsendringshendelse> {
        with(connection()) {
            prepareStatement(
                """
                   SELECT id, sak_id, type, opprettet, data, status, behandling_id, hendelse_gjelder_rolle, korrekt_i_pdl
                   FROM grunnlagsendringshendelse
                   WHERE opprettet <= ?
                   AND status = ?
                """.trimIndent()
            ).use {
                it.setTimestamp(1, Timestamp.from(Instant.now().minus(minutter, ChronoUnit.MINUTES)))
                it.setString(2, GrunnlagsendringStatus.VENTER_PAA_JOBB.name)
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
                    SELECT id, sak_id, type, opprettet, data, status, behandling_id, hendelse_gjelder_rolle, korrekt_i_pdl
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
        listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB)
    )

    private fun ResultSet.asGrunnlagsendringshendelse(): Grunnlagsendringshendelse {
        return when (val type = GrunnlagsendringsType.valueOf(getString("type"))) {
            GrunnlagsendringsType.DOEDSFALL -> {
                Grunnlagsendringshendelse(
                    getObject("id") as UUID,
                    getLong("sak_id"),
                    type,
                    getTimestamp("opprettet").toLocalDateTime(),
                    objectMapper.readValue(getString("data"), Grunnlagsinformasjon.Doedsfall::class.java),
                    GrunnlagsendringStatus.valueOf(getString("status")),
                    getObject("behandling_id")?.let { it as UUID },
                    Saksrolle.valueOf(getString("hendelse_gjelder_rolle")),
                    KorrektIPDL.valueOf(getString("korrekt_i_pdl"))
                )
            }
            GrunnlagsendringsType.UTFLYTTING -> {
                Grunnlagsendringshendelse(
                    getObject("id") as UUID,
                    getLong("sak_id"),
                    type,
                    getTimestamp("opprettet").toLocalDateTime(),
                    objectMapper.readValue(getString("data"), Grunnlagsinformasjon.Utflytting::class.java),
                    GrunnlagsendringStatus.valueOf(getString("status")),
                    getObject("behandling_id")?.let { it as UUID },
                    Saksrolle.valueOf(getString("hendelse_gjelder_rolle")),
                    KorrektIPDL.valueOf(getString("korrekt_i_pdl"))
                )
            }
            GrunnlagsendringsType.FORELDER_BARN_RELASJON -> {
                Grunnlagsendringshendelse(
                    getObject("id") as UUID,
                    getLong("sak_id"),
                    type,
                    getTimestamp("opprettet").toLocalDateTime(),
                    objectMapper.readValue(getString("data"), Grunnlagsinformasjon.ForelderBarnRelasjon::class.java),
                    GrunnlagsendringStatus.valueOf(getString("status")),
                    getObject("behandling_id")?.let { it as UUID },
                    Saksrolle.valueOf(getString("hendelse_gjelder_rolle")),
                    KorrektIPDL.valueOf(getString("korrekt_i_pdl"))
                )
            }
        }
    }
}