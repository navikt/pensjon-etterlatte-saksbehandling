package no.nav.etterlatte.sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.sak.Saksendring.Endringstype
import no.nav.etterlatte.sak.Saksendring.Identtype
import sak.KomplettSak
import java.sql.Connection
import java.util.UUID

class SakendringerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    internal fun opprettSak(
        endringstype: Endringstype,
        block: (connection: Connection) -> Sak,
    ): Sak =
        connectionAutoclosing
            .hentConnection { connection ->
                block(connection)
            }.also { sak ->
                lagreSaksendring(
                    saksendring(
                        endringstype = endringstype,
                        sakFoer = null,
                        sakEtter = krevIkkeNull(hentKomplettSak(sak.id)) { "Sak med ID ${sak.id} ble ikke funnet etter oppretting" },
                        kommentar = null,
                    ),
                )
            }

    internal fun oppdaterSaker(
        sakIdList: Collection<SakId>,
        endringstype: Endringstype,
        kommentar: String?,
        block: (connection: Connection) -> Unit,
    ) {
        val sakerFoer =
            sakIdList.map {
                krevIkkeNull(hentKomplettSak(it)) { "Fant ikke sak med ID $it. Må ha en sak for å kunne endre den" }
            }

        connectionAutoclosing.hentConnection { connection ->
            block(connection)
        }

        val sakerEtter =
            sakIdList.map {
                krevIkkeNull(hentKomplettSak(it)) { "Fant ikke sak med ID $it. Må ha en sak etter endring" }
            }

        sakerEtter.forEach { sakEtter ->
            lagreSaksendring(
                saksendring(
                    endringstype,
                    sakerFoer.singleOrNull { it.id == sakEtter.id },
                    sakEtter,
                    kommentar,
                ),
            )
        }
    }

    internal fun oppdaterSak(
        id: SakId,
        endringstype: Endringstype,
        block: (connection: Connection) -> Unit,
    ) = oppdaterSaker(listOf(id), endringstype, null, block)

    internal fun hentKomplettSak(sakId: SakId): KomplettSak? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("SELECT * FROM sak WHERE id = ?")
                statement.setLong(1, sakId.sakId)
                statement
                    .executeQuery()
                    .singleOrNull {
                        KomplettSak(
                            id = SakId(getLong("id")),
                            ident = getString("fnr"),
                            sakType = enumValueOf(getString("sakType")),
                            adressebeskyttelse =
                                getString("adressebeskyttelse")
                                    ?.let { enumValueOf<AdressebeskyttelseGradering>(it) },
                            erSkjermet = if (getObject("erskjermet") != null) getBoolean("erskjermet") else null,
                            enhet = Enhetsnummer(getString("enhet")),
                            flyktning = getString("flyktning")?.let { objectMapper.readValue(it) },
                            opprettet = getTidspunkt("opprettet"),
                        )
                    }
            }
        }

    internal fun hentEndringerForSak(sakId: SakId): List<Saksendring> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("select * from saksendring where sak_id = ?")
                statement.setLong(1, sakId.sakId)
                statement
                    .executeQuery()
                    .toList {
                        Saksendring(
                            id = UUID.fromString(getString("id")),
                            endringstype = enumValueOf<Endringstype>(getString("endringstype")),
                            foer = getString("foer")?.let { objectMapper.readValue(it) },
                            etter = objectMapper.readValue(getString("etter")),
                            tidspunkt = getTidspunkt("tidspunkt"),
                            ident = getString("ident"),
                            identtype = enumValueOf(getString("identtype")),
                            kommentar = getString("kommentar"),
                        )
                    }
            }
        }

    internal fun lagreSaksendring(saksendring: Saksendring): Int {
        if (saksendring.foer != null) {
            krev(saksendring.foer.id == saksendring.etter.id) { "Saks-ID må være like før og etter" }
        }
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO saksendring(id, sak_id, endringstype, foer, etter, tidspunkt, ident, identtype)
                        VALUES(?::UUID, ?, ?, ?::JSONB, ?::JSONB, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, saksendring.id)
                statement.setLong(2, saksendring.etter.id.sakId)
                statement.setString(3, saksendring.endringstype.name)
                statement.setJsonb(4, saksendring.foer)
                statement.setJsonb(5, saksendring.etter)
                statement.setTidspunkt(6, saksendring.tidspunkt)
                statement.setString(7, saksendring.ident)
                statement.setString(8, saksendring.identtype.name)

                statement.executeUpdate()
            }
        }
    }

    private fun saksendring(
        endringstype: Endringstype,
        sakFoer: KomplettSak?,
        sakEtter: KomplettSak,
        kommentar: String?,
    ): Saksendring {
        val appUser = Kontekst.get().AppUser

        return Saksendring(
            id = UUID.randomUUID(),
            endringstype = endringstype,
            foer = sakFoer,
            etter = sakEtter,
            tidspunkt = Tidspunkt.now(),
            ident = appUser.name(),
            identtype =
                when (appUser) {
                    is SaksbehandlerMedEnheterOgRoller -> Identtype.SAKSBEHANDLER
                    else -> Identtype.GJENNY
                },
            kommentar = kommentar,
        )
    }
}
