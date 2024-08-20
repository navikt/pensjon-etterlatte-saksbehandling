package no.nav.etterlatte.behandling.sjekkliste

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.util.UUID

class SjekklisteDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun opprettSjekkliste(
        behandlingId: UUID,
        sjekklisteItems: List<String>,
    ) {
        val brukernavn = Kontekst.get().AppUser.name()

        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    INSERT INTO sjekkliste (id, opprettet_av, endret_av)
                     VALUES(?, ?, ?)
                    """.trimIndent(),
                ).apply {
                    setObject(1, behandlingId)
                    setString(2, brukernavn)
                    setString(3, brukernavn)
                }.executeUpdate()
            }
        }

        if (sjekklisteItems.isNotEmpty()) {
            connectionAutoclosing.hentConnection {
                with(it) {
                    prepareStatement(
                        """
                        INSERT INTO sjekkliste_item (sjekkliste_id, beskrivelse, opprettet_av, endret_av) 
                        VALUES(?, ?, ?, ?)
                        """.trimIndent(),
                    ).apply {
                        sjekklisteItems.forEach { sjekklisteItem ->
                            setObject(1, behandlingId)
                            setString(2, sjekklisteItem)
                            setString(3, brukernavn)
                            setString(4, brukernavn)
                            addBatch()
                        }
                    }.executeBatch()
                }
            }
        }
    }

    fun oppdaterSjekkliste(
        sjekklisteId: UUID,
        oppdatering: OppdatertSjekkliste,
    ) = connectionAutoclosing.hentConnection { connection ->
        with(connection) {
            val stmt =
                prepareStatement(
                    """
                    UPDATE sjekkliste 
                     SET kommentar = ?,
                         kontonr_reg = ?,
                         onsket_skattetrekk = ?,
                         bekreftet = ?,
                         endret = CURRENT_TIMESTAMP,
                         endret_av = ?,
                         versjon = versjon + 1
                    WHERE id = ?
                    """.trimIndent(),
                )
            stmt.setString(1, oppdatering.kommentar)
            stmt.setString(2, oppdatering.kontonrRegistrert)
            stmt.setString(3, oppdatering.onsketSkattetrekk)
            stmt.setBoolean(4, oppdatering.bekreftet)
            stmt.setString(5, Kontekst.get().AppUser.name())
            stmt.setObject(6, sjekklisteId)
            stmt.executeUpdate().also {
                if (it != 1) {
                    throw IllegalStateException("Feil under oppdatering av sjekkliste $sjekklisteId")
                }
            }
        }
    }

    fun hentSjekkliste(id: UUID): Sjekkliste? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, 
                         kommentar,
                         kontonr_reg,
                         onsket_skattetrekk,
                         bekreftet,
                         versjon
                        FROM sjekkliste
                        WHERE id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, id)
                stmt
                    .executeQuery()
                    .toList {
                        Sjekkliste(
                            id = getUUID("id"),
                            kommentar = getString("kommentar"),
                            kontonrRegistrert = getString("kontonr_reg"),
                            onsketSkattetrekk = getString("onsket_skattetrekk"),
                            bekreftet = getBoolean("bekreftet"),
                            sjekklisteItems = hentSjekklisteItems(id),
                            versjon = getLong("versjon"),
                        )
                    }.firstOrNull()
            }
        }

    private fun hentSjekklisteItems(sjekklisteId: UUID): List<SjekklisteItem> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, 
                         beskrivelse,
                         avkrysset,
                         versjon
                        FROM sjekkliste_item
                        WHERE sjekkliste_id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, sjekklisteId)
                stmt.executeQuery().toList { sjekklisteItem() }.sortedBy { it.id }
            }
        }

    fun hentSjekklisteItem(sjekklisteItemId: Long): SjekklisteItem =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, 
                         beskrivelse,
                         avkrysset,
                         versjon
                        FROM sjekkliste_item
                        WHERE id = ?
                        """.trimIndent(),
                    )
                stmt.setObject(1, sjekklisteItemId)
                stmt
                    .executeQuery()
                    .toList { sjekklisteItem() }
                    .also { if (it.isEmpty()) throw IllegalStateException("Fant ingen sjekklisterad id=$sjekklisteItemId") }
                    .first()
            }
        }

    private fun ResultSet.sjekklisteItem() =
        SjekklisteItem(
            id = getLong("id"),
            beskrivelse = getString("beskrivelse"),
            avkrysset = getBoolean("avkrysset"),
            versjon = getLong("versjon"),
        )

    fun oppdaterSjekklisteItem(
        itemId: Long,
        oppdatering: OppdaterSjekklisteItem,
    ) = connectionAutoclosing.hentConnection { connection ->
        with(connection) {
            val stmt =
                prepareStatement(
                    """
                    UPDATE sjekkliste_item 
                     SET avkrysset = ?,
                         endret = CURRENT_TIMESTAMP,
                         endret_av = ?,
                         versjon = versjon + 1
                    WHERE id = ?
                    """.trimIndent(),
                )
            stmt.setBoolean(1, oppdatering.avkrysset)
            stmt.setString(2, Kontekst.get().AppUser.name())
            stmt.setObject(3, itemId)
            stmt.executeUpdate().also {
                if (it != 1) {
                    throw IllegalStateException("Feil under oppdatering av sjekklisterad $itemId")
                }
            }
        }
    }
}
