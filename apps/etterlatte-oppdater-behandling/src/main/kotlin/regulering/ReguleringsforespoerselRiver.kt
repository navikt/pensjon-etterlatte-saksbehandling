package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.tilbakestilteBehandlinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class ReguleringsforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(ReguleringsforespoerselRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.START_REGULERING) {
            validate { it.requireKey(DATO_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Leser reguleringsfoerespoersel for dato ${packet.dato}")

        if (!featureToggleService.isEnabled(ReguleringFeatureToggle.START_REGULERING, false)) {
            logger.info("Regulering er deaktivert ved funksjonsbryter. Avbryter reguleringsforespørsel.")
            return
        }

        val sakerTilOmregning = behandlingService.hentAlleSaker()

        val tilbakemigrerte =
            behandlingService.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(sakerTilOmregning)
                .also { sakIdListe ->
                    logger.info(
                        "Tilbakemigrert ${sakIdListe.ider.size} behandlinger:\n" +
                            sakIdListe.ider.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" },
                    )
                }

        sakerTilOmregning.saker.forEach {
            packet.setEventNameForHendelseType(ReguleringHendelseType.FINN_LOEPENDE_YTELSER)
            packet.tilbakestilteBehandlinger = tilbakemigrerte.behandlingerForSak(it.id)
            packet.sakId = it.id
            context.publish(packet.toJson())
        }
    }
}

enum class ReguleringFeatureToggle(private val key: String) : FeatureToggle {
    START_REGULERING("start-regulering"),
    ;

    override fun key() = key
}
