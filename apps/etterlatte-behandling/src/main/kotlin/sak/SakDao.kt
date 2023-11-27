package no.nav.etterlatte.sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet

data class SakMedUtenlandstilknytning(
    val ident: String,
    val sakType: SakType,
    val id: Long,
    val enhet: String,
    val utlandstilknytning: Utlandstilknytning?,
) {
    companion object {
        fun fra(
            sak: Sak,
            utlandstilknytning: Utlandstilknytning?,
        ) = SakMedUtenlandstilknytning(
            ident = sak.ident,
            sakType = sak.sakType,
            id = sak.id,
            enhet = sak.enhet,
            utlandstilknytning = utlandstilknytning,
        )
    }
}

class SakDao(private val connection: () -> Connection) {
    fun hentUtenlandstilknytningForSak(sakId: Long): SakMedUtenlandstilknytning? {
        with(connection()) {
            val statement =
                prepareStatement(
                    "SELECT * from sak where id = ?",
                )
            statement.setLong(1, sakId)
            return statement.executeQuery().singleOrNull {
                SakMedUtenlandstilknytning(
                    sakType = enumValueOf(getString("sakType")),
                    ident = getString("fnr"),
                    id = getLong("id"),
                    enhet = getString("enhet"),
                    utlandstilknytning = getString("utenlandstilknytning")?.let { objectMapper.readValue(it) },
                )
            }
        }
    }

    fun oppdaterUtenlandstilknytning(
        sakId: Long,
        utenlandstilknytning: Utlandstilknytning,
    ) {
        with(connection()) {
            val statement =
                prepareStatement(
                    "UPDATE sak set utenlandstilknytning = ? where id = ?",
                )
            statement.setJsonb(1, utenlandstilknytning)
            statement.setLong(2, sakId)
            statement.executeUpdate()
        }
    }

    fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    ) {
        with(connection()) {
            val statement =
                prepareStatement(
                    "UPDATE sak set flyktning = ? where id = ?",
                )
            statement.setJsonb(1, flyktning)
            statement.setLong(2, sakId)
            statement.executeUpdate()
        }
    }

    fun finnSakerMedGraderingOgSkjerming(sakIder: List<Long>): List<SakMedGradering> {
        with(connection()) {
            val statement =
                prepareStatement(
                    "SELECT id, adressebeskyttelse from sak where id = any(?)",
                )
            statement.setArray(1, createArrayOf("bigint", sakIder.toTypedArray()))
            return statement.executeQuery().toList {
                SakMedGradering(
                    id = getLong(1),
                    adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                )
            }
        }
    }

    fun oppdaterAdresseBeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        with(connection()) {
            val statement = prepareStatement("UPDATE sak SET adressebeskyttelse = ? where id = ?")
            statement.setString(1, adressebeskyttelseGradering.toString())
            statement.setLong(2, sakId)
            return statement.executeUpdate().also { require(it == 1) }
        }
    }

    fun hentSaker(): List<Sak> {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr, enhet from sak")
        return statement.executeQuery().toList { this.toSak() }
    }

    fun hentSak(id: Long): Sak? {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr, enhet from sak where id = ?")
        statement.setLong(1, id)
        return statement.executeQuery().singleOrNull { this.toSak() }
    }

    fun opprettSak(
        fnr: String,
        type: SakType,
        enhet: String,
    ): Sak {
        val statement =
            connection().prepareStatement(
                "INSERT INTO sak(sakType, fnr, enhet) VALUES(?, ?, ?) RETURNING id, sakType, fnr, enhet",
            )
        statement.setString(1, type.name)
        statement.setString(2, fnr)
        statement.setString(3, enhet)
        return requireNotNull(
            statement.executeQuery().singleOrNull { this.toSak() },
        ) { "Kunne ikke opprette sak for fnr: $fnr" }
    }

    fun oppdaterEnheterPaaSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>) {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    UPDATE sak 
                    set enhet = ? 
                    where id = ?
                    """.trimIndent(),
                )
            saker.forEach {
                statement.setString(1, it.enhet)
                statement.setLong(2, it.id)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun finnSaker(fnr: String): List<Sak> {
        val statement = connection().prepareStatement("SELECT id, sakType, fnr, enhet from sak where fnr = ?")
        statement.setString(1, fnr)
        return statement.executeQuery().toList { this.toSak() }
    }

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    ): Int {
        return with(connection()) {
            val statement =
                prepareStatement(
                    """
                    UPDATE sak 
                    set erSkjermet = ? 
                    where id = any(?)
                    """.trimIndent(),
                )
            statement.setBoolean(1, skjermet)
            statement.setArray(2, createArrayOf("bigint", sakIder.toTypedArray()))
            statement.executeUpdate()
        }
    }

    private fun ResultSet.toSak() =
        Sak(
            sakType = enumValueOf(getString("sakType")),
            ident = getString("fnr"),
            id = getLong("id"),
            enhet = getString("enhet"),
        )
}
