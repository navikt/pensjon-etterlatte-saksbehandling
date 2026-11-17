package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.regler.RegelPeriode
import tools.jackson.databind.JsonNode
import java.util.UUID
import javax.sql.DataSource

class AnvendtTrygdetidRepository(
    private val dataSource: DataSource,
) {
    fun lagreAnvendtTrygdetid(
        behandlingId: UUID,
        utrekning: AnvendtTrygdetidPeriodeUtrekning,
    ) = lagreAnvendtTrygdetid(
        behandlingId,
        utrekning.utrekning.map {
            AnvendtTrygdetidRegelresultat(it.periode, it.resultat.toJsonNode(), it.reglerVersjon)
        },
        utrekning.anvendt,
    )

    fun lagreAnvendtTrygdetid(
        behandlingId: UUID,
        utrekning: List<AnvendtTrygdetidRegelresultat>,
        anvendt: List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>>,
    ) = dataSource.transaction { tx ->
        tx.oppdater(
            query =
                """insert into anvendt_trygdetid (behandling_id, foer_kombinering, etter_kombinering) 
                    values(:behandling_id::uuid, :foer_kombinering::jsonb, :etter_kombinering::jsonb)
                """.trimMargin(),
            params =
                mapOf(
                    "behandling_id" to behandlingId.toString(),
                    "foer_kombinering" to utrekning.toJson(),
                    "etter_kombinering" to anvendt.toJson(),
                ),
            loggtekst = "Lagrer anvendt trygdetid-utrekninga",
        )
    }
}

data class AnvendtTrygdetidRegelresultat(
    val periode: RegelPeriode,
    val resultat: JsonNode,
    val reglerVersjon: String,
)
