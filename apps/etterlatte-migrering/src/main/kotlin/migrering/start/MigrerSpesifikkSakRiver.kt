package no.nav.etterlatte.migrering.start

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.migrering.pen.BarnepensjonGrunnlagResponse
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.pen.tilVaarModell
import no.nav.etterlatte.migrering.person.krr.KrrKlient
import no.nav.etterlatte.migrering.verifisering.Verifiserer
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rapidsandrivers.migrering.kilde
import no.nav.etterlatte.rapidsandrivers.migrering.loependeJanuer2024
import no.nav.etterlatte.rapidsandrivers.migrering.migreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class MigrerSpesifikkSakRiver(
    rapidsConnection: RapidsConnection,
    private val penKlient: PenKlient,
    private val pesysRepository: PesysRepository,
    private val featureToggleService: FeatureToggleService,
    private val verifiserer: Verifiserer,
    private val krrKlient: KrrKlient,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagEventnameForType()) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(LOPENDE_JANUAR_2024_KEY) }
            validate { it.requireKey(MIGRERING_KJORING_VARIANT) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        val kjoringsVariant = packet.migreringKjoringVariant
        val migreringStatus = pesysRepository.hentStatus(sakId)

        if (kjoringsVariant == MigreringKjoringVariant.FORTSETT_ETTER_PAUSE) {
            if (migreringStatus == Migreringsstatus.PAUSE) {
                sendSakRettTilVedtak(packet, context)
            } else {
                logger.info("Sak $sakId har status $migreringStatus og kan ikke fortsette etter pause. Avbryter.")
            }
            return
        }

        if (migreringStatus in
            setOf(
                Migreringsstatus.UNDER_MIGRERING,
                Migreringsstatus.UTBETALING_OK,
                Migreringsstatus.BREVUTSENDING_OK,
                Migreringsstatus.FERDIG,
                Migreringsstatus.PAUSE,
                Migreringsstatus.MANUELL,
                Migreringsstatus.UNDER_MIGRERING_MANUELT,
                Migreringsstatus.UTBETALING_FEILA,
            )
        ) {
            logger.info("Sak $sakId har status $migreringStatus. Avbryter.")
            return
        }
        val lopendeJanuar2024 = packet.loependeJanuer2024

        val pesyssak =
            hentSak(sakId, lopendeJanuar2024)
                .tilVaarModell { runBlocking { krrKlient.hentDigitalKontaktinformasjon(it) } }
                .also { pesysRepository.lagrePesyssak(pesyssak = it) }
        packet.eventName = Migreringshendelser.MIGRER_SAK.lagEventnameForType()
        val verifisertRequest = verifiserer.verifiserRequest(pesyssak.tilMigreringsrequest())
        packet.hendelseData = verifisertRequest

        if (featureToggleService.isEnabled(MigreringFeatureToggle.SendSakTilMigrering, false)) {
            sendSakTilMigrering(packet, verifisertRequest, context, pesyssak)
        } else {
            logger.info("Migrering er skrudd av. Sender ikke pesys-sak ${pesyssak.id} videre.")
            pesysRepository.lagreGyldigDryRun(verifisertRequest)
        }
    }

    private fun hentSak(
        sakId: Long,
        lopendeJanuar2024: Boolean,
    ): BarnepensjonGrunnlagResponse {
        logger.info("Prøver å hente sak $sakId fra PEN")
        val sakFraPEN = runBlocking { penKlient.hentSak(sakId, lopendeJanuar2024) }
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
        packet.kilde = Vedtaksloesning.PESYS
        context.publish(packet.toJson())
        logger.info(
            "Migrering starta for pesys-sak ${sak.id} og melding om behandling ble sendt.",
        )
        pesysRepository.oppdaterStatus(PesysId(sak.id), Migreringsstatus.UNDER_MIGRERING)
    }

    private fun sendSakRettTilVedtak(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        if (!featureToggleService.isEnabled(MigreringFeatureToggle.SendSakTilMigrering, false)) {
            logger.info("Migrering er skrudd av. Sender ikke pesys-sak $sakId videre.")
            return
        }

        val behandlingId =
            pesysRepository.hentKoplingTilBehandling(PesysId(sakId))?.behandlingId
                ?: throw IllegalStateException("Mangler kobling mellom behandling i Gjenny og pesys sak for pesysId=$sakId")

        packet[BEHANDLING_ID_KEY] = behandlingId
        packet.eventName = Migreringshendelser.VEDTAK.lagEventnameForType()
        context.publish(packet.toJson())
        logger.info("Fortsetter migrering for sak $sakId.")
    }
}

enum class MigreringFeatureToggle(private val key: String) : FeatureToggle {
    SendSakTilMigrering("pensjon-etterlatte.bp-send-sak-til-migrering"),
    OpphoerSakIPesys("opphoer-sak-i-pesys"),
    MigrerNaarSoekerHarVerge("migrer-naar-soeker-har-verge"),
    ;

    override fun key() = key
}
