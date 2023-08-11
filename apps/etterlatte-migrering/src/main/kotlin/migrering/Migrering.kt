package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.START_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.request
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class Migrering(rapidsConnection: RapidsConnection, private val pesysRepository: PesysRepository) :
    ListenerMedLoggingOgFeilhaandtering(START_MIGRERING) {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) =
        pesysRepository.hentSaker().forEach { migrerSak(packet, it, context) }

    private fun migrerSak(
        packet: JsonMessage,
        sak: Pesyssak,
        context: MessageContext
    ) {
        packet.eventName = Migreringshendelser.MIGRER_SAK
        val request = tilMigreringsrequest(sak)
        packet.request = request.toJson()
        packet[FNR_KEY] = request.soeker.value
        packet[BEHOV_NAME_KEY] = Opplysningstype.AVDOED_PDL_V1
        context.publish(packet.toJson())
        logger.info(
            "Migrering starta for pesys-sak ${sak.pesysId} og melding om behandling ble sendt."
        )
        pesysRepository.settSakMigrert(sak.id)
    }

    private fun tilMigreringsrequest(sak: Pesyssak) = MigreringRequest(
        pesysId = sak.pesysId,
        enhet = sak.enhet,
        soeker = sak.soeker,
        gjenlevendeForelder = sak.gjenlevendeForelder,
        avdoedForelder = sak.avdoedForelder,
        virkningstidspunkt = sak.virkningstidspunkt,
        foersteVirkningstidspunkt = sak.foersteVirkningstidspunkt,
        beregning = sak.beregning,
        trygdetid = sak.trygdetid,
        flyktningStatus = sak.flyktningStatus
    )
}