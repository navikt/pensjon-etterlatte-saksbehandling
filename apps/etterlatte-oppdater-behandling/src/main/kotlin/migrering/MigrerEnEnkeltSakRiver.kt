package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.MigrerSoekerRequest
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PERSONGALLERI_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.ROLLE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.OPPLYSNING_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class MigrerEnEnkeltSakRiver(
    rapidsConnection: RapidsConnection,
    private val behandlinger: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.MIGRER_SAK) {
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(FNR_KEY) }
            validate { it.rejectKey(OPPLYSNING_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt migreringshendelse")

        val hendelse: MigreringRequest = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
        val (behandlingId, sakId) = behandlinger.migrer(hendelse)

        packet.behandlingId = behandlingId
        packet.sakId = sakId

        packet[SAK_TYPE_KEY] = SakType.BARNEPENSJON
        packet[ROLLE_KEY] = PersonRolle.AVDOED
        packet[MIGRERING_GRUNNLAG_KEY] =
            MigrerSoekerRequest(
                soeker = hendelse.soeker.value,
            )
        packet[PERSONGALLERI_KEY] = hendelse.opprettPersongalleri()
        packet.eventName = Migreringshendelser.LAGRE_KOPLING.lagEventnameForType()

        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert migreringshendelse")
    }
}
