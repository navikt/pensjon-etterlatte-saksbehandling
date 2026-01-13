package no.nav.etterlatte.sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.util.UUID

class SakendringerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    internal fun opprettSak(block: (connection: Connection) -> Sak): Sak =
        connectionAutoclosing
            .hentConnection { connection ->
                block(connection)
            }.also { sak ->
                lagreSaksendring(
                    Saksendring.create(
                        user = Kontekst.get().AppUser,
                        endringstype = Endringstype.OPPRETT_SAK,
                        sakFoer = null,
                        sakEtter = krevIkkeNull(hentKomplettSak(sak.id)) { "Sak med sakId ${sak.id} ble ikke funnet etter oppretting" },
                    ),
                )
            }

    internal fun oppdaterSak(
        sakId: SakId,
        endringstype: Endringstype,
        kommentar: String? = null,
        block: (connection: Connection) -> Unit,
    ) {
        val sakFoer = krevIkkeNull(hentKomplettSak(sakId)) { "Fant ikke sak med sakId $sakId. Må ha en sak for å kunne endre den" }

        connectionAutoclosing.hentConnection { connection ->
            block(connection)
        }

        val sakEtter = krevIkkeNull(hentKomplettSak(sakId)) { "Fant ikke sak med sakId $sakId. Må ha en sak etter endring" }
        if (sakEtter == sakFoer) {
            return
        }

        lagreSaksendring(
            Saksendring.create(
                user = Kontekst.get().AppUser,
                endringstype = endringstype,
                sakFoer = sakFoer,
                sakEtter = sakEtter,
                kommentar = kommentar,
            ),
        )
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

    private fun lagreSaksendring(saksendring: Saksendring): Int {
        if (saksendring.foer != null) {
            krev(saksendring.foer.id == saksendring.etter.id) { "SakId må være like før og etter" }
        }
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO saksendring(id, sak_id, endringstype, foer, etter, tidspunkt, ident, identtype, kommentar)
                        VALUES(?::UUID, ?, ?, ?::JSONB, ?::JSONB, ?, ?, ?, ?)
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
                statement.setString(9, saksendring.kommentar)

                statement.executeUpdate()
            }
        }
    }

    private fun hentKomplettSak(sakId: SakId): KomplettSak? =
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
                            opprettet = getTidspunktOrNull("opprettet"),
                        )
                    }
            }
        }
}
