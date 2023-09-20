package no.nav.etterlatte.behandling.kommerbarnettilgode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.util.UUID

class KommerBarnetTilGodeDao(private val connection: () -> Connection) {
    fun lagreKommerBarnetTilGode(kommerBarnetTilGode: KommerBarnetTilgode) {
        if (kommerBarnetTilGode.behandlingId?.let { hentKommerBarnetTilGode(it) } != null) {
            connection().prepareStatement(
                "UPDATE kommerbarnettilgode SET svar = ?, begrunnelse = ?, kilde = ? WHERE behandling_id = ?",
            )
                .also {
                    it.setString(1, kommerBarnetTilGode.svar.name)
                    it.setString(2, kommerBarnetTilGode.begrunnelse)
                    it.setObject(3, kommerBarnetTilGode.kilde.toJson())
                    it.setObject(4, kommerBarnetTilGode.behandlingId)
                    it.executeUpdate()
                }
        } else {
            connection().prepareStatement(
                "INSERT INTO kommerbarnettilgode(behandling_id, svar, begrunnelse, kilde) VALUES(?,?,?,?)",
            ).also {
                it.setObject(1, kommerBarnetTilGode.behandlingId)
                it.setString(2, kommerBarnetTilGode.svar.name)
                it.setString(3, kommerBarnetTilGode.begrunnelse)
                it.setObject(4, kommerBarnetTilGode.kilde.toJson())
                it.executeUpdate()
            }
        }
    }

    fun hentKommerBarnetTilGode(behandlingId: UUID): KommerBarnetTilgode? {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT k.svar, k.begrunnelse, k.kilde, k.behandling_id FROM kommerbarnettilgode k
                    WHERE k.behandling_id = ?
                    """,
            ).also { it.setObject(1, behandlingId) }

        return stmt.executeQuery().singleOrNull {
            KommerBarnetTilgode(
                svar = getString("svar").let { JaNei.valueOf(it) },
                begrunnelse = getString("begrunnelse"),
                kilde = getString("kilde").let { objectMapper.readValue(it) },
                behandlingId = behandlingId,
            )
        }
    }
}
