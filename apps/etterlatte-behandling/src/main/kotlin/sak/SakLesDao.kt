package no.nav.etterlatte.sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Date
import java.sql.ResultSet
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger

class SakLesDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<SakId>,
        ekskluderteSaker: List<SakId>,
        sakType: SakType? = null,
        rekjoereManuellUtenOppgave: Boolean = false,
    ): List<Sak> =
        connectionAutoclosing.hentConnection { connection ->
            val statuserSomRekjoeres: List<KjoeringStatus> =
                rekjoereManuellUtenOppgave.let {
                    when (it) {
                        true -> listOf(KjoeringStatus.FEILA, KjoeringStatus.TIL_MANUELL_UTEN_OPPGAVE)
                        false -> listOf(KjoeringStatus.FEILA)
                    }
                }
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        WITH siste_kjoering AS (
                            SELECT DISTINCT ON (sak_id) sak_id, status
                            FROM omregningskjoering
                            WHERE kjoering = ?
                            ORDER BY sak_id, tidspunkt DESC
                        )
                        SELECT s.id, s.sakType, s.fnr, s.enhet, s.adressebeskyttelse, s.erSkjermet
                        FROM sak s
                        WHERE (
                            NOT EXISTS (SELECT 1 FROM siste_kjoering k WHERE k.sak_id = s.id)
                            OR EXISTS (SELECT 1 FROM siste_kjoering k WHERE k.sak_id = s.id AND k.status = ANY (?))
                        )
                        AND EXISTS (SELECT 1 FROM behandling b WHERE b.sak_id = s.id)
                        ${if (spesifikkeSaker.isEmpty()) "" else " AND id = ANY(?)"}
                        ${if (ekskluderteSaker.isEmpty()) "" else " AND NOT(id = ANY(?))"}
                        ${if (sakType == null) "" else " AND s.saktype = ?"}
                        order by id ASC
                        limit $antall
                        """.trimMargin(),
                    ).apply {
                        val paramIndex = AtomicInteger(1)

                        setString(paramIndex.getAndIncrement(), kjoering)
                        setArray(paramIndex.getAndIncrement(), createArrayOf("text", statuserSomRekjoeres.map { it.name }.toTypedArray()))
                        if (spesifikkeSaker.isNotEmpty()) {
                            setArray(paramIndex.getAndIncrement(), createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                        }
                        if (ekskluderteSaker.isNotEmpty()) {
                            setArray(paramIndex.getAndIncrement(), createArrayOf("bigint", ekskluderteSaker.toTypedArray()))
                        }
                        if (sakType != null) {
                            setString(paramIndex.get(), sakType.name)
                        }
                    }
                statement.executeQuery().toList(mapTilSak)
            }
        }

    fun hentSak(id: SakId): Sak? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("SELECT id, sakType, fnr, enhet, adressebeskyttelse, erSkjermet from sak where id = ?")
                statement.setSakId(1, id)
                statement
                    .executeQuery()
                    .singleOrNull(mapTilSak)
            }
        }

    fun finnSakMedGraderingOgSkjerming(id: SakId): SakMedGraderingOgSkjermet =
        connectionAutoclosing.hentConnection { connection ->
            val statement =
                connection.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet, enhet from sak where id = ?")
            statement.setSakId(1, id)
            statement.executeQuery().single {
                SakMedGraderingOgSkjermet(
                    id = SakId(getLong("id")),
                    adressebeskyttelseGradering =
                        getString("adressebeskyttelse")?.let {
                            AdressebeskyttelseGradering.valueOf(it)
                        },
                    erSkjermet = getBoolean("erskjermet"),
                    enhetNr = Enhetsnummer(getString("enhet")),
                )
            }
        }

    fun finnFlyktningForSak(id: SakId): Flyktning? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("SELECT flyktning from sak where id = ?")
                statement.setSakId(1, id)
                statement.executeQuery().singleOrNull {
                    this.getString("flyktning")?.let { objectMapper.readValue(it) }
                }
            }
        }

    fun finnSaker(
        fnr: String,
        type: SakType? = null,
    ): List<Sak> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, sakType, fnr, enhet, adressebeskyttelse, erSkjermet
                        FROM sak
                        WHERE fnr = ?
                            AND (? OR saktype = ?)
                        """.trimIndent(),
                    )
                statement.setString(1, fnr)
                statement.setBoolean(2, type == null)
                statement.setString(3, type?.name)
                statement
                    .executeQuery()
                    .toList(mapTilSak)
            }
        }

    fun hentSakerMedIder(sakIder: List<SakId>): List<Sak> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, fnr, enhet, adressebeskyttelse, erSkjermet, sakType 
                        FROM sak 
                        WHERE id = ANY (?)
                        """.trimIndent(),
                    )
                statement.setArray(1, createArrayOf("bigint", sakIder.toTypedArray()))
                statement
                    .executeQuery()
                    .toList(mapTilSak)
            }
        }

    fun finnSakerMedPleieforholdOpphoerer(maanedOpphoerte: YearMonth): List<SakId> =
        connectionAutoclosing.hentConnection { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT DISTINCT ON (sak_id) sak_id, tidligere_familiepleier ->> 'opphoertPleieforhold' FROM behandling 
                    WHERE tidligere_familiepleier ->> 'opphoertPleieforhold' IS NOT NULL 
                    AND status = ?
                    AND TO_DATE(tidligere_familiepleier ->> 'opphoertPleieforhold', 'YYYY-MM-DD') BETWEEN ? AND ?
                    ORDER BY sak_id, behandling_opprettet DESC;
                    """.trimIndent(),
                )
            statement.setString(1, BehandlingStatus.IVERKSATT.name)
            statement.setDate(2, Date.valueOf(maanedOpphoerte.atDay(1)))
            statement.setDate(3, Date.valueOf(maanedOpphoerte.atEndOfMonth()))
            statement.executeQuery().toList {
                SakId(getLong("sak_id"))
            }
        }

    fun finnSakerMedSkjerming(sakType: SakType): List<Sak> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, sakType, fnr, enhet, adressebeskyttelse, erSkjermet
                        FROM sak
                        WHERE sakType = ?
                        AND erSkjermet is true
                        """.trimIndent(),
                    )
                statement.setString(1, sakType.name)
                statement
                    .executeQuery()
                    .toList(mapTilSak)
            }
        }

    fun hentAllePersonerMedBehandlingMedStatusIkkeAvbruttEllerAvslag(): List<String> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    SELECT DISTINCT(fnr)
                    FROM sak
                    JOIN behandling ON behandling.sak_id = sak.id
                    WHERE behandling.status NOT IN ('AVSLAG','AVBRUTT');
                    """.trimIndent(),
                ).executeQuery()
                    .toList { getString("fnr") }
            }
        }
}

internal val mapTilSak: ResultSet.() -> Sak = {
    Sak(
        sakType = enumValueOf(getString("sakType")),
        ident = getString("fnr"),
        id = SakId(getLong("id")),
        enhet = Enhetsnummer(getString("enhet")),
        adressebeskyttelse = getString("adressebeskyttelse")?.let { enumValueOf<AdressebeskyttelseGradering>(it) },
        erSkjermet = getBoolean("erSkjermet"),
    )
}
