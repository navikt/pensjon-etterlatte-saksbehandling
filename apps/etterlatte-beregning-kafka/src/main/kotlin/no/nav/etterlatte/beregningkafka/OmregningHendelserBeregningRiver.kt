package no.nav.etterlatte.beregningkafka

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.BEREGNING_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_ETTER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.AVKORTING_FOER
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory
import tidspunkt.erEtter
import tidspunkt.erFoerEllerPaa
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs

internal class OmregningHendelserBeregningRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.TRYGDETID_KOPIERT) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_TYPE) }
            validate { it.rejectKey(BEREGNING_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
            validate { it.requireKey(DATO_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregninghendelse")
        val behandlingId = packet.behandlingId
        val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()
        val sakType = objectMapper.treeToValue<SakType>(packet[SAK_TYPE])
        runBlocking {
            val beregning = beregn(sakType, behandlingId, behandlingViOmregnerFra, packet.dato)
            packet[BEREGNING_KEY] = beregning.beregning
            sendMedInformasjonTilKontrollsjekking(beregning, packet)
        }
        packet.setEventNameForHendelseType(ReguleringHendelseType.BEREGNA)
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert omregningshendelse")
    }

    internal suspend fun beregn(
        sakType: SakType,
        behandlingId: UUID,
        behandlingViOmregnerFra: UUID,
        dato: LocalDate,
    ): BeregningOgAvkorting {
        val g = beregningService.hentGrunnbeloep()
        beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)
        beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(behandlingId)
        val beregning = beregningService.beregn(behandlingId).body<BeregningDTO>()
        val forrigeBeregning = beregningService.hentBeregning(behandlingViOmregnerFra).body<BeregningDTO>()

        verifiserToleransegrenser(ny = beregning, gammel = forrigeBeregning, g = g, behandlingId = behandlingId)

        return if (sakType == SakType.OMSTILLINGSSTOENAD) {
            val avkorting =
                beregningService
                    .regulerAvkorting(behandlingId, behandlingViOmregnerFra)
                    .body<AvkortingDto>()
            val forrigeAvkorting =
                beregningService
                    .hentAvkorting(behandlingViOmregnerFra)
                    .takeIf { it.status == HttpStatusCode.OK }
                    ?.body<AvkortingDto>() ?: throw IllegalStateException("Forrige behandling $behandlingViOmregnerFra mangler avkorting")
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

    private fun sendMedInformasjonTilKontrollsjekking(
        beregning: BeregningOgAvkorting,
        packet: JsonMessage,
    ) {
        val forrige =
            requireNotNull(beregning.forrigeBeregning.beregningsperioder.paaDato(packet.dato))
                .let {
                    Pair(it.utbetaltBeloep, it.grunnbelop)
                }.also {
                    packet[ReguleringEvents.BEREGNING_BELOEP_FOER] = it.first
                    packet[ReguleringEvents.BEREGNING_G_FOER] = it.second
                }
        val naavaerende =
            requireNotNull(beregning.beregning.beregningsperioder.paaDato(packet.dato))
                .let {
                    Pair(it.utbetaltBeloep, it.grunnbelop)
                }.also {
                    packet[ReguleringEvents.BEREGNING_BELOEP_ETTER] = it.first
                    packet[ReguleringEvents.BEREGNING_G_ETTER] = it.second
                }
        packet[ReguleringEvents.BEREGNING_BRUKT_OMREGNINGSFAKTOR] =
            BigDecimal(naavaerende.first).divide(BigDecimal(forrige.first))

        beregning.forrigeAvkorting?.avkortetYtelse?.paaDato(packet.dato)?.let {
            packet[AVKORTING_FOER] = it.avkortingsbeloep
        }
        beregning.avkorting?.avkortetYtelse?.paaDato(packet.dato)?.let {
            packet[AVKORTING_ETTER] = it.avkortingsbeloep
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
        val sistePeriodeNy = requireNotNull(ny.beregningsperioder.paaDato(dato))
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
        val endring = BigDecimal(nyttBeloep).divide(BigDecimal(gammeltBeloep), 2, RoundingMode.HALF_UP)
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
