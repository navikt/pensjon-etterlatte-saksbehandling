package no.nav.etterlatte.grunnlagsendring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.SamsvarMellomPdlOgGrunnlag
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.temporal.ChronoUnit
import java.util.*

class GrunnlagsendringshendelseDao(val connection: () -> Connection) {

    fun opprettGrunnlagsendringshendelse(hendelse: Grunnlagsendringshendelse): Grunnlagsendringshendelse {
        with(connection()) {
            val stmt = prepareStatement(
                """
                INSERT INTO grunnlagsendringshendelse(id, sak_id, type, opprettet, status, hendelse_gjelder_rolle, 
                    samsvar_mellom_pdl_og_grunnlag, gjelder_person)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
            with(hendelse) {
                stmt.setObject(1, id)
                stmt.setLong(2, sakId)
                stmt.setString(3, type.name)
                stmt.setTidspunkt(4, opprettet.toTidspunkt())
                stmt.setString(5, status.name)
                stmt.setString(6, hendelseGjelderRolle.name)
                stmt.setJsonb(7, samsvarMellomPdlOgGrunnlag)
                stmt.setString(8, gjelderPerson)
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
                    SELECT id, sak_id, type, opprettet, status, behandling_id, hendelse_gjelder_rolle, 
                        samsvar_mellom_pdl_og_grunnlag, gjelder_person 
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
                    SELECT id, sak_id, type, opprettet, status, behandling_id, hendelse_gjelder_rolle, 
                        samsvar_mellom_pdl_og_grunnlag, gjelder_person
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
        samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag
    ) {
        with(connection()) {
            prepareStatement(
                """
                   UPDATE grunnlagsendringshendelse
                   SET status = ?,
                   samsvar_mellom_pdl_og_grunnlag = ?
                   WHERE id = ?
                   AND status = ?
                """.trimIndent()
            ).use {
                it.setString(1, etterStatus.name)
                it.setJsonb(2, samsvarMellomPdlOgGrunnlag)
                it.setObject(3, hendelseId)
                it.setString(4, foerStatus.name)
                it.executeUpdate()
            }
        }
    }

    fun settBehandlingIdForTattMedIBehandling(
        grlaghendelseId: UUID,
        behandlingId: UUID
    ) {
        with(connection()) {
            prepareStatement(
                """
                   UPDATE grunnlagsendringshendelse
                   SET behandling_id = ?
                   WHERE id = ?
                   AND status = ?
                """.trimIndent()
            ).use {
                it.setObject(1, behandlingId)
                it.setObject(2, grlaghendelseId)
                it.setString(3, GrunnlagsendringStatus.TATT_MED_I_BEHANDLING.name)
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
                   SELECT id, sak_id, type, opprettet, status, behandling_id, hendelse_gjelder_rolle, 
                       samsvar_mellom_pdl_og_grunnlag, gjelder_person
                   FROM grunnlagsendringshendelse
                   WHERE opprettet <= ?
                   AND status = ?
                """.trimIndent()
            ).use {
                it.setTidspunkt(1, Tidspunkt.now().minus(minutter, ChronoUnit.MINUTES))
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
                    SELECT id, sak_id, type, opprettet, status, behandling_id, hendelse_gjelder_rolle, 
                        samsvar_mellom_pdl_og_grunnlag, gjelder_person
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

    fun hentGrunnlagsendringshendelserSomErSjekketAvJobb(
        sakId: Long
    ): List<Grunnlagsendringshendelse> = hentGrunnlagsendringshendelserMedStatuserISak(
        sakId,
        listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB)
    )

    private fun ResultSet.asGrunnlagsendringshendelse(): Grunnlagsendringshendelse {
        return Grunnlagsendringshendelse(
            id = getObject("id") as UUID,
            sakId = getLong("sak_id"),
            type = GrunnlagsendringsType.valueOf(getString("type")),
            opprettet = getTidspunkt("opprettet").toLocalDatetimeUTC(),
            status = GrunnlagsendringStatus.valueOf(getString("status")),
            behandlingId = getObject("behandling_id")?.let { it as UUID },
            hendelseGjelderRolle = Saksrolle.valueOf(getString("hendelse_gjelder_rolle")),
            gjelderPerson = getString("gjelder_person"),
            samsvarMellomPdlOgGrunnlag = objectMapper.readValue(
                getString("samsvar_mellom_pdl_og_grunnlag")
            )
        )
    }
}

inline fun <reified T> PreparedStatement.setJsonb(parameterIndex: Int, jsonb: T): PreparedStatement {
    val jsonObject = PGobject()
    jsonObject.type = "json"
    jsonObject.value = objectMapper.writeValueAsString(jsonb)
    this.setObject(parameterIndex, jsonObject)
    return this
}