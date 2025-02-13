package no.nav.etterlatte.sak

import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.ktor.route.logger

class SakSkrivDao(
    private val sakendringerDao: SakendringerDao,
) {
    fun opprettSak(
        fnr: String,
        type: SakType,
        enhet: Enhetsnummer,
    ): Sak =
        sakendringerDao.opprettSak { connection ->
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
                ) { "Kunne ikke opprette sak for fnr: ${fnr.maskerFnr()}" }
            }
        }

    fun oppdaterAdresseBeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ) = sakendringerDao.oppdaterSak(sakId, Endringstype.ENDRE_ADRESSEBESKYTTELSE) { connection ->
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

    fun oppdaterIdent(
        sakId: SakId,
        nyIdent: Folkeregisteridentifikator,
    ) {
        sakendringerDao.oppdaterSak(sakId, Endringstype.ENDRE_IDENT) {
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

    fun oppdaterEnhet(
        sakMedEnhet: SakMedEnhet,
        kommentar: String? = null,
    ) {
        sakendringerDao.oppdaterSak(sakMedEnhet.id, Endringstype.ENDRE_ENHET, kommentar) {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE sak 
                        set enhet = ? 
                        where id = ?
                        """.trimIndent(),
                    )

                statement.setString(1, sakMedEnhet.enhet.enhetNr)
                statement.setSakId(2, sakMedEnhet.id)
                statement.executeUpdate()
            }
        }
    }

    fun oppdaterSkjerming(
        sakId: SakId,
        skjermet: Boolean,
    ) = sakendringerDao.oppdaterSak(sakId, Endringstype.ENDRE_SKJERMING) {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    UPDATE sak 
                    set erSkjermet = ? 
                    where id = ?
                    """.trimIndent(),
                )
            statement.setBoolean(1, skjermet)
            statement.setSakId(2, sakId)
            statement.executeUpdate()
        }
    }
}
