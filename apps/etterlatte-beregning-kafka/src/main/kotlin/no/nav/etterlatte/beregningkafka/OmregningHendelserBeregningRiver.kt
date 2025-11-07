package no.nav.etterlatte.beregningkafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.omregning.omregningData
import no.nav.etterlatte.rapidsandrivers.BEREGNING_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_FOER
import org.slf4j.LoggerFactory
import tidspunkt.erEtter
import tidspunkt.erFoerEllerPaa
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.abs

internal class OmregningHendelserBeregningRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.TRYGDETID_KOPIERT) {
            precondition { it.forbid(BEREGNING_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(OmregningDataPacket.BEHANDLING_ID) }
            validate { it.requireKey(OmregningDataPacket.FORRIGE_BEHANDLING_ID) }
            validate { it.requireKey(OmregningDataPacket.SAK_TYPE) }
            validate { it.requireKey(OmregningDataPacket.FRA_DATO) }
            validate { it.interestedIn(OmregningDataPacket.INNTEKTSJUSTERING) }
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregninghendelse")
        val omregningData = packet.omregningData
        runBlocking {
            val beregning = beregn(omregningData)
            packet[BEREGNING_KEY] = beregning.beregning

            if (
                omregningData.revurderingaarsak == Revurderingaarsak.REGULERING ||
                omregningData.revurderingaarsak == Revurderingaarsak.AARLIG_INNTEKTSJUSTERING ||
                omregningData.revurderingaarsak == Revurderingaarsak.INNTEKTSENDRING
            ) {
                sendMedInformasjonTilKontrollsjekking(beregning, packet)
            }
        }
        packet.setEventNameForHendelseType(OmregningHendelseType.BEREGNA)
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert omregningshendelse")
    }

    internal suspend fun beregn(omregningData: OmregningData): BeregningOgAvkorting {
        val behandlingId = omregningData.hentBehandlingId()
        val behandlingViOmregnerFra = omregningData.hentForrigeBehandlingid()
        val sakType = omregningData.hentSakType()
        val fraDato = omregningData.hentFraDato()
        val revurderingaarsak = omregningData.revurderingaarsak
        beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)

        if (revurderingaarsak == Revurderingaarsak.REGULERING) {
            beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(behandlingId)
        }
        val beregning = beregningService.beregn(behandlingId).body<BeregningDTO>()
        val forrigeBeregning = beregningService.hentBeregning(behandlingViOmregnerFra).body<BeregningDTO>()

        if (revurderingaarsak == Revurderingaarsak.REGULERING) {
            // TODO denne burde flyttes ut av omregning
            val g = beregningService.hentGrunnbeloep()
            verifiserToleransegrenser(ny = beregning, gammel = forrigeBeregning, g = g, behandlingId = behandlingId)
        }

        return if (sakType == SakType.OMSTILLINGSSTOENAD) {
            val avkorting =
                when (revurderingaarsak) {
                    Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> {
                        beregningService
                            .omregnAarligInntektsjustering(
                                aar = fraDato.year,
                                behandlingId = behandlingId,
                                forrigeBehandlingId = behandlingViOmregnerFra,
                            ).body<AvkortingDto>()
                    }

                    Revurderingaarsak.INNTEKTSENDRING -> {
                        beregningService
                            .omregnMottattInntektsjustering(
                                MottattInntektsjusteringAvkortigRequest(
                                    behandlingId = behandlingId,
                                    virkningstidspunkt = YearMonth.from(omregningData.hentFraDato()),
                                    mottattInntektsjustering = omregningData.hentInntektsjustering(),
                                ),
                            ).body<AvkortingDto>()
                    }

                    else -> {
                        beregningService
                            .omregnAvkorting(behandlingId, behandlingViOmregnerFra)
                            .body<AvkortingDto>()
                    }
                }

            val forrigeAvkorting =
                beregningService
                    .hentAvkorting(behandlingViOmregnerFra)
                    .takeIf { it.status == HttpStatusCode.OK }
                    ?.body<AvkortingDto>()
                    ?: throw IllegalStateException("Forrige behandling $behandlingViOmregnerFra mangler avkorting")
            BeregningOgAvkorting(
                beregning = beregning,
                forrigeBeregning = forrigeBeregning,
                avkorting = avkorting,
                forrigeAvkorting = forrigeAvkorting,
            )
        } else {
            BeregningOgAvkorting(
                beregning = beregning,
                forrigeBeregning = forrigeBeregning,
                avkorting = null,
                forrigeAvkorting = null,
            )
        }
    }

    fun sendMedInformasjonTilKontrollsjekking(
        beregning: BeregningOgAvkorting,
        packet: JsonMessage,
    ) {
        val dato = packet.omregningData.hentFraDato()
        val forrigeBeregning =
            when (val forrigePeriode = beregning.forrigeBeregning.beregningsperioder.paaDato(dato)) {
                null ->
                    beregning.forrigeBeregning.beregningsperioder
                        .minByOrNull { it.datoFOM }
                        ?.takeIf { it.datoFOM.atDay(1) > dato }
                else -> forrigePeriode
            }
        val forrige =
            krevIkkeNull(forrigeBeregning) {
                "Forrige beregning mangler beregningsperiode på dato $dato, og har heller ikke en senere beregningsperiode"
            }.let {
                UtbetaltbeloepOgGrunnbeloep(utbetaltBeloep = it.utbetaltBeloep, grunnbeloep = it.grunnbelop)
            }.also {
                packet[ReguleringEvents.BEREGNING_BELOEP_FOER] = it.utbetaltBeloep
                packet[ReguleringEvents.BEREGNING_G_FOER] = it.grunnbeloep
            }
        val naavaerende =
            krevIkkeNull(beregning.beregning.beregningsperioder.paaDato(dato)) {
                "Beregning mangler beregningsperiode på dato $dato"
            }.let {
                UtbetaltbeloepOgGrunnbeloep(utbetaltBeloep = it.utbetaltBeloep, grunnbeloep = it.grunnbelop)
            }.also {
                packet[ReguleringEvents.BEREGNING_BELOEP_ETTER] = it.utbetaltBeloep
                packet[ReguleringEvents.BEREGNING_G_ETTER] = it.grunnbeloep
            }
        packet[ReguleringEvents.BEREGNING_BRUKT_OMREGNINGSFAKTOR] =
            if (forrige.utbetaltBeloep > 0) {
                BigDecimal(naavaerende.utbetaltBeloep).divide(BigDecimal(forrige.utbetaltBeloep), 14, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ONE
            }

        if (beregning.forrigeAvkorting != null) {
            val forrigeAvkortingBeloep =
                when (val forrigePeriode = beregning.forrigeAvkorting.avkortetYtelse.paaDato(dato)) {
                    null ->
                        beregning.forrigeAvkorting.avkortetYtelse
                            .minByOrNull { it.fom }
                            ?.takeIf { it.fom.atDay(1) > dato }
                    else -> forrigePeriode.avkortingsbeloep
                }
            krevIkkeNull(forrigeAvkortingBeloep) {
                "Forrige beregning mangler avkortingsperiode på dato $dato, og har heller ikke en senere avkortingsperiode"
            }.let { beloep ->
                packet[AVKORTING_FOER] = beloep
            }

            krevIkkeNull(beregning.avkorting?.avkortetYtelse?.paaDato(dato)) {
                "Vi har en tidligere avkorting men har ikke nåværende avkorting"
            }.let {
                packet[AVKORTING_ETTER] = it.avkortingsbeloep
            }
        }
    }

    private fun verifiserToleransegrenser(
        ny: BeregningDTO,
        gammel: BeregningDTO,
        g: Grunnbeloep,
        behandlingId: UUID,
    ) {
        val dato =
            ny.beregningsperioder
                .first()
                .datoFOM
                .atDay(1)
        val sistePeriodeNy =
            krevIkkeNull(ny.beregningsperioder.paaDato(dato)) {
                "Fant ingen beregningsperiode på dato $dato"
            }
        val nyttBeloep = sistePeriodeNy.utbetaltBeloep
        val sistePeriodeGammel = gammel.beregningsperioder.paaDato(dato)
        val gammeltBeloep = sistePeriodeGammel?.utbetaltBeloep
        if (gammeltBeloep == null) {
            logger.debug(
                "Gammelt beløp er null på {} for beregning {}, avbryter toleransegrensesjekk",
                dato,
                gammel.beregningId,
            )
            return
        }
        if (nyttBeloep < gammeltBeloep) {
            throw MindreEnnForrigeBehandling(ny.behandlingId)
        }
        if (gammeltBeloep == 0) {
            logger.warn("Gammelt beløp er 0. Nytt beløp er $nyttBeloep for behandling $behandlingId")
            return
        }
        val endring =
            if (gammeltBeloep > 0) {
                BigDecimal(nyttBeloep).divide(BigDecimal(gammeltBeloep), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ONE
            }
        if (endring >= BigDecimal(1.50)) {
            throw ForStorOekning(ny.behandlingId, endring)
        }
        if (sistePeriodeNy.grunnbelop == sistePeriodeGammel.grunnbelop) {
            logger.debug("Grunnbeløpet er det samme for gammel og ny beregning for behandling {}.", behandlingId)
            return
        }
        verifiserFaktoromregning(g, gammeltBeloep, nyttBeloep, behandlingId)
    }

    private fun verifiserFaktoromregning(
        g: Grunnbeloep,
        gammeltBeloep: Int,
        nyttBeloep: Int,
        behandlingId: UUID,
    ) {
        val forventaNyttBeloep =
            g.omregningsfaktor!!.times(gammeltBeloep.toBigDecimal()).setScale(0, RoundingMode.HALF_UP)
        if (abs(nyttBeloep - forventaNyttBeloep.toInt()) > 1) {
            logger.warn(
                "Noe skurrer for regulering i behandling $behandlingId. " +
                    "Nytt beløp er $nyttBeloep, forventa nytt beløp var $forventaNyttBeloep, " +
                    "omregningsfaktor er ${g.omregningsfaktor}",
            )
        }
    }

    private fun List<Beregningsperiode>.paaDato(dato: LocalDate) =
        filter { it.datoFOM.erFoerEllerPaa(dato) }
            .firstOrNull { it.datoTOM.erEtter(dato) }

    private fun List<AvkortetYtelseDto>.paaDato(dato: LocalDate) =
        filter { it.fom.erFoerEllerPaa(dato) }
            .firstOrNull { it.tom.erEtter(dato) }
}

class MindreEnnForrigeBehandling(
    behandlingId: UUID,
) : ForespoerselException(
        code = "OMREGNING_UTENFOR_TOLERANSEGRENSE_MINDRE",
        detail = "Ny beregning for behandling $behandlingId gir lavere sum enn forrige beregning. Skal ikke skje under omregning.",
        status = HttpStatusCode.ExpectationFailed.value,
    )

class ForStorOekning(
    behandlingId: UUID,
    endring: BigDecimal,
) : ForespoerselException(
        code = "OMREGNING_UTENFOR_TOLERANSEGRENSE_FOR_STOR_OEKNING",
        detail = "Ny beregning for behandling $behandlingId gir for stor økning fra forrige omregning. Endringa var $endring",
        status = HttpStatusCode.ExpectationFailed.value,
    )

data class BeregningOgAvkorting(
    val beregning: BeregningDTO,
    val forrigeBeregning: BeregningDTO,
    val avkorting: AvkortingDto?,
    val forrigeAvkorting: AvkortingDto?,
)

data class UtbetaltbeloepOgGrunnbeloep(
    val utbetaltBeloep: Int,
    val grunnbeloep: Int,
)
