package no.nav.etterlatte.behandling.kommerbarnettilgode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.util.UUID

class KommerBarnetTilGodeDao(private val connection: () -> Connection) {
    fun lagreKommerBarnetTilGode(kommerBarnetTilGode: KommerBarnetTilgode) {
        if (kommerBarnetTilGode.behandlingId?.let { hentKommerBarnetTilGode(it) } != null) {
            connection().prepareStatement("UPDATE kommerbarnettilgode SET svar = ?, begrunnelse = ?, kilde = ?")
                .also { it.setString(1, kommerBarnetTilGode.svar.name) }
                .also { it.setString(2, kommerBarnetTilGode.begrunnelse) }
                .also { it.setObject(3, kommerBarnetTilGode.kilde.toJson()) }
                .also { it.executeUpdate() }
        } else {
            val statement =
                connection().prepareStatement(
                    "INSERT INTO kommerbarnettilgode(behandling_id, svar, begrunnelse, kilde) VALUES(?,?,?,?)"
                )
            statement.setObject(1, kommerBarnetTilGode.behandlingId)
            statement.setString(2, kommerBarnetTilGode.svar.name)
            statement.setString(3, kommerBarnetTilGode.begrunnelse)
            statement.setObject(4, kommerBarnetTilGode.kilde.toJson())
            statement.executeUpdate()
        }
    }

    fun hentKommerBarnetTilGode(behandlingId: UUID): KommerBarnetTilgode? {
        val stmt =
            connection().prepareStatement(
                """
                    $kommerbarnettilgode
                    WHERE k.behandling_id = ?
                    """
            ).also { it.setObject(1, behandlingId) }

        return stmt.executeQuery().singleOrNull {
            KommerBarnetTilgode(
                svar = getString("svar").let { objectMapper.readValue(it) },
                begrunnelse = getString("begrunnelse"),
                kilde = getString("kilde").let { objectMapper.readValue(it) },
                behandlingId = behandlingId
            )
        }
    }

    private val kommerbarnettilgode =
        "SELECT k.svar, k.begrunnelse, k.kilde, k.behandling_id FROM kommerbarnettilgode k"
}