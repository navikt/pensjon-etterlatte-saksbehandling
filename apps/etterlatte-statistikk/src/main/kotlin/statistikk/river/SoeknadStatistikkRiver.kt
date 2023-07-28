package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_KRITERIER_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.GYLDIG_FOR_BEHANDLING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SOEKNAD_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.statistikk.service.SoeknadStatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.migrering.ListenerMedLogging

class SoeknadStatistikkRiver(
    rapidsConnection: RapidsConnection,
    private val statistikkService: SoeknadStatistikkService
) : ListenerMedLogging(rapidsConnection) {

    init {
        initialiser {
            eventName(EventNames.FORDELER_STATISTIKK)
            validate { it.requireKey(SOEKNAD_ID_KEY) }
            validate { it.requireKey(GYLDIG_FOR_BEHANDLING_KEY) }
            validate { it.requireKey(SAK_TYPE_KEY) }
            validate { it.interestedIn(FEILENDE_KRITERIER_KEY) }
        }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) =
        try {
            val soeknadId = packet[SOEKNAD_ID_KEY].longValue()
            val gyldigForBehandling = packet[GYLDIG_FOR_BEHANDLING_KEY].booleanValue()
            val sakType = enumValueOf<SakType>(packet[SAK_TYPE_KEY].textValue())
            val feilendeKriterier = when (val feilendeKriterier = packet[FEILENDE_KRITERIER_KEY]) {
                is MissingNode, is NullNode -> null
                else -> objectMapper.readValue<List<String>>(feilendeKriterier.toString())
            }
            statistikkService.registrerSoeknadStatistikk(soeknadId, gyldigForBehandling, sakType, feilendeKriterier)
        } catch (e: Exception) {
            logger.error(
                """
                    Kunne ikke mappe ut statisikk for fordelerevent i pakken med korrelasjonsid ${packet.correlationId}.
                    Dette betyr at statistikk for denne søknadens fordeling ikke blir med i oversikten, 
                    så det vil være avvik. Bør fikses raskt, men stopper ikke prosessering av annen statistikk.
                """.trimIndent(),
                e
            )
        }
}