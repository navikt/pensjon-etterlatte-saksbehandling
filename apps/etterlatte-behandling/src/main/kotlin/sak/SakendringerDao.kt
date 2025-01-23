package no.nav.etterlatte.sak

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.sak.Saksendring.Endringstype
import no.nav.etterlatte.sak.Saksendring.Identtype
import java.sql.Connection
import java.util.UUID

class SakendringerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
    private val hentSak: (id: SakId) -> Sak?,
) {
    internal fun oppdaterSaker(
        id: SakId,
        endringstype: Endringstype,
        block: (connection: Connection) -> Unit,
    ): Int {
        val foer = krevIkkeNull(hentSak(id)) { "Må ha en sak for å kunne endre den" }
        connectionAutoclosing.hentConnection { connection ->
            block(connection)
        }
        val etter = krevIkkeNull(hentSak(id)) { "Må ha en sak etter endring" }

        return lagreSaksendring(saksendring(endringstype, foer, etter))
    }

    internal fun opprettSak(
        endringstype: Endringstype,
        block: (connection: Connection) -> Sak,
    ) = connectionAutoclosing
        .hentConnection { connection ->
            block(connection)
        }.also { sakEtter -> lagreSaksendring(saksendring(endringstype, null, sakEtter)) }

    internal fun oppdaterSaker(
        sakIdList: Collection<SakId>,
        endringstype: Endringstype,
        block: (connection: Connection) -> Unit,
    ) = sakIdList.forEach { sakId ->
        oppdaterSaker(sakId, endringstype, block)
    }

    private fun lagreSaksendring(saksendring: Saksendring): Int =
        connectionAutoclosing.hentConnection {
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

    private fun saksendring(
        endringstype: Endringstype,
        sakFoer: Sak?,
        sakEtter: Sak,
    ): Saksendring {
        val appUser = Kontekst.get().AppUser
        return Saksendring(
            UUID.randomUUID(),
            endringstype,
            sakFoer,
            sakEtter,
            Tidspunkt.now(),
            appUser.name(),
            when (appUser) {
                is SaksbehandlerMedEnheterOgRoller -> Identtype.SAKSBEHANDLER
                else -> Identtype.GJENNY
            },
        )
    }
}
