package no.nav.etterlatte.behandling.sjekkliste

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class SjekklisteDao(private val connection: () -> Connection) {
    fun opprettSjekkliste(
        behandlingId: UUID,
        sjekklisteItems: List<String>,
    ) {
        val brukernavn = Kontekst.get().AppUser.name()

        connection().prepareStatement(
            """
            INSERT INTO sjekkliste (ID, OPPRETTET_AV, ENDRET_AV)
             VALUES(?, ?, ?)
            """.trimIndent(),
        ).apply {
            setObject(1, behandlingId)
            setString(2, brukernavn)
            setString(3, brukernavn)
        }.executeUpdate()

        if (sjekklisteItems.isNotEmpty()) {
            connection().prepareStatement(
                """
                INSERT INTO sjekkliste_item (SJEKKLISTE, BESKRIVELSE, OPPRETTET_AV, ENDRET_AV) 
                VALUES(?, ?, ?, ?)
                """.trimIndent(),
            ).apply {
                sjekklisteItems.forEach {
                    setObject(1, behandlingId)
                    setString(2, it)
                    setString(3, brukernavn)
                    setString(4, brukernavn)
                    addBatch()
                }
            }.executeBatch()
        }
    }

    fun hentSjekkliste(id: UUID): Sjekkliste? {
        val stmt =
            connection().prepareStatement(
                """
                SELECT id, 
                 kommentar,
                 adresse_brev,
                 kontonr_reg,
                 versjon
                FROM sjekkliste
                WHERE id = ?
                """.trimIndent(),
            )
        stmt.setObject(1, id)
        return stmt.executeQuery().toList {
            Sjekkliste(
                id = getUUID("id"),
                kommentar = getString("kommentar"),
                adresseForBrev = getString("adresse_brev"),
                kontonrRegistrert = getString("kontonr_reg"),
                sjekklisteItems = hentSjekklisteItems(id),
                versjon = getLong("versjon"),
            )
        }.firstOrNull()
    }

    private fun hentSjekklisteItems(sjekklisteId: UUID): List<SjekklisteItem> {
        val stmt =
            connection().prepareStatement(
                """
                SELECT id, 
                 beskrivelse,
                 avkrysset,
                 versjon
                FROM sjekkliste_item
                WHERE sjekkliste = ?
                """.trimIndent(),
            )
        stmt.setObject(1, sjekklisteId)
        return stmt.executeQuery().toList { sjekklisteItem() }.sortedBy { it.id }
    }

    fun hentSjekklisteItem(sjekklisteItemId: Long): SjekklisteItem {
        val stmt =
            connection().prepareStatement(
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
        return stmt.executeQuery().toList { sjekklisteItem() }
            .also { if (it.isEmpty()) throw IllegalStateException("Fant ingen sjekklisterad id=$sjekklisteItemId") }
            .first()
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
    ) {
        val stmt =
            connection().prepareStatement(
                """
                UPDATE sjekkliste_item 
                 SET avkrysset = ?,
                     endret = CURRENT_TIMESTAMP,
                     endret_av = ?,
                     versjon = versjon + 1
                WHERE id = ?
                """.trimIndent(),
            )
        stmt.setObject(1, oppdatering.avkrysset)
        stmt.setObject(2, Kontekst.get().AppUser.name())
        stmt.setObject(3, itemId)
        stmt.executeUpdate().also {
            if (it != 1) {
                throw IllegalStateException("Feil under oppdatering av sjekklisterad $itemId")
            }
        }
    }
}
