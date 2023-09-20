package no.nav.etterlatte.behandling.klage

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

interface KlageDao {
    fun lagreKlage(klage: Klage)

    fun hentKlage(id: UUID): Klage?

    fun hentKlagerISak(sakId: Long): List<Klage>

    fun oppdaterKabalStatus(
        sakId: Long,
        kabalrespons: Kabalrespons,
    )
}

class KlageDaoImpl(private val connection: () -> Connection) : KlageDao {
    override fun lagreKlage(klage: Klage) {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO klage(id, sak_id, opprettet, status, kabalstatus, formkrav, utfall)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET status = excluded.status, 
                            formkrav = excluded.formkrav, 
                            kabalstatus = excluded.kabalstatus, 
                            utfall = excluded.utfall
                    """.trimIndent(),
                )
            statement.setObject(1, klage.id)
            statement.setLong(2, klage.sak.id)
            statement.setTidspunkt(3, klage.opprettet)
            statement.setString(4, klage.status.name)
            statement.setString(5, klage.kabalStatus?.name)
            statement.setJsonb(6, klage.formkrav)
            statement.setJsonb(7, klage.utfall)
            statement.executeUpdate()
        }
    }

    override fun hentKlage(id: UUID): Klage? {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT k.id, k.sak_id, saktype, fnr, enhet, opprettet, status, 
                        kabalstatus, formkrav, utfall
                    FROM klage k INNER JOIN sak s on k.sak_id = s.id
                    WHERE k.id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, id)
            return statement.executeQuery().singleOrNull {
                somKlage()
            }
        }
    }

    override fun hentKlagerISak(sakId: Long): List<Klage> {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT k.id, k.sak_id, saktype, fnr, enhet, opprettet, status, 
                        kabalstatus, formkrav, utfall
                    FROM klage k INNER JOIN sak s on k.sak_id = s.id
                    WHERE s.id = ?
                    """.trimIndent(),
                )
            statement.setLong(1, sakId)
            return statement.executeQuery().toList {
                somKlage()
            }
        }
    }

    override fun oppdaterKabalStatus(
        sakId: Long,
        kabalrespons: Kabalrespons,
    ) {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    UPDATE klage
                    SET kabalstatus = ?, kabalresultat = ?
                    WHERE sak_id = ?
                    """.trimIndent(),
                )
            statement.setString(1, kabalrespons.kabalStatus.name)
            statement.setString(2, kabalrespons.resultat.name)
            statement.setObject(3, sakId)
            statement.executeUpdate()
        }
    }

    private fun ResultSet.somKlage(): Klage {
        return Klage(
            id = getString("id").let { UUID.fromString(it) },
            sak =
                Sak(
                    ident = getString("fnr"),
                    sakType = enumValueOf(getString("saktype")),
                    id = getLong("sak_id"),
                    enhet = getString("enhet"),
                ),
            opprettet = getTidspunkt("opprettet"),
            status = enumValueOf(getString("status")),
            kabalStatus = getString("kabalstatus")?.let { enumValueOf<KabalStatus>(it) },
            formkrav = getString("formkrav")?.let { objectMapper.readValue(it) },
            utfall = getString("utfall")?.let { objectMapper.readValue(it) },
        )
    }
}
