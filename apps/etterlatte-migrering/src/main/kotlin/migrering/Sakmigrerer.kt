package no.nav.etterlatte.migrering

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.etterlatte.rapidsandrivers.migrering.request
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

enum class MigreringFeatureToggle(private val key: String) : FeatureToggle {
    SendSakTilMigrering("pensjon-etterlatte.bp-send-sak-til-migrering");

    override fun key() = key
}

internal class Sakmigrerer(
    private val pesysRepository: PesysRepository,
    private val featureToggleService: FeatureToggleService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal fun migrerSak(
        packet: JsonMessage,
        sak: Pesyssak,
        context: MessageContext
    ) {
        packet.eventName = Migreringshendelser.MIGRER_SAK
        val request = tilMigreringsrequest(sak)
        packet.request = request.toJson()
        if (featureToggleService.isEnabled(MigreringFeatureToggle.SendSakTilMigrering, false)) {
            sendSakTilMigrering(packet, request, context, sak)
        } else {
            logger.info("Migrering er skrudd av. Sender ikke pesys-sak ${sak.id} videre.")
        }
    }

    private fun sendSakTilMigrering(
        packet: JsonMessage,
        request: MigreringRequest,
        context: MessageContext,
        sak: Pesyssak
    ) {
        packet[FNR_KEY] = request.soeker.value
        packet[BEHOV_NAME_KEY] = Opplysningstype.AVDOED_PDL_V1
        packet.pesysId = PesysId(sak.id)
        context.publish(packet.toJson())
        logger.info(
            "Migrering starta for pesys-sak ${sak.id} og melding om behandling ble sendt."
        )
        pesysRepository.oppdaterStatus(sak.id, Migreringsstatus.UNDER_MIGRERING)
    }

    private fun tilMigreringsrequest(sak: Pesyssak) = MigreringRequest(
        pesysId = PesysId(sak.id),
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