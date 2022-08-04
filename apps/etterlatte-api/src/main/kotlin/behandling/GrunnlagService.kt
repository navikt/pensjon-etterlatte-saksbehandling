package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import java.time.Instant
import java.util.*


data class GrunnlagResult(val response: String)

class GrunnlagService(
    private val grunnlagKlient: EtterlatteGrunnlag,
    private val behandlingKlient: BehandlingKlient,
    private val rapid: KafkaProdusent<String, String>
) {
    suspend fun finnOpplysning(
        sakId: String,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<ObjectNode> {
        return grunnlagKlient.finnOpplysning(sakId, opplysningsType, accessToken)
    }

    suspend fun lagreResultatKommerBarnetTilgode(
        behandlingId: String,
        svar: String,
        begrunnelse: String,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)

        val opplysning: List<Grunnlagsopplysning<out Any>> = listOf(
            lagOpplysning(
                Opplysningstyper.SAKSBEHANDLER_KOMMER_BARNET_TILGODE_V1,
                Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
                ResultatKommerBarnetTilgode(svar, begrunnelse)
            )
        )

        rapid.publiser(
            behandlingId, JsonMessage.newMessage(
                mapOf(
                    eventNameKey to "OPPLYSNING:NY",
                    "opplysning" to opplysning,
                    "sakId" to behandling.sak,
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
