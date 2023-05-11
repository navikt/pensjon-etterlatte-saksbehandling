package no.nav.etterlatte

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.ROLLE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.GRUNNLAG_OPPDATERT
import rapidsandrivers.withFeilhaandtering

internal class FortsettMigreringsflyten(rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for migreringshendelser")
        River(rapidsConnection).apply {
            eventName(GRUNNLAG_OPPDATERT)

            correlationId()
            validate { it.rejectValue(BEHOV_NAME_KEY, Opplysningstype.MIGRERING.name) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.rejectKey("opplysning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.START_MIGRERING) {
                logger.info("Mottatt migreringshendelse")
                packet[BEHOV_NAME_KEY] = Opplysningstype.MIGRERING

                packet[SAK_TYPE_KEY] = SakType.BARNEPENSJON
                packet[ROLLE_KEY] = PersonRolle.AVDOED
                packet.eventName = Migreringshendelser.GRUNNLAG

                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse")
            }
        }
    }
}