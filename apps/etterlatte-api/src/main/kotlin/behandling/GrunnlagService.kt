package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
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

    /**
     * Legger til en ny periode i listen over saksbehandlerperioder. Dersom det finnes en periode med samme id fra før av,
     * vil den bli erstattet med den nye perioden.
     */
    suspend fun upsertAvdoedMedlemskapPeriode(
        behandlingId: String,
        periode: SaksbehandlerMedlemskapsperiode,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val grunnlag: Grunnlagsopplysning<SaksbehandlerMedlemskapsperioder>? = grunnlagKlient.finnPerioder(
            behandling.sak,
            Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
            token
        )

        val grunnlagMedNyPeriode = grunnlag?.opplysning?.perioder
            ?.filter { it.id != periode.id }
            ?.plus(periode) ?: listOf(periode)
        val opplysning = lagAvdoedOpplysning(saksbehandlerId, grunnlagMedNyPeriode)

        rapid.publiser(
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf("opplysning" to opplysning, "sakId" to behandling.sak)
            ).toJson()
        )

        return GrunnlagResult("Lagret")
    }

    suspend fun slettAvdoedMedlemskapPeriode(
        behandlingId: String,
        saksbehandlerPeriodeId: String,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val grunnlag: Grunnlagsopplysning<SaksbehandlerMedlemskapsperioder>? = grunnlagKlient.finnPerioder(
            behandling.sak,
            Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
            token
        )

        val grunnlagUtenPeriode =
            grunnlag?.opplysning?.perioder?.filter { it.id != saksbehandlerPeriodeId } ?: emptyList()
        val opplysning = lagAvdoedOpplysning(saksbehandlerId, grunnlagUtenPeriode)

        rapid.publiser(
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf("opplysning" to opplysning, "sakId" to behandling.sak)
            ).toJson()
        )

        return GrunnlagResult("Slettet")
    }

    private fun lagAvdoedOpplysning(saksbehandlerId: String, perioder: List<SaksbehandlerMedlemskapsperiode>) = listOf(
        lagOpplysning(
            Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
            Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
            SaksbehandlerMedlemskapsperioder(perioder)
        )
    )

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

    suspend fun lagreSoeskenMedIBeregning(
        behandlingId: String,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val opplysning: List<Grunnlagsopplysning<Beregningsgrunnlag>> = listOf(
            lagOpplysning(
                opplysningsType = Opplysningstyper.SOESKEN_I_BEREGNINGEN,
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
    opplysningsType: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(UUID.randomUUID(), kilde, opplysningsType, objectMapper.createObjectNode(), opplysning)
}