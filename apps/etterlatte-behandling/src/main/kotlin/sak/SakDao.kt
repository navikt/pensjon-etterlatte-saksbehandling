package no.nav.etterlatte.sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.ktor.route.logger
import java.sql.ResultSet

data class SakMedUtlandstilknytning(
    val ident: String,
    val sakType: SakType,
    val id: Long,
    val enhet: String,
    val utlandstilknytning: Utlandstilknytning?,
) {
    companion object {
        fun fra(
            sak: Sak,
            utlandstilknytning: Utlandstilknytning?,
        ) = SakMedUtlandstilknytning(
            ident = sak.ident,
            sakType = sak.sakType,
            id = sak.id,
            enhet = sak.enhet,
            utlandstilknytning = utlandstilknytning,
        )
    }
}

class SakDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val mapTilSak: ResultSet.() -> Sak = {
        Sak(
            sakType = enumValueOf(getString("sakType")),
            ident = getString("fnr"),
            id = getLong("id"),
            enhet = getString("enhet"),
        )
    }

    fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "UPDATE sak set flyktning = ?, sistEndretAv = ? where id = ?",
                    )
                statement.setJsonb(1, flyktning)
                statement.setString(2, Kontekst.get().AppUser.name())
                statement.setLong(3, sakId)
                statement.executeUpdate()
            }
        }
    }

    fun finnSakerMedGraderingOgSkjerming(sakIder: List<Long>): List<SakMedGradering> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        "SELECT id, adressebeskyttelse from sak where id = any(?)",
                    )
                statement.setArray(1, createArrayOf("bigint", sakIder.toTypedArray()))
                statement.executeQuery().toList {
                    SakMedGradering(
                        id = getLong(1),
                        adressebeskyttelseGradering = getString(2)?.let { AdressebeskyttelseGradering.valueOf(it) },
                    )
                }
            }
        }

    fun oppdaterAdresseBeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("UPDATE sak SET adressebeskyttelse = ?, sistEndretAv = ? where id = ?")
                statement.setString(1, adressebeskyttelseGradering.name)
                statement.setString(2, Kontekst.get().AppUser.name())
                statement.setLong(3, sakId)
                statement.executeUpdate().also {
                    logger.info(
                        "Oppdaterer adressebeskyttelse med: $adressebeskyttelseGradering for sak med id: $sakId, antall oppdatert er $it",
                    )
                    require(it == 1)
                }
            }
        }

    fun hentSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<Long>,
        ekskluderteSaker: List<Long>,
        sakType: SakType? = null,
    ): List<Sak> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """SELECT id, sakType, fnr, enhet from sak s 
                    where
                    (
                    -- ikke kjørt i det hele tatt
                    not exists (
                        select 1 from omregningskjoering k where k.sak_id=s.id 
                        and k.kjoering='$kjoering' 
                        and k.status!='${KjoeringStatus.KLAR_TIL_REGULERING.name}'
                    )
                    or exists(
                        -- nyeste kjøring har feila
                        select 1 from omregningskjoering k
                            where k.sak_id=s.id 
                            and k.kjoering='$kjoering' 
                            and k.status = '${KjoeringStatus.FEILA.name}'
                            and k.tidspunkt > (select max(o.tidspunkt) from omregningskjoering o where o.sak_id=k.sak_id and o.kjoering=k.kjoering and o.status != '${KjoeringStatus.FEILA.name}')
                        )
                        )
                    ${if (spesifikkeSaker.isEmpty()) "" else " and id = any(?)"}
                    ${if (ekskluderteSaker.isEmpty()) "" else " and NOT(id = any(?))"}
                    ${if (sakType == null) "" else " and s.saktype = ?"}
                    ORDER by id ASC
                    LIMIT $antall
                        """.trimMargin(),
                    )
                var paramIndex = 1
                if (spesifikkeSaker.isNotEmpty()) {
                    statement.setArray(paramIndex, createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                    paramIndex += 1
                }
                if (ekskluderteSaker.isNotEmpty()) {
                    statement.setArray(paramIndex, createArrayOf("bigint", ekskluderteSaker.toTypedArray()))
                    paramIndex += 1
                }
                if (sakType != null) {
                    statement.setString(paramIndex, sakType.name)
                    paramIndex += 1
                }

                statement
                    .executeQuery()
                    .toList(mapTilSak)
            }
        }

    fun hentSak(id: Long): Sak? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("SELECT id, sakType, fnr, enhet from sak where id = ?")
                statement.setLong(1, id)
                statement
                    .executeQuery()
                    .singleOrNull(mapTilSak)
            }
        }

    fun finnSakMedGraderingOgSkjerming(id: Long): SakMedGraderingOgSkjermet =
        connectionAutoclosing.hentConnection { connection ->
            val statement = connection.prepareStatement("SELECT id, adressebeskyttelse, erSkjermet, enhet from sak where id = ?")
            statement.setLong(1, id)
            statement.executeQuery().single {
                SakMedGraderingOgSkjermet(
                    id = getLong("id"),
                    adressebeskyttelseGradering =
                        getString("adressebeskyttelse")?.let {
                            AdressebeskyttelseGradering.valueOf(it)
                        },
                    erSkjermet = getBoolean("erskjermet"),
                    enhetNr = getString("enhet"),
                )
            }
        }

    fun finnFlyktningForSak(id: Long): Flyktning? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("SELECT flyktning from sak where id = ?")
                statement.setLong(1, id)
                statement.executeQuery().singleOrNull {
                    this.getString("flyktning")?.let { objectMapper.readValue(it) }
                }
            }
        }

    fun opprettSak(
        fnr: String,
        type: SakType,
        enhet: String,
    ): Sak =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        "INSERT INTO sak(sakType, fnr, enhet, opprettet, sistEndretAv) VALUES(?, ?, ?, ?, ?) RETURNING id, sakType, fnr, enhet",
                    )
                statement.setString(1, type.name)
                statement.setString(2, fnr)
                statement.setString(3, enhet)
                statement.setTidspunkt(4, Tidspunkt.now())
                statement.setString(5, Kontekst.get().AppUser.name())
                requireNotNull(
                    statement
                        .executeQuery()
                        .singleOrNull(mapTilSak),
                ) { "Kunne ikke opprette sak for fnr: $fnr" }
            }
        }

    fun oppdaterEnheterPaaSaker(saker: List<SakMedEnhet>) {
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE sak 
                        set enhet = ?,
                         sistEndretAv = ?
                        where id = ?
                        """.trimIndent(),
                    )
                saker.forEach {
                    statement.setString(1, it.enhet)
                    statement.setString(2, Kontekst.get().AppUser.name())
                    statement.setLong(3, it.id)
                    statement.addBatch()
                }
                statement.executeBatch()
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
                        SELECT id, sakType, fnr, enhet
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

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    ): Int =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE sak 
                        set erSkjermet = ?,
                         sistEndretAv = ?
                        where id = any(?)
                        """.trimIndent(),
                    )
                statement.setBoolean(1, skjermet)
                statement.setString(2, Kontekst.get().AppUser.name())
                statement.setArray(3, createArrayOf("bigint", sakIder.toTypedArray()))
                statement.executeUpdate()
            }
        }

    fun hentSakerMedIder(sakIder: List<Long>): List<Sak> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, fnr, enhet, sakType 
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
}
