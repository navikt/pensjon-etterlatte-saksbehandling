package no.nav.etterlatte.sak

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.ktor.route.logger
import java.sql.ResultSet

class SakSkrivDao(
    private val sakendringerDao: SakendringerDao,
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

    fun oppdaterAdresseBeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int =
        sakendringerDao.lagreEndringerPaaSak(sakId) { connection ->
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
        sakendringerDao.opprettSak { connection ->
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
        sakendringerDao.lagreEndringerPaaSaker(saker.map { it.id }) {
            with(it) {
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
                    statement.executeUpdate()
                }
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
