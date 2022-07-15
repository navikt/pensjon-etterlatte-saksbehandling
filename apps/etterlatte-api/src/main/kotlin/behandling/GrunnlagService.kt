package no.nav.etterlatte.behandling

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
    private val grunnlagKlient: GrunnlagKlient
) {

    suspend fun lagreResultatKommerBarnetTilgode(behandlingId: String, svar: String, begrunnelse: String, saksbehandlerId: String, token: String): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val opplysning = byggOpplysninger(svar, begrunnelse, saksbehandlerId)

        grunnlagKlient.lagreResultatKommerBarnetTilgode(behandling.sak.toInt(), behandling.id.toString(), opplysning, token)
        return GrunnlagResult("Lagret")
    }
}


fun byggOpplysninger(svar: String, begrunnelse: String, saksbehandlerId: String): List<Grunnlagsopplysning<out Any>> {
    val opplysninger = ArrayList<Grunnlagsopplysning<out Any>>()

    opplysninger.add(lagOpplysning(
        Opplysningstyper.SAKSBEHANDLER_KOMMER_BARNET_TILGODE_V1,
        Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
        ResultatKommerBarnetTilgode(svar, begrunnelse)
    ))

    return opplysninger
}


fun <T> lagOpplysning(opplysningsType: Opplysningstyper, kilde: Grunnlagsopplysning.Kilde, opplysning: T): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        kilde,
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}
