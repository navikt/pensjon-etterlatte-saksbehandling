package no.nav.etterlatte.regulering

import com.fasterxml.jackson.databind.node.MissingNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakslisteDTO
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.aapneBehandlinger
import no.nav.etterlatte.rapidsandrivers.antall
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.ekskluderteSaker
import no.nav.etterlatte.rapidsandrivers.kjoering
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.rapidsandrivers.tilbakestilteBehandlinger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit

internal class ReguleringsforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

        val kjoering = packet.kjoering
        val antall = packet.antall
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
                sakerTilOmregning.sakIdListe.forEach { sakId ->
                    publiserSak(sakId, kjoering, packet, sakListe, context)
                }
            },
            batchStoerrelse = 25,
            venteperiode = Duration.of(2L, ChronoUnit.SECONDS),
        )
    }

    private fun JsonMessage.optionalSakType(): SakType? =
        when (val node = this[SAK_TYPE]) {
            is MissingNode -> null
            else -> SakType.valueOf(node.asText())
        }

    private fun flyttBehandlingerUnderArbeidTilbakeTilTrygdetidOppdatert(sakslisteDTOTilOmregning: SakslisteDTO): SakIDListe =
        behandlingService
            .migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(sakslisteDTOTilOmregning)
            .also { sakIdListe ->
                logger.info(
                    "Tilbakeført ${sakIdListe.tilbakestileBehandlinger.size} behandlinger til trygdetid oppdatert:\n" +
                        sakIdListe.tilbakestileBehandlinger.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" },
                )
            }

    private fun publiserSak(
        sakId: SakId,
        kjoering: String,
        packet: JsonMessage,
        sakListe: SakIDListe,
        context: MessageContext,
    ) {
        logger.debug("Lagrer kjøring starta for sak $sakId")
        behandlingService.lagreKjoering(sakId, KjoeringStatus.STARTA, kjoering)
        logger.debug("Ferdig lagra kjøring starta for sak $sakId")
        packet.setEventNameForHendelseType(ReguleringHendelseType.SAK_FUNNET)
        packet.tilbakestilteBehandlinger = sakListe.tilbakestilteForSak(sakId)
        packet.aapneBehandlinger = sakListe.aapneBehandlingerForSak(sakId)
        packet[HENDELSE_DATA_KEY] =
            OmregningData(
                kjoering = kjoering,
                sakId = sakId,
                revurderingaarsak = Revurderingaarsak.REGULERING,
            )
        logger.debug("Sender til omregning for sak $sakId")
        context.publish(packet.toJson())
    }
}

enum class ReguleringFeatureToggle(
    private val key: String,
) : FeatureToggle {
    START_REGULERING("start-regulering"),
    SKAL_STOPPE_ETTER_FATTET_VEDTAK("omregning-skal-stoppe-etter-fattet-vedtak"),
    ;

    override fun key() = key
}
