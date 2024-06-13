package no.nav.etterlatte

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
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

class TidshendelseRiver(
    rapidsConnection: RapidsConnection,
    private val tidshendelseService: TidshendelseService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, EventNames.TIDSHENDELSE) {
            validate { it.requireValue(TIDSHENDELSE_STEG_KEY, "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR") }
            validate { it.requireKey(TIDSHENDELSE_TYPE_KEY) }
            validate { it.requireKey(TIDSHENDELSE_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(DRYRUN) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.interestedIn("yrkesskadefordel_pre_20240101") }
            validate { it.interestedIn("oms_rett_uten_tidsbegrensning") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val tidshendelse = TidshendelsePacket(packet)

        withLogContext(
            correlationId = getCorrelationId(),
            mapOf(
                "hendelseId" to tidshendelse.hendelseId,
                "sakId" to tidshendelse.sakId.toString(),
                "type" to tidshendelse.jobbtype.name,
                "dryRun" to tidshendelse.dryrun.toString(),
            ),
        ) {
            haandterHendelse(tidshendelse)
                .forEach { (key, value) -> packet[key] = value }
            context.publish(packet.toJson())
        }
    }

    private fun haandterHendelse(hendelse: TidshendelsePacket): MutableMap<String, Any> {
        val packetUpdates = mutableMapOf<String, Any>()
        packetUpdates[TIDSHENDELSE_STEG_KEY] = "OPPGAVE_OPPRETTET"
        packetUpdates[HENDELSE_DATA_KEY] = emptyMap<String, Any>()

        when (val result = tidshendelseService.haandterHendelse(hendelse)) {
            is TidshendelseResult.OpprettetOmregning -> {
                packetUpdates[TIDSHENDELSE_STEG_KEY] = "BEHANDLING_OPPRETTET"
                packetUpdates[BEHANDLING_ID_KEY] = result.behandlingId
                packetUpdates[BEHANDLING_VI_OMREGNER_FRA_KEY] = result.forrigeBehandlingId
            }

            is TidshendelseResult.OpprettetOppgave -> {
                packetUpdates[HENDELSE_DATA_KEY] = mapOf("opprettetOppgaveId" to result.opprettetOppgaveId)
            }

            is TidshendelseResult.OpprettRevurderingForAktivitetsplikt -> {
                packetUpdates[TIDSHENDELSE_STEG_KEY] = "AKTIVITETSPLIKT_REVURDERING_OPPRETTET"
                packetUpdates[BEHANDLING_ID_KEY] = result.behandlingId
            }

            is TidshendelseResult.Skipped -> {
                packetUpdates[TIDSHENDELSE_STEG_KEY] = "HOPPET_OVER"
            }
        }
        return packetUpdates
    }
}
