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
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

class TidshendelseRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERGANG) {
            validate { it.requireKey(ALDERSOVERGANG_STEG_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_TYPE_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(DRYRUN) }
            validate { it.interestedIn(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val type = packet[ALDERSOVERGANG_TYPE_KEY].asText()
        val step = packet[ALDERSOVERGANG_STEG_KEY].asText()
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
            if (step == "VURDERT_LOEPENDE_YTELSE") {
                // Opprette ny for å fjerne datanøkkel
                val kvittering =
                    JsonMessage.newMessage(
                        EventNames.ALDERSOVERGANG.lagEventnameForType(),
                        mapOf(
                            ALDERSOVERGANG_STEG_KEY to "OPPGAVE_OPPRETTET",
                            ALDERSOVERGANG_TYPE_KEY to type,
                            ALDERSOVERGANG_ID_KEY to UUID.fromString(hendelseId),
                            SAK_ID_KEY to sakId,
                            DATO_KEY to packet.dato,
                            DRYRUN to dryrun,
                        ),
                    )

                if (packet[HENDELSE_DATA_KEY].asBoolean()) {
                    val behandlingsmaaned = packet.dato.let { YearMonth.of(it.year, it.month) }
                    logger.info("Løpende ytelse: opprette oppgave for sak $sakId, behandlingsmåned=$behandlingsmaaned")

                    if (!dryrun) {
                        val frist = Tidspunkt.ofNorskTidssone(behandlingsmaaned.atDay(20), LocalTime.NOON)
                        val oppgaveId =
                            behandlingService.opprettOppgave(
                                sakId,
                                OppgaveType.MANUELT_OPPHOER,
                                merknad = "Aldersovergang",
                                frist = frist,
                            )
                        kvittering[HENDELSE_DATA_KEY] = oppgaveId
                    } else {
                        logger.info("Dry run: skipper oppgave")
                    }
                } else {
                    logger.info("Ingen løpende ytelse funnet for sak $sakId")
                }

                context.publish(kvittering.toJson())
            } else {
                logger.info("Ikke-støttet steg: $step, ignorerer hendelse")
            }
        }
    }
}
