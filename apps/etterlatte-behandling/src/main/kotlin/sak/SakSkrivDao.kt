package no.nav.etterlatte.sak

import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.ktor.route.logger
import java.sql.ResultSet

class SakSkrivDao(
    private val sakendringerDao: SakendringerDao,
) {
    private val mapTilSak: ResultSet.() -> Sak = {
        Sak(
            sakType = enumValueOf(getString("sakType")),
            ident = getString("fnr"),
            id = getLong("id"),
            enhet = Enhetsnummer(getString("enhet")),
        )
    }

    fun oppdaterAdresseBeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int =
        sakendringerDao.lagreEndringerPaaSak(sakId, "oppdaterAdresseBeskyttelse") { connection ->
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
        enhet: Enhetsnummer,
    ): Sak =
        sakendringerDao.opprettSak("opprettSak") { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        "INSERT INTO sak(sakType, fnr, enhet, opprettet) VALUES(?, ?, ?, ?) RETURNING id, sakType, fnr, enhet",
                    )
                statement.setString(1, type.name)
                statement.setString(2, fnr)
                statement.setString(3, enhet.enhetNr)
                statement.setTidspunkt(4, Tidspunkt.now())
                requireNotNull(
                    statement
                        .executeQuery()
                        .singleOrNull(mapTilSak),
                ) { "Kunne ikke opprette sak for fnr: $fnr" }
            }
        }

    fun oppdaterEnheterPaaSaker(saker: List<SakMedEnhet>) {
        sakendringerDao.lagreEndringerPaaSaker(saker.map { it.id }, "oppdaterEnheterPaaSaker") {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE sak 
                        set enhet = ? 
                        where id = ?
                        """.trimIndent(),
                    )
                saker.forEach { sak ->
                    statement.setString(1, sak.enhet.enhetNr)
                    statement.setLong(2, sak.id)
                    statement.executeUpdate()
                }
            }
        }
    }

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    ) = sakendringerDao.lagreEndringerPaaSaker(sakIder, "markerSakerMedSkjerming") {
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
