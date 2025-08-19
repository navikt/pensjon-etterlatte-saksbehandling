package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.ETTEROPPGJOER_RESULTAT_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.ETTEROPPGJOER_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatistikkDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.statistikk.service.StatistikkService
import org.slf4j.LoggerFactory

class EtteroppgjoerHendelseRiver(
    rapidsConnection: RapidsConnection,
    private val statistikkService: StatistikkService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(EtteroppgjoerHendelseRiver::class.java)

    private val etteroppgjoerHendelser = EtteroppgjoerHendelseType.entries.map { it.lagEventnameForType() }

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            precondition { it.requireAny(EVENT_NAME_KEY, etteroppgjoerHendelser) }
            validate { it.requireKey(ETTEROPPGJOER_STATISTIKK_RIVER_KEY) }
            validate { it.interestedIn(TEKNISK_TID_KEY) }
            validate { it.interestedIn(ETTEROPPGJOER_RESULTAT_RIVER_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            val hendelse: EtteroppgjoerHendelseType = enumValueOf(packet[EVENT_NAME_KEY].textValue().split(":")[1])
            val tekniskTid = parseTekniskTid(packet, logger)
            val statistikkDto: EtteroppgjoerForbehandlingStatistikkDto =
                objectMapper.treeToValue(packet[ETTEROPPGJOER_STATISTIKK_RIVER_KEY])
            val resultat: BeregnetEtteroppgjoerResultatDto? =
                when (val resultatNode = packet[ETTEROPPGJOER_RESULTAT_RIVER_KEY]) {
                    is NullNode -> null
                    else -> objectMapper.treeToValue(resultatNode)
                }

            statistikkService.registrerEtteroppgjoerHendelse(
                hendelse = hendelse,
                statistikkDto = statistikkDto,
                tekniskTid = tekniskTid.toTidspunkt(),
                resultat = resultat,
            )
            logger.info("Registrerte statistikk på etteroppgjør for pakke med korrelasjonsid ${packet.correlationId}")
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke registrere statistikk for etteroppgjør i pakken med korrelasjonsid" +
                    "=${packet.correlationId}. Dette blokkerer lesing av statistikk og må sees på snarest!",
                e,
            )
            throw e
        }
    }
}
