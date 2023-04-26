package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.oppgave.domain.Oppgave
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

enum class Rolle {
    SAKSBEHANDLER, ATTESTANT, STRENGT_FORTROLIG
}

class OppgaveDao(private val connection: () -> Connection) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(statuser: List<BehandlingStatus>): List<Oppgave> {
        with(connection()) {
            val stmt = prepareStatement(
                """
                |SELECT b.id, b.sak_id, soeknad_mottatt_dato, fnr, sakType, enhet, status, behandling_opprettet,
                |behandlingstype, soesken, b.prosesstype, adressebeskyttelse, merknad
                |FROM behandling b INNER JOIN sak s ON b.sak_id = s.id 
                |WHERE ((adressebeskyttelse = ?) OR (adressebeskyttelse = ?)) 
                |AND status = ANY(?) AND (b.prosesstype is NULL OR b.prosesstype != ?)
                """.trimMargin()
            )
            stmt.setString(1, AdressebeskyttelseGradering.STRENGT_FORTROLIG.toString())
            stmt.setString(2, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.toString())
            stmt.setArray(3, createArrayOf("text", statuser.toTypedArray()))
            stmt.setString(4, Prosesstype.AUTOMATISK.toString())
            return stmt.executeQuery().toList(tilOppgave)
        }
    }

    fun finnOppgaverMedStatuser(statuser: List<BehandlingStatus>): List<Oppgave> {
        if (statuser.isEmpty()) return emptyList()

        with(connection()) {
            val stmt = prepareStatement(
                """
                |SELECT b.id, b.sak_id, soeknad_mottatt_dato, fnr, sakType, enhet, status, behandling_opprettet,
                |behandlingstype, soesken, b.prosesstype, adressebeskyttelse, merknad
                |FROM behandling b INNER JOIN sak s ON b.sak_id = s.id 
                |WHERE status = ANY(?) AND (b.prosesstype is NULL OR b.prosesstype != ?)
                |AND adressebeskyttelse is null OR 
                |(adressebeskyttelse is NOT NULL AND (adressebeskyttelse != ? AND adressebeskyttelse != ?))
                """.trimMargin()
            )
            stmt.setArray(1, createArrayOf("text", statuser.toTypedArray()))
            stmt.setString(2, Prosesstype.AUTOMATISK.toString())
            stmt.setString(3, AdressebeskyttelseGradering.STRENGT_FORTROLIG.toString())
            stmt.setString(4, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.toString())
            return stmt.executeQuery().toList(tilOppgave).also {
                logger.info(
                    "Hentet behandlingsoppgaveliste for bruker med statuser $statuser. Fant ${it.size} oppgaver"
                )
            }
        }
    }

    fun finnOppgaverFraGrunnlagsendringshendelser(): List<Oppgave> {
        with(connection()) {
            val stmt = prepareStatement(
                """
                SELECT g.sak_id, g.type, g.behandling_id, g.opprettet, s.fnr, s.sakType, s.enhet, g.hendelse_gjelder_rolle, g.samsvar_mellom_pdl_og_grunnlag
                FROM grunnlagsendringshendelse g 
                INNER JOIN sak s ON g.sak_id = s.id
                WHERE status = ?
                """.trimIndent()
            )
            stmt.setString(1, GrunnlagsendringStatus.SJEKKET_AV_JOBB.name)
            return stmt.executeQuery().toList {
                val registrertDato = getTidspunkt("opprettet")
                Oppgave.Grunnlagsendringsoppgave(
                    sak = this.mapSak(),
                    registrertDato = registrertDato,
                    grunnlagsendringsType = GrunnlagsendringsType.valueOf(getString("type")),
                    gjelderRolle = Saksrolle.enumVedNavnEllerUkjent(getString("hendelse_gjelder_rolle")),
                    samsvarMellomKildeOgGrunnlag = objectMapper.readValue(
                        getString("samsvar_mellom_pdl_og_grunnlag")
                    )
                )
            }.also {
                logger.info("Hentet grunnlagsoppgaveliste for saksbehandler. Fant ${it.size} oppgaver")
            }
        }
    }

    private fun ResultSet.mapSak() = Sak(
        id = this.getLong("sak_id"),
        sakType = enumValueOf(this.getString("sakType")),
        ident = this.getString("fnr"),
        enhet = this.getString("enhet").takeUnless { this.wasNull() }
    )

    private val tilOppgave: (ResultSet) -> Oppgave = { rs ->
        val mottattDato = rs.getTidspunktOrNull("soeknad_mottatt_dato")
            ?: rs.getTidspunktOrNull("behandling_opprettet")
            ?: throw IllegalStateException(
                "Vi har en behandling som hverken har soeknad mottatt dato eller behandling opprettet dato "
            )

        Oppgave.BehandlingOppgave(
            sak = rs.mapSak(),
            behandlingId = rs.getObject("id") as UUID,
            behandlingStatus = BehandlingStatus.valueOf(rs.getString("status")),
            registrertDato = mottattDato,
            behandlingsType = BehandlingType.valueOf(rs.getString("behandlingstype")),
            merknad = rs.getString("merknad")
        )
    }
}