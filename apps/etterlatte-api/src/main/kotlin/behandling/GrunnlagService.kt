package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import java.time.Instant
import java.time.YearMonth
import java.util.*

data class GrunnlagResult(val response: String)

class GrunnlagService(
    private val behandlingKlient: BehandlingKlient,
    private val rapid: KafkaProdusent<String, String>,
    private val grunnlagKlient: EtterlatteGrunnlag
) {

    suspend fun upsertAvdoedMedlemskapPeriode(
        behandlingId: String,
        periode: AvdoedesMedlemskapsperiode,
        saksbehandlerId: String,
        token: String
    ): GrunnlagResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        val avdoedFnr = behandling.avdoed?.singleOrNull()
            ?: throw RuntimeException("Flere avdøde oppdaget på sak ${behandling.sak} ved upserting av opplysning") // TODO sj: Håndterer ikke flere avdøde
        val periodisertOpplysning =
            lagOpplysning(
                opplysningsType = Opplysningstype.MEDLEMSKAPSPERIODE,
                kilde = Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
                opplysning = periode,
                fnr = Foedselsnummer.of(avdoedFnr),
                periode = Periode(YearMonth.from(periode.fraDato), YearMonth.from(periode.tilDato))
            )

        rapid.publiser(
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf("opplysning" to periodisertOpplysning, "sakId" to behandling.sak)
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
        val avdoedFnr = behandling.avdoed?.singleOrNull()
            ?: throw RuntimeException("Flere avdøde oppdaget på sak ${behandling.sak} ved sletting av opplysning") // TODO sj: Håndterer ikke flere avdøde
        val grunnlag: Grunnlagsopplysning<SaksbehandlerMedlemskapsperioder>? = grunnlagKlient.finnPerioder(
            behandling.sak,
            Opplysningstype.MEDLEMSKAPSPERIODE,
            token
        )

        val grunnlagUtenPeriode = grunnlag?.opplysning?.perioder?.singleOrNull { it.id != saksbehandlerPeriodeId }
            ?: throw RuntimeException("Fant ikke opplysning med id $saksbehandlerPeriodeId på sak ${behandling.sak}")

        val periodisertOpplysning =
            lagOpplysning(
                opplysningsType = Opplysningstype.MEDLEMSKAPSPERIODE,
                kilde = Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
                opplysning = null,
                fnr = Foedselsnummer.of(avdoedFnr),
                periode = Periode(
                    YearMonth.from(grunnlagUtenPeriode.fraDato),
                    YearMonth.from(grunnlagUtenPeriode.tilDato)
                )
            )

        rapid.publiser(
            behandlingId,
            JsonMessage.newMessage(
                eventName = "OPPLYSNING:NY",
                map = mapOf("opplysning" to periodisertOpplysning, "sakId" to behandling.sak)
            ).toJson()
        )

        return GrunnlagResult("Slettet")
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
                Opplysningstype.KOMMER_BARNET_TILGODE,
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