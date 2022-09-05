package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import java.time.Instant
import java.util.*

data class GrunnlagResult(val response: String)

class GrunnlagService(
    private val behandlingKlient: BehandlingKlient,
    private val rapid: KafkaProdusent<String, String>,
    private val grunnlagKlient: EtterlatteGrunnlag
) {

    suspend fun lagreAvdoedMedlemskapPeriode(
        behandlingId: String,
        periode: AvdoedesMedlemskapsperiode,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
/*        val grunnlag: Grunnlagsopplysning<AvdoedesMedlemskapsperiode>? = grunnlagKlient.finnPersonOpplysning(
            behandling.sak,
            Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
            token
        ).let { setOpplysningType(it) }*/

        val opplysning: List<Grunnlagsopplysning<out Any>> = listOf(
            lagOpplysning(
                Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
                Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
                periode
            )
        )

        rapid.publiser(
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf("opplysning" to opplysning, "sakId" to behandling.sak)
            ).toJson()
        )

        return GrunnlagResult("Lagret")
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
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf("opplysning" to opplysning, "sakId" to behandling.sak)
            ).toJson()
        )
        return GrunnlagResult("Lagret")
    }

    internal data class Beregningsgrunnlag(val beregningsgrunnlag: List<SoeskenMedIBeregning>)

    suspend fun lagreSoeskenMedIBeregning(
        behandlingId: String,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val opplysning: List<Grunnlagsopplysning<Beregningsgrunnlag>> = listOf(
            lagOpplysning(
                opplysningsType = Opplysningstyper.SAKSBEHANDLER_SOESKEN_I_BEREGNINGEN,
                kilde = Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
                opplysning = Beregningsgrunnlag(soeskenMedIBeregning)
            )
        )

        rapid.publiser(
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf(
                    "opplysning" to opplysning,
                    "sakId" to behandling.sak
                )
            ).toJson()
        )

        return GrunnlagResult("Lagret")
    }

    companion object {
        inline fun <reified T> setOpplysningType(
            opplysning: Grunnlagsopplysning<ObjectNode>?
        ): Grunnlagsopplysning<T>? {
            return opplysning?.let {
                Grunnlagsopplysning(
                    opplysning.id,
                    opplysning.kilde,
                    opplysning.opplysningType,
                    opplysning.meta,
                    objectMapper.readValue(opplysning.opplysning.toString())
                )
            }
        }
    }
}

fun <T> lagOpplysning(
    opplysningsType: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(UUID.randomUUID(), kilde, opplysningsType, objectMapper.createObjectNode(), opplysning)
}