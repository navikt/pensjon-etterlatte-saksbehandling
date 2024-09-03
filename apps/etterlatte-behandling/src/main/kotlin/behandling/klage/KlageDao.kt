package no.nav.etterlatte.behandling.klage

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.util.UUID

interface KlageDao {
    fun lagreKlage(klage: Klage)

    fun hentKlage(id: UUID): Klage?

    fun hentKlagerISak(sakId: SakId): List<Klage>

    fun oppdaterKabalStatus(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    )
}

class KlageDaoImpl(
    private val connectionAutoclosing: ConnectionAutoclosing,
) : KlageDao {
    override fun lagreKlage(klage: Klage) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO klage(id, sak_id, opprettet, status, kabalstatus, formkrav, utfall, resultat,  innkommende_klage, aarsak_til_avbrytelse, initielt_utfall)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET status = excluded.status, 
                                formkrav = excluded.formkrav, 
                                kabalstatus = excluded.kabalstatus, 
                                utfall = excluded.utfall,
                                initielt_utfall = excluded.initielt_utfall,
                                resultat = excluded.resultat,
                                aarsak_til_avbrytelse = excluded.aarsak_til_avbrytelse
                        """.trimIndent(),
                    )
                statement.setObject(1, klage.id)
                statement.setLong(2, klage.sak.id)
                statement.setTidspunkt(3, klage.opprettet)
                statement.setString(4, klage.status.name)
                statement.setString(5, klage.kabalStatus?.name)
                statement.setJsonb(6, klage.formkrav)
                statement.setJsonb(7, klage.utfall)
                statement.setJsonb(8, klage.resultat)
                statement.setJsonb(9, klage.innkommendeDokument)
                statement.setString(10, klage.aarsakTilAvbrytelse?.name)
                statement.setJsonb(11, klage.initieltUtfall)
                statement.executeUpdate()
            }
        }
    }

    override fun hentKlage(id: UUID): Klage? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT k.id, k.sak_id, s.saktype, s.fnr, s.enhet, k.opprettet, k.status, 
                            kabalstatus, formkrav, utfall, resultat, kabalresultat, innkommende_klage, aarsak_til_avbrytelse, initielt_utfall
                        FROM klage k INNER JOIN sak s on k.sak_id = s.id
                        WHERE k.id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, id)
                statement.executeQuery().singleOrNull {
                    somKlage()
                }
            }
        }

    override fun hentKlagerISak(sakId: SakId): List<Klage> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT k.id, k.sak_id, s.saktype, s.fnr, s.enhet, k.opprettet, k.status, 
                            kabalstatus, formkrav, utfall, resultat, kabalresultat, innkommende_klage, aarsak_til_avbrytelse, initielt_utfall
                        FROM klage k INNER JOIN sak s on k.sak_id = s.id
                        WHERE s.id = ?
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId)
                statement.executeQuery().toList {
                    somKlage()
                }
            }
        }

    override fun oppdaterKabalStatus(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE klage
                        SET kabalstatus = ?, kabalresultat = ?
                        WHERE id = ?
                        """.trimIndent(),
                    )
                statement.setString(1, kabalrespons.kabalStatus.name)
                statement.setString(2, kabalrespons.resultat.name)
                statement.setObject(3, klageId)
                statement.executeUpdate()
            }
        }
    }

    private fun ResultSet.somKlage(): Klage =
        Klage(
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
            resultat = getString("resultat")?.let { objectMapper.readValue(it) },
            kabalResultat = getString("kabalresultat")?.let { enumValueOf<BehandlingResultat>(it) },
            innkommendeDokument = getString("innkommende_klage")?.let { objectMapper.readValue(it) },
            aarsakTilAvbrytelse = getString("aarsak_til_avbrytelse")?.let { enumValueOf<AarsakTilAvbrytelse>(it) },
            initieltUtfall = getString("initielt_utfall")?.let { objectMapper.readValue(it) },
        )
}
