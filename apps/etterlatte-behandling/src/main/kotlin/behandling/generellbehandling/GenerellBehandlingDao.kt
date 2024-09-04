package no.nav.etterlatte.behandling.generellbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.SQLJsonb
import no.nav.etterlatte.libs.database.SQLLong
import no.nav.etterlatte.libs.database.SQLObject
import no.nav.etterlatte.libs.database.SQLString
import no.nav.etterlatte.libs.database.SQLTidspunkt
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdaterOgReturner
import no.nav.etterlatte.libs.database.opprettOgReturner
import java.sql.ResultSet
import java.util.UUID

class GenerellBehandlingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun opprettGenerellbehandling(generellBehandling: GenerellBehandling): GenerellBehandling =
        connectionAutoclosing.opprettOgReturner(
            """
                        INSERT INTO generellbehandling(id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status)
                        VALUES(?::UUID, ?, ?, ?, ?, ?, ?)
                        RETURNING id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        """,
            mapOf(
                1 to SQLObject(generellBehandling.id),
                2 to SQLJsonb(generellBehandling.innhold),
                3 to SQLLong(generellBehandling.sakId),
                4 to SQLTidspunkt(generellBehandling.opprettet),
                5 to SQLString(generellBehandling.type.name),
                6 to SQLObject(generellBehandling.tilknyttetBehandling),
                7 to SQLString(generellBehandling.status.name),
            ),
        ) { toGenerellBehandling() }

    fun oppdaterGenerellBehandling(generellBehandling: GenerellBehandling): GenerellBehandling =
        connectionAutoclosing.oppdaterOgReturner(
            """
                        UPDATE generellbehandling
                        SET innhold = ?, status = ?, behandler = ?, attestant = ?, kommentar = ?
                        where id = ?
                        RETURNING id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        """,
            mapOf(
                1 to SQLJsonb(generellBehandling.innhold),
                2 to SQLString(generellBehandling.status.name),
                3 to SQLJsonb(generellBehandling.behandler),
                4 to SQLJsonb(generellBehandling.attestant),
                5 to SQLString(generellBehandling.returnertKommenar),
                6 to SQLObject(generellBehandling.id),
            ),
        ) { toGenerellBehandling() }

    fun hentGenerellBehandlingMedId(id: UUID): GenerellBehandling? =
        connectionAutoclosing
            .hent(
                """
                        SELECT id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        FROM generellbehandling
                        WHERE id = ?
                        """,
                mapOf(1 to SQLObject(id)),
            ) { toGenerellBehandling() }

    fun hentGenerellBehandlingForSak(sakId: SakId): List<GenerellBehandling> =
        connectionAutoclosing.hentListe(
            """
                        SELECT id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        FROM generellbehandling
                        WHERE sak_id = ?
                        """,
            mapOf(1 to SQLLong(sakId)),
        ) { toGenerellBehandling() }

    fun hentBehandlingForTilknyttetBehandling(tilknyttetBehandlingId: UUID): GenerellBehandling? =
        connectionAutoclosing
            .hent(
                """
                        SELECT id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        FROM generellbehandling
                        WHERE tilknyttet_behandling = ?::uuid
                        """,
                mapOf(1 to SQLObject(tilknyttetBehandlingId)),
            ) { toGenerellBehandling() }

    private fun ResultSet.toGenerellBehandling(): GenerellBehandling =
        GenerellBehandling(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            type = GenerellBehandling.GenerellBehandlingType.valueOf(getString("type")),
            innhold = getString("innhold")?.let { objectMapper.readValue(it) },
            opprettet = getTidspunkt("opprettet"),
            tilknyttetBehandling = getObject("tilknyttet_behandling") as UUID?,
            status = GenerellBehandling.Status.valueOf(getString("status")),
            behandler = getString("behandler")?.let { objectMapper.readValue(it) },
            attestant = getString("attestant")?.let { objectMapper.readValue(it) },
            returnertKommenar = getString("kommentar"),
        )
}
