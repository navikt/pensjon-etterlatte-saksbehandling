package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.GRUNNLAG_OPPDATERT
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val grunnlagKlient: GrunnlagKlient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.NY_OPPLYSNING) {
            validate { it.interestedIn(FNR_KEY) }
            validate { it.requireKey(OPPLYSNING_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.rejectValue(EVENT_NAME_KEY, GRUNNLAG_OPPDATERT) }
            validate { it.rejectValue(EVENT_NAME_KEY, EventNames.FEILA.lagEventnameForType()) }
            validate { it.rejectValue(VILKAARSVURDERT_KEY, true) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = SakId(packet[SAK_ID_KEY].asLong())
        val behandlingId = packet[BEHANDLING_ID_KEY].let { UUID.fromString(it.asText()) }

        val opplysninger: List<Grunnlagsopplysning<JsonNode>> =
            objectMapper.readValue(packet[OPPLYSNING_KEY].toJson())!!

        val fnr = packet[FNR_KEY].textValue()
        if (fnr == null) {
            logger.info("Lagrer nye saksopplysninger på sak $sakId")

            grunnlagKlient.lagreNyeSaksopplysninger(
                sakId,
                behandlingId,
                opplysninger,
            )
        } else {
            logger.info("Lagrer nye personopplysninger på sak $sakId")

            grunnlagKlient.lagreNyePersonopplysninger(
                sakId,
                behandlingId,
                Folkeregisteridentifikator.of(fnr),
                opplysninger,
            )
        }
        packet.eventName = GRUNNLAG_OPPDATERT
        context.publish(packet.toJson())

        logger.info("Grunnlag opppdatert på sak $sakId")
    }
}
