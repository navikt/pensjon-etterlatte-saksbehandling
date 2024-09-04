package no.nav.etterlatte.behandling.bosattutland

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.ForventaResultat
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
            mapOf(
                1 to SQLObject(bosattUtland.behandlingId),
                2 to SQLObject(bosattUtland.rinanummer),
                3 to SQLJsonb(bosattUtland.mottatteSeder),
                4 to SQLJsonb(bosattUtland.sendteSeder),
            ),
            ForventaResultat.RADER,
        )

    fun hentBosattUtland(behandlingId: UUID): BosattUtland? =
        connectionAutoclosing
            .hent(
                "SELECT behandlingid, rinanummer, mottattSeder, sendteSeder from bosattutland where behandlingid = ?",
                mapOf(1 to SQLObject(behandlingId)),
            ) { toBosattUtland() }

    private fun ResultSet.toBosattUtland(): BosattUtland =
        BosattUtland(
            behandlingId = getUUID("behandlingid"),
            rinanummer = getString("rinanummer"),
            mottatteSeder = getString("mottattSeder").let { objectMapper.readValue(it) },
            sendteSeder = getString("sendteSeder").let { objectMapper.readValue(it) },
        )
}
