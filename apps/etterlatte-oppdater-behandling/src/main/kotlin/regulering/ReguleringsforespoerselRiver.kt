package no.nav.etterlatte.regulering

import com.fasterxml.jackson.databind.node.MissingNode
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.aapneBehandlinger
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.ekskluderteSaker
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.saker
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
        initialiserRiver(rapidsConnection, ReguleringHendelseType.REGULERING_STARTA) {
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(KJOERING) }
            validate { it.requireKey(ANTALL) }
            validate { it.interestedIn(SPESIFIKKE_SAKER) }
            validate { it.interestedIn(EKSKLUDERTE_SAKER) }
            validate { it.interestedIn(SAK_TYPE) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Leser reguleringsforespørsel for dato ${packet.dato}")

        if (!featureToggleService.isEnabled(ReguleringFeatureToggle.START_REGULERING, false)) {
            logger.info("Regulering er deaktivert ved funksjonsbryter. Avbryter reguleringsforespørsel.")
            return
        }

        val kjoering = packet[KJOERING].asText()
        val antall = packet[ANTALL].asInt()
        val spesifikkeSaker = packet.saker
        val ekskluderteSaker = packet.ekskluderteSaker
        val sakType = packet.optionalSakType()

        kjoerIBatch(
            logger = logger,
            antall = antall,
            finnSaker = { antallIDenneRunden ->
                behandlingService.hentAlleSaker(
                    kjoering,
                    antallIDenneRunden,
                    spesifikkeSaker,
                    ekskluderteSaker,
                    sakType,
                )
            },
            haandterSaker = { sakerTilOmregning ->
                val sakListe = flyttBehandlingerUnderArbeidTilbakeTilTrygdetidOppdatert(sakerTilOmregning)
                sakerTilOmregning.saker.forEach {
                    publiserSak(it, kjoering, packet, sakListe, context)
                }
            },
        )
    }

    private fun JsonMessage.optionalSakType(): SakType? =
        when (val node = this[SAK_TYPE]) {
            is MissingNode -> null
            else -> SakType.valueOf(node.asText())
        }

    private fun flyttBehandlingerUnderArbeidTilbakeTilTrygdetidOppdatert(sakerTilOmregning: Saker): SakIDListe =
        behandlingService
            .migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(sakerTilOmregning)
            .also { sakIdListe ->
                logger.info(
                    "Tilbakeført ${sakIdListe.tilbakestileBehandlinger.size} behandlinger til trygdetid oppdatert:\n" +
                        sakIdListe.tilbakestileBehandlinger.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" },
                )
            }

    private fun publiserSak(
        sak: Sak,
        kjoering: String,
        packet: JsonMessage,
        sakListe: SakIDListe,
        context: MessageContext,
    ) {
        logger.debug("Lagrer kjøring starta for sak ${sak.id}")
        behandlingService.lagreKjoering(sak.id, KjoeringStatus.STARTA, kjoering)
        logger.debug("Ferdig lagra kjøring starta for sak ${sak.id}")
        packet.setEventNameForHendelseType(ReguleringHendelseType.SAK_FUNNET)
        packet.tilbakestilteBehandlinger = sakListe.tilbakestilteForSak(sak.id)
        packet.aapneBehandlinger = sakListe.aapneBehandlingerForSak(sak.id)
        packet.sakId = sak.id
        logger.debug("Sender til omregning for sak ${sak.id}")
        context.publish(packet.toJson())
    }
}

enum class ReguleringFeatureToggle(
    private val key: String,
) : FeatureToggle {
    START_REGULERING("start-regulering"),
    ;

    override fun key() = key
}
