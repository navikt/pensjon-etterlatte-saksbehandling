package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.util.*

data class GrunnlagResult(val response: String)

class GrunnlagService(
    private val behandlingKlient: BehandlingKlient,
    private val rapid: KafkaProdusent<String, String>
) {

    suspend fun lagreSoeskenMedIBeregning(
        behandlingId: String,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val opplysning: List<Grunnlagsopplysning<Beregningsgrunnlag>> = listOf(
            lagOpplysning(
                opplysningsType = Opplysningstype.SOESKEN_I_BEREGNINGEN,
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
}

fun <T> lagOpplysning(
    opplysningsType: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T,
    fnr: Foedselsnummer? = null,
    periode: Periode? = null
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = kilde,
        opplysningType = opplysningsType,
        meta = objectMapper.createObjectNode(),
        opplysning = opplysning,
        fnr = fnr,
        periode = periode
    )
}