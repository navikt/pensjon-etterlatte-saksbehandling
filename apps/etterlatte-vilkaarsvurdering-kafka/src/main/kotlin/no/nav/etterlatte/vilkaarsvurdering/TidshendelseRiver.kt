package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class TidshendelseRiver(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERGANG) {
            validate { it.requireValue(ALDERSOVERGANG_STEG_KEY, "VURDERT_LOEPENDE_YTELSE") }
            validate { it.requireKey(ALDERSOVERGANG_TYPE_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(DRYRUN) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val type = packet[ALDERSOVERGANG_TYPE_KEY].asText()
        val hendelseId = packet[ALDERSOVERGANG_ID_KEY].asText()
        val dryrun = packet[DRYRUN].asBoolean()
        val sakId = packet.sakId

        withLogContext(
            correlationId = getCorrelationId(),
            mapOf(
                "hendelseId" to hendelseId,
                "sakId" to sakId.toString(),
                "type" to type,
                "dryRun" to dryrun.toString(),
            ),
        ) {
            packet[HENDELSE_DATA_KEY]["loependeYtelse_januar2024_behandlingId"]?.let {
                val behandlingId = it.asText()
                val result = vilkaarsvurderingService.harMigrertYrkesskadefordel(behandlingId)
                logger.info("Løpende ytelse: sjekk av yrkesskadefordel før 2024-01-01: $result")
                packet["yrkesskadefordel_pre_20240101"] = result
            }

            packet[ALDERSOVERGANG_STEG_KEY] = "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
            context.publish(packet.toJson())
        }
    }
}
