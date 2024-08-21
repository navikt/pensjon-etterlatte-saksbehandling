package no.nav.etterlatte.sak

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.ktor.route.logger
import java.sql.ResultSet

data class SakMedUtlandstilknytning(
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
        ) = SakMedUtlandstilknytning(
            ident = sak.ident,
            sakType = sak.sakType,
            id = sak.id,
            enhet = sak.enhet,
            utlandstilknytning = utlandstilknytning,
        )
    }
}

class SakSkrivDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val mapTilSak: ResultSet.() -> Sak = {
        Sak(
            sakType = enumValueOf(getString("sakType")),
            ident = getString("fnr"),
            id = getLong("id"),
            enhet = getString("enhet"),
        )
    }

    fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "UPDATE sak set flyktning = ? where id = ?",
                    )
                statement.setJsonb(1, flyktning)
                statement.setLong(2, sakId)
                statement.executeUpdate()
            }
        }
    }

    fun oppdaterAdresseBeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("UPDATE sak SET adressebeskyttelse = ? where id = ?")
                statement.setString(1, adressebeskyttelseGradering.name)
                statement.setLong(2, sakId)
                statement.executeUpdate().also {
                    logger.info(
                        "Oppdaterer adressebeskyttelse med: $adressebeskyttelseGradering for sak med id: $sakId, antall oppdatert er $it",
                    )
                    require(it == 1)
                }
            }
        }

    fun opprettSak(
        fnr: String,
        type: SakType,
        enhet: String,
    ): Sak =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        "INSERT INTO sak(sakType, fnr, enhet, opprettet) VALUES(?, ?, ?, ?) RETURNING id, sakType, fnr, enhet",
                    )
                statement.setString(1, type.name)
                statement.setString(2, fnr)
                statement.setString(3, enhet)
                statement.setTidspunkt(4, Tidspunkt.now())
                requireNotNull(
                    statement
                        .executeQuery()
                        .singleOrNull(mapTilSak),
                ) { "Kunne ikke opprette sak for fnr: $fnr" }
            }
        }

    fun oppdaterEnheterPaaSaker(saker: List<SakMedEnhet>) {
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
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
    }

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    ): Int =
        connectionAutoclosing.hentConnection {
            with(it) {
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
}
