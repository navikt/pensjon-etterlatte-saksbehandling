package no.nav.etterlatte.behandling.bosattutland

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.SQLJsonb
import no.nav.etterlatte.libs.database.SQLObject
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.opprett
import java.sql.ResultSet
import java.util.UUID

class BosattUtlandDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreBosattUtland(bosattUtland: BosattUtland) =
        connectionAutoclosing.opprett(
            """
                        insert into bosattutland(behandlingid, rinanummer, mottattSeder, sendteSeder)
                            VALUES(?,?,?,?)
                            ON CONFLICT(behandlingid)
                            DO UPDATE SET rinanummer = excluded.rinanummer,
                            mottattSeder = excluded.mottattSeder, sendteSeder = excluded.sendteSeder
                        """,
            listOf(
                SQLObject(bosattUtland.behandlingId),
                SQLObject(bosattUtland.rinanummer),
                SQLJsonb(bosattUtland.mottatteSeder),
                SQLJsonb(bosattUtland.sendteSeder),
            ),
            { require(it == 1) },
        )

    fun hentBosattUtland(behandlingId: UUID): BosattUtland? =
        connectionAutoclosing
            .hent(
                "SELECT behandlingid, rinanummer, mottattSeder, sendteSeder from bosattutland where behandlingid = ?",
                listOf(SQLObject(behandlingId)),
            ) { toBosattUtland() }

    private fun ResultSet.toBosattUtland(): BosattUtland =
        BosattUtland(
            behandlingId = getUUID("behandlingid"),
            rinanummer = getString("rinanummer"),
            mottatteSeder = getString("mottattSeder").let { objectMapper.readValue(it) },
            sendteSeder = getString("sendteSeder").let { objectMapper.readValue(it) },
        )
}
