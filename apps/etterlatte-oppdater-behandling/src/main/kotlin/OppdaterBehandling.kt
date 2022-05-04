package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class OppdaterBehandling(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("grunnlag") }
            validate { it.requireKey("id") }
            validate { it.interestedIn("@gyldighetsvurdering") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                val behandlingsID = packet["id"].asText()
                if (packet["@gyldighetsvurdering"].toString().isNotEmpty()) {
                    behandlinger.leggTilGyldighetsresultat(UUID.fromString(behandlingsID), objectMapper.readValue(packet["@gyldighetsvurdering"].toString()))
                    logger.info("Oppdatert Behandling med id $behandlingsID med ny gyldighetsvurdering")
                }

            } catch (e: Exception){
                //TODO endre denne
                logger.info("Spiser en melding: "+e)
            }

        }
    }


interface Behandling {
    fun leggTilGyldighetsresultat(behandling: UUID, gyldighetsResultat: GyldighetsResultat)
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()