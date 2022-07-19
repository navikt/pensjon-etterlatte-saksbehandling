package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList


data class GrunnlagResult(val response: String)

class GrunnlagService(
    private val behandlingKlient: BehandlingKlient,
    private val rapid: KafkaProdusent<String, String>
) {

    suspend fun lagreResultatKommerBarnetTilgode(
        behandlingId: String,
        svar: String,
        begrunnelse: String,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)

        val opplysning : List<Grunnlagsopplysning<out Any>> = listOf(lagOpplysning(
            Opplysningstyper.SAKSBEHANDLER_KOMMER_BARNET_TILGODE_V1,
            Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
            ResultatKommerBarnetTilgode(svar, begrunnelse)
        ))

        rapid.publiser(behandlingId, JsonMessage.newMessage(
            mapOf(
                "opplysning" to opplysning,
                "sak" to behandling.sak,
            )
        ).toJson()
        )
        return GrunnlagResult("Lagret")
    }
}

fun <T> lagOpplysning(
    opplysningsType: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(UUID.randomUUID(), kilde, opplysningsType, objectMapper.createObjectNode(), opplysning)
}
