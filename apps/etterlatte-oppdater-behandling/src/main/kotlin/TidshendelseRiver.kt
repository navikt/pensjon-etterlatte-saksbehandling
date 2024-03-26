package no.nav.etterlatte

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.YearMonth

class TidshendelseRiver(
    rapidsConnection: RapidsConnection,
    private val tidshendelseService: TidshendelseService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERGANG) {
            validate { it.requireValue(ALDERSOVERGANG_STEG_KEY, "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR") }
            validate { it.requireKey(ALDERSOVERGANG_TYPE_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_ID_KEY) }
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
        packetUpdates[ALDERSOVERGANG_STEG_KEY] = "OPPGAVE_OPPRETTET"
        packetUpdates[HENDELSE_DATA_KEY] = emptyMap<String, Any>()

        when (val result = tidshendelseService.haandterHendelse(hendelse)) {
            is TidshendelseResult.OpprettetOmregning -> {
                packetUpdates[ALDERSOVERGANG_STEG_KEY] = "BEHANDLING_OPPRETTET"
                packetUpdates[BEHANDLING_ID_KEY] = result.behandlingId
                packetUpdates[BEHANDLING_VI_OMREGNER_FRA_KEY] = result.forrigeBehandlingId
            }

            is TidshendelseResult.OpprettetOppgave -> {
                packetUpdates[HENDELSE_DATA_KEY] = mapOf("opprettetOppgaveId" to result.opprettetOppgaveId)
            }

            is TidshendelseResult.Skipped -> {}
        }
        return packetUpdates
    }
}

class TidshendelsePacket(packet: JsonMessage) {
    val sakId = packet.sakId
    val behandlingsmaaned: YearMonth = packet.dato.let { YearMonth.of(it.year, it.month) }
    val harLoependeYtelse = packet[HENDELSE_DATA_KEY]["loependeYtelse"]?.asBoolean() == true
    val behandlingId = packet[HENDELSE_DATA_KEY]["loependeYtelse_behandlingId"]?.asUUID()
    val harMigrertYrkesskadeFordel = packet["yrkesskadefordel_pre_20240101"].asBoolean()
    val harRettUtenTidsbegrensning = packet["oms_rett_uten_tidsbegrensning"].asBoolean()
    val dryrun = packet[DRYRUN].asBoolean()
    val jobbtype = TidshendelseService.TidshendelserJobbType.valueOf(packet[ALDERSOVERGANG_TYPE_KEY].asText())
    val hendelseId: String = packet[ALDERSOVERGANG_ID_KEY].asText()
}

enum class TidshendelserFeatureToggle(private val key: String) : FeatureToggle {
    OpprettOppgaveForVarselbrevAktivitetsplikt("opprett-oppgave-for-varselbrev-aktivitetsplikt"),
    ;

    override fun key() = key
}
