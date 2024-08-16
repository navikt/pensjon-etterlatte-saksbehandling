package no.nav.etterlatte.beregning

import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import java.util.UUID
import javax.sql.DataSource

class AnvendtTrygdetidRepository(
    private val dataSource: DataSource,
) {
    fun lagreAnvendtTrygdetid(
        behandlingId: UUID,
        utrekning: AnvendtTrygdetidPeriodeUtrekning,
    ) = dataSource.transaction { tx ->
        tx.oppdater(
            query =
                """insert into anvendt_trygdetid (behandling_id, foer_kombinering, etter_kombinering) 
                    values(:behandling_id, :foer_kombinering, :etter_kombinering)
                """.trimMargin(),
            params =
                mapOf(
                    "behandling_id" to behandlingId.toString(),
                    "foer_kombinering" to utrekning.utrekning.toJson(),
                    "etter_kombinering" to utrekning.anvendt.toJson(),
                ),
            loggtekst = "Lagrer anvendt trygdetid-utrekninga",
        )
    }
}
