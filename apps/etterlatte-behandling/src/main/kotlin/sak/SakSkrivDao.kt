package no.nav.etterlatte.sak

import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.sak.Saksendring.Endringstype.ENDRE_ADRESSEBESKYTTELSE
import no.nav.etterlatte.sak.Saksendring.Endringstype.ENDRE_ENHET
import no.nav.etterlatte.sak.Saksendring.Endringstype.ENDRE_IDENT
import no.nav.etterlatte.sak.Saksendring.Endringstype.ENDRE_SKJERMING
import no.nav.etterlatte.sak.Saksendring.Endringstype.OPPRETT_SAK
import java.sql.ResultSet

class SakSkrivDao(
    private val sakendringerDao: SakendringerDao,
) {
    private val mapTilSak: ResultSet.() -> Sak = {
        Sak(
            sakType = enumValueOf(getString("sakType")),
            ident = getString("fnr"),
            id = SakId(getLong("id")),
            enhet = Enhetsnummer(getString("enhet")),
        )
    }

    fun oppdaterAdresseBeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int =
        sakendringerDao.oppdaterSaker(sakId, ENDRE_ADRESSEBESKYTTELSE) { connection ->
            with(connection) {
                val statement = prepareStatement("UPDATE sak SET adressebeskyttelse = ? where id = ?")
                statement.setString(1, adressebeskyttelseGradering.name)
                statement.setSakId(2, sakId)
                statement.executeUpdate().also {
                    logger.info(
                        "Oppdaterer adressebeskyttelse med: $adressebeskyttelseGradering for sak med id: $sakId, antall oppdatert er $it",
                    )
                    krev(it > 0) {
                        "Kunne ikke oppdaterAdresseBeskyttelse for id sakid $sakId"
                    }
                }
            }
        }

    fun opprettSak(
        fnr: String,
        type: SakType,
        enhet: Enhetsnummer,
    ): Sak =
        sakendringerDao.opprettSak(OPPRETT_SAK) { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        "INSERT INTO sak(sakType, fnr, enhet, opprettet) VALUES(?, ?, ?, ?) RETURNING id, sakType, fnr, enhet",
                    )
                statement.setString(1, type.name)
                statement.setString(2, fnr)
                statement.setString(3, enhet.enhetNr)
                statement.setTidspunkt(4, Tidspunkt.now())
                krevIkkeNull(
                    statement
                        .executeQuery()
                        .singleOrNull(mapTilSak),
                ) { "Kunne ikke opprette sak for fnr: $fnr" }
            }
        }

    fun oppdaterIdent(
        sakId: SakId,
        nyIdent: Folkeregisteridentifikator,
    ) {
        sakendringerDao.oppdaterSaker(sakId, ENDRE_IDENT) {
            it
                .prepareStatement(
                    """
                    UPDATE sak 
                    SET fnr = ? 
                    WHERE id = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, nyIdent.value)
                    setLong(2, sakId.sakId)
                }.executeUpdate()
        }
    }

    fun oppdaterEnheterPaaSaker(saker: List<SakMedEnhet>) {
        sakendringerDao.oppdaterSaker(saker.map { it.id }, ENDRE_ENHET) {
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
                    statement.setSakId(2, sak.id)
                    statement.executeUpdate()
                }
            }
        }
    }

    fun markerSakerMedSkjerming(
        sakIder: List<SakId>,
        skjermet: Boolean,
    ) = sakendringerDao.oppdaterSaker(sakIder, ENDRE_SKJERMING) {
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
