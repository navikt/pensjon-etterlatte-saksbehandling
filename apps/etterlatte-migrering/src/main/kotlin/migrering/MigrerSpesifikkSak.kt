package no.nav.etterlatte.migrering

import kotlinx.coroutines.runBlocking
import migrering.verifisering.Verifiserer
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.pen.tilVaarModell
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.MIGRER_SPESIFIKK_SAK
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class MigrerSpesifikkSak(
    rapidsConnection: RapidsConnection,
    private val penKlient: PenKlient,
    private val pesysRepository: PesysRepository,
    private val featureToggleService: FeatureToggleService,
    private val verifiserer: Verifiserer,
) : ListenerMedLoggingOgFeilhaandtering(MIGRER_SPESIFIKK_SAK) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        if (pesysRepository.hentStatus(sakId) != null) {
            logger.info("Har allerede migrert sak $sakId. Avbryter.")
            return
        }

        val pesyssak =
            hentSak(sakId).tilVaarModell().also {
                pesysRepository.lagrePesyssak(pesyssak = it)
            }
        packet.eventName = Migreringshendelser.MIGRER_SAK
        val request = pesyssak.tilMigreringsrequest()
        packet.hendelseData = request
        try {
            verifiserer.verifiserRequest(request)
        } catch (e: Exception) {
            logger.warn("Sak ${request.pesysId} har ufullstendige data i PDL, kan ikke migrere")
            pesysRepository.oppdaterStatus(request.pesysId, Migreringsstatus.FEILA)
            throw e
        }

        if (featureToggleService.isEnabled(MigreringFeatureToggle.SendSakTilMigrering, false)) {
            sendSakTilMigrering(packet, request, context, pesyssak)
        } else {
            logger.info("Migrering er skrudd av. Sender ikke pesys-sak ${pesyssak.id} videre.")
        }
    }

    private fun hentSak(sakId: Long): BarnepensjonGrunnlagResponse {
        logger.info("Prøver å hente sak $sakId fra PEN")
        val sakFraPEN = runBlocking { penKlient.hentSak(sakId) }
        logger.info("Henta sak $sakId fra PEN")
        return sakFraPEN
    }

    private fun sendSakTilMigrering(
        packet: JsonMessage,
        request: MigreringRequest,
        context: MessageContext,
        sak: Pesyssak,
    ) {
        packet[FNR_KEY] = request.soeker.value
        packet[BEHOV_NAME_KEY] = Opplysningstype.AVDOED_PDL_V1
        packet.pesysId = PesysId(sak.id)
        context.publish(packet.toJson())
        logger.info(
            "Migrering starta for pesys-sak ${sak.id} og melding om behandling ble sendt.",
        )
        pesysRepository.oppdaterStatus(PesysId(sak.id), Migreringsstatus.UNDER_MIGRERING)
    }
}

enum class MigreringFeatureToggle(private val key: String) : FeatureToggle {
    SendSakTilMigrering("pensjon-etterlatte.bp-send-sak-til-migrering"),
    ;

    override fun key() = key
}
