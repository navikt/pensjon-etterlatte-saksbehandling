package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.time.YearMonth

class TidshendelseRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
            val hendelseData = mutableMapOf<String, Any>()

            if (type == "AO_BP20" && packet["yrkesskadefordel_pre_20240101"].asBoolean()) {
                logger.info("Har migrert yrkesskadefordel: utvidet aldersgrense [sak=$sakId]")
            } else if (type in arrayOf("OMS_DOED_3AAR", "OMS_DOED_5AAR") && packet["oms_rett_uten_tidsbegrensning"].asBoolean()) {
                logger.info("Har omstillingsstønad med rett uten tidsbegrensning, opphører ikke [sak=$sakId]")
            } else if (packet[HENDELSE_DATA_KEY]["loependeYtelse"]?.asBoolean() == true) {
                val behandlingsmaaned = packet.dato.let { YearMonth.of(it.year, it.month) }
                logger.info("Løpende ytelse: opprette oppgave for sak $sakId, behandlingsmåned=$behandlingsmaaned")

                if (!dryrun) {
                    val frist = Tidspunkt.ofNorskTidssone(behandlingsmaaned.atEndOfMonth(), LocalTime.NOON)
                    val oppgaveId =
                        behandlingService.opprettOppgave(
                            sakId,
                            OppgaveType.REVURDERING,
                            merknad = generateMerknad(type),
                            frist = frist,
                        )
                    hendelseData["opprettetOppgaveId"] = oppgaveId
                } else {
                    logger.info("Dry run: skipper oppgave")
                }
            } else {
                logger.info("Ingen løpende ytelse funnet for sak $sakId")
            }

            packet[ALDERSOVERGANG_STEG_KEY] = "OPPGAVE_OPPRETTET"
            packet[HENDELSE_DATA_KEY] = hendelseData
            context.publish(packet.toJson())
        }
    }

    private fun generateMerknad(type: String): String {
        return when (type) {
            "AO_BP20" -> "Aldersovergang v/20 år"
            "AO_BP21" -> "Aldersovergang v/21 år"
            "AO_OMS67" -> "Aldersovergang v/67 år"
            "OMS_DOED_3AAR" -> "Opphør OMS etter 3 år"
            "OMS_DOED_5AAR" -> "Opphør OMS etter 5 år"
            else -> throw IllegalArgumentException("Ikke-støttet type: $type")
        }
    }
}
