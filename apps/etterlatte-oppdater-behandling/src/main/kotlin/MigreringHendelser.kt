package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.GRUNNLAG
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.MIGRER_SAK
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.START_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.ROLLE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val behandlinger: BehandlingService) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for migreringshendelser")
        River(rapidsConnection).apply {
            eventName(MIGRER_SAK)

            correlationId()
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(FNR_KEY) }
            validate { it.rejectKey("opplysning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, START_MIGRERING) {
                logger.info("Mottatt migreringshendelse")
                packet[BEHOV_NAME_KEY] = Opplysningstype.MIGRERING

                val hendelse: MigreringRequest = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
                val behandlingOgSak = behandlinger.migrer(hendelse)

                packet.behandlingId = behandlingOgSak.behandlingId
                packet.sakId = behandlingOgSak.sakId
                packet[SAK_TYPE_KEY] = SakType.BARNEPENSJON
                packet[ROLLE_KEY] = PersonRolle.AVDOED
                packet.eventName = GRUNNLAG

                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse")
            }
        }
    }
}