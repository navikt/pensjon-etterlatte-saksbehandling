package no.nav.etterlatte.regulering

import com.fasterxml.jackson.databind.node.MissingNode
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.aapneBehandlinger
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.rapidsandrivers.tilbakestilteBehandlinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.math.max
import kotlin.math.min

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
            validate { it.requireKey(SPESIFIKKE_SAKER) }
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
        val sakType = packet.optionalSakType()

        val maksBatchstoerrelse = MAKS_BATCHSTOERRELSE
        var tatt = 0

        while (tatt < antall) {
            val antallIDenneRunden = max(0, min(maksBatchstoerrelse, antall - tatt))
            logger.info("Starter å ta $antallIDenneRunden av totalt $antall saker")
            val sakerTilOmregning =
                behandlingService.hentAlleSaker(kjoering, antallIDenneRunden, spesifikkeSaker, sakerViIkkeRegulererAutomatiskNaa, sakType)
            logger.info("Henta ${sakerTilOmregning.saker.size} saker")

            if (sakerTilOmregning.saker.isEmpty()) {
                logger.debug("Ingen saker i denne runden. Returnerer")
                break
            }

            val sakListe =
                behandlingService
                    .migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(sakerTilOmregning)
                    .also { sakIdListe ->
                        logger.info(
                            "Tilbakeført ${sakIdListe.tilbakestileBehandlinger.size} behandlinger til trygdetid oppdatert:\n" +
                                sakIdListe.tilbakestileBehandlinger.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" },
                        )
                    }

            sakerTilOmregning.saker.forEach {
                logger.debug("Lagrer kjøring starta for sak ${it.id}")
                behandlingService.lagreKjoering(it.id, KjoeringStatus.STARTA, kjoering)
                logger.debug("Ferdig lagra kjøring starta for sak ${it.id}")
                packet.setEventNameForHendelseType(ReguleringHendelseType.SAK_FUNNET)
                packet.tilbakestilteBehandlinger = sakListe.tilbakestilteForSak(it.id)
                packet.aapneBehandlinger = sakListe.aapneBehandlingerForSak(it.id)
                packet.sakId = it.id
                logger.debug("Sender til omregning for sak ${it.id}")
                context.publish(packet.toJson())
            }
            tatt += sakerTilOmregning.saker.size
            logger.info("Ferdig med $tatt av totalt $antall saker")
            if (sakerTilOmregning.saker.size < maksBatchstoerrelse) {
                break
            }
            val venteperiode = Duration.ofSeconds(5)
            logger.info("Venter $venteperiode før neste runde.")
            Thread.sleep(venteperiode)
        }
    }

    private fun JsonMessage.optionalSakType(): SakType? =
        when (val node = this[SAK_TYPE]) {
            is MissingNode -> null
            else -> SakType.valueOf(node.asText())
        }

    companion object {
        const val MAKS_BATCHSTOERRELSE = 100
    }
}

enum class ReguleringFeatureToggle(
    private val key: String,
) : FeatureToggle {
    START_REGULERING("start-regulering"),
    ;

    override fun key() = key
}

private val sakerViIkkeRegulererAutomatiskNaa: List<Long> = emptyList()
