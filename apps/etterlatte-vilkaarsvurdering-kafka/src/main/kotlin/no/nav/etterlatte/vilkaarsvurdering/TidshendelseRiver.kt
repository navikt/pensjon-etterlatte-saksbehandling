package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.etterlatte.rapidsandrivers.behandlingId
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
        initialiserRiver(rapidsConnection, EventNames.TIDSHENDELSE) {
            validate {
                it.requireAny(
                    TIDSHENDELSE_STEG_KEY,
                    listOf("VURDERT_LOEPENDE_YTELSE", "BEHANDLING_OPPRETTET"),
                )
            }
            validate { it.requireKey(TIDSHENDELSE_TYPE_KEY) }
            validate { it.requireKey(TIDSHENDELSE_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(DRYRUN) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.interestedIn(BEHANDLING_ID_KEY) }
            validate { it.interestedIn(BEHANDLING_VI_OMREGNER_FRA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val steg = packet[TIDSHENDELSE_STEG_KEY].asText()
        val type = packet[TIDSHENDELSE_TYPE_KEY].asText()
        val hendelseId = packet[TIDSHENDELSE_ID_KEY].asText()
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
            val behandlet =
                when (steg) {
                    "VURDERT_LOEPENDE_YTELSE" -> sjekkUnntaksregler(packet, type)
                    "BEHANDLING_OPPRETTET" -> vilkaarsvurder(packet, dryrun)
                    else -> false
                }

            if (behandlet) {
                // Reply med oppdatert melding
                context.publish(packet.toJson())
            }
        }
    }

    private fun sjekkUnntaksregler(
        packet: JsonMessage,
        type: String,
    ): Boolean {
        val hendelseData = packet[HENDELSE_DATA_KEY]

        hendelseData["loependeYtelse_januar2024_behandlingId"]?.let {
            val behandlingId = it.asText()
            val result = vilkaarsvurderingService.harMigrertYrkesskadefordel(behandlingId)
            logger.info("Løpende ytelse: sjekk av yrkesskadefordel før 2024-01-01: $result")
            packet["yrkesskadefordel_pre_20240101"] = result
        }

        if (type in arrayOf("OMS_DOED_3AAR", "OMS_DOED_5AAR") && hendelseData["loependeYtelse"]?.asBoolean() == true) {
            val loependeBehandlingId = hendelseData["loependeYtelse_behandlingId"].asText()
            val result = vilkaarsvurderingService.harRettUtenTidsbegrensning(loependeBehandlingId)
            logger.info("OMS: sjekk av rett uten tidsbegrensning: $result")
            packet["oms_rett_uten_tidsbegrensning"] = result
        }

        packet[TIDSHENDELSE_STEG_KEY] = "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
        return true
    }

    private fun vilkaarsvurder(
        packet: JsonMessage,
        dryrun: Boolean,
    ): Boolean {
        val behandlingId = packet.behandlingId
        val forrigeBehandlingId = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asUUID()

        if (dryrun) {
            logger.info("Dryrun: skipper å behandle vilkårsvurdering for behandlingId=$behandlingId")
        } else {
            logger.info("Oppretter vilkårsvurdering for aldersovergang, behandlingId=$behandlingId")
            vilkaarsvurderingService.opphoerAldersovergang(behandlingId, forrigeBehandlingId)
        }

        packet[TIDSHENDELSE_STEG_KEY] = "VILKAARSVURDERT"
        return true
    }
}
