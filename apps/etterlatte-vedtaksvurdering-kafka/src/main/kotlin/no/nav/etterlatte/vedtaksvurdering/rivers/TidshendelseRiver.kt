package no.nav.etterlatte.vedtaksvurdering.rivers

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents.IDENTIFISERT_SAK
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents.VEDTAK_ATTESTERT
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents.VILKAARSVURDERT
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents.VURDERT_LOEPENDE_YTELSE
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month

class TidshendelseRiver(
    rapidsConnection: RapidsConnection,
    private val vedtakService: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERGANG) {
            validate {
                it.requireAny(
                    ALDERSOVERGANG_STEG_KEY,
                    listOf(IDENTIFISERT_SAK.name, VILKAARSVURDERT.name),
                )
            }
            validate { it.requireKey(ALDERSOVERGANG_TYPE_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(DRYRUN) }
            validate { it.interestedIn(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val type = packet[ALDERSOVERGANG_TYPE_KEY].asText()
        val step = packet[ALDERSOVERGANG_STEG_KEY].asText()
        val hendelseId = packet[ALDERSOVERGANG_ID_KEY].asText()
        val sakId = packet.sakId
        val dryrun = packet[DRYRUN].asBoolean()

        withLogContext(
            correlationId = getCorrelationId(),
            mapOf(
                "hendelseId" to hendelseId,
                "sakId" to sakId.toString(),
                "type" to type,
            ),
        ) {
            val behandlet =
                when (step) {
                    IDENTIFISERT_SAK.name ->
                        handleIdentifisertSak(
                            packet = packet,
                            type = type,
                            sakId = sakId,
                            dryrun = dryrun,
                        )

                    VILKAARSVURDERT.name ->
                        handleVilkarsvurdertOgSkalFatteVedtak(
                            packet = packet,
                            sakId = sakId,
                            dryrun = dryrun,
                            context = context,
                        )
                    else -> false
                }

            if (behandlet) {
                // Reply med oppdatert melding
                context.publish(packet.toJson())
            }
        }
    }

    private fun handleIdentifisertSak(
        packet: JsonMessage,
        type: String,
        sakId: Long,
        dryrun: Boolean,
    ): Boolean {
        val hendelseData = mutableMapOf<String, Any>()

        val behandlingsdato = packet.dato
        // Sjekker løpende ytelse for måneden _etter_ behandlingsdato (dvs når bruker fyller år, dødsdato osv.)
        // Ytelsen skal løpe ut behandlingsmåneden. Unngå videre behandling der ytelsen kanskje allerede er opphørt.
        val maanedEtterBehandlingsdato = behandlingsdato.plusMonths(1)
        logger.info("Sjekker løpende ytelse for sak $sakId, behandlingsmåned=$behandlingsdato, dryrun=$dryrun")

        val loependeYtelse = vedtakService.harLoependeYtelserFra(sakId, maanedEtterBehandlingsdato)
        logger.info("Sak $sakId har løpende ytelse per $maanedEtterBehandlingsdato? ${loependeYtelse.erLoepende}")
        hendelseData["loependeYtelse"] = loependeYtelse.erLoepende
        loependeYtelse.behandlingId?.let {
            hendelseData["loependeYtelse_behandlingId"] = it.toString()
        }

        if (loependeYtelse.erLoepende && type == "AO_BP20") {
            val loependeYtelsePerJanuar2024 = vedtakService.harLoependeYtelserFra(sakId, ikrafttredenEtterlattereformen)
            hendelseData["loependeYtelse_januar2024"] = loependeYtelsePerJanuar2024.erLoepende
            loependeYtelsePerJanuar2024.behandlingId?.let {
                hendelseData["loependeYtelse_januar2024_behandlingId"] = it.toString()
            }
        }

        packet[ALDERSOVERGANG_STEG_KEY] = VURDERT_LOEPENDE_YTELSE.name
        packet[HENDELSE_DATA_KEY] = hendelseData
        return true
    }

    private fun handleVilkarsvurdertOgSkalFatteVedtak(
        packet: JsonMessage,
        sakId: Long,
        dryrun: Boolean,
        context: MessageContext,
    ): Boolean {
        val behandlingIdForOpphoer = packet.behandlingId

        if (dryrun) {
            logger.info("Dryrun: Fatter ikke vedtak for behandling=$behandlingIdForOpphoer")
        } else {
            logger.info("Fatter opphørsvedtak for behandling=$behandlingIdForOpphoer")
            vedtakService
                .opprettVedtakFattOgAttester(sakId, behandlingIdForOpphoer)
                .let {
                    RapidUtsender.sendUt(it, JsonMessage.newMessage(emptyMap()), context)
                }
        }

        packet[ALDERSOVERGANG_STEG_KEY] = VEDTAK_ATTESTERT.name
        return true
    }

    companion object {
        val ikrafttredenEtterlattereformen: LocalDate = LocalDate.of(2024, Month.JANUARY, 1)
    }
}
