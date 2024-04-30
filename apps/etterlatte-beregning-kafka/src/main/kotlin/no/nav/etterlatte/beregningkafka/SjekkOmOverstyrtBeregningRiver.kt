package no.nav.etterlatte.beregningkafka

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import no.nav.etterlatte.rapidsandrivers.tilbakestilteBehandlinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class SjekkOmOverstyrtBeregningRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.LOEPENDE_YTELSE_FUNNET) {
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.interestedIn(TILBAKESTILTE_BEHANDLINGER_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt sjekk om finnes åpen behandling med overstyrt beregning hendelse")

        packet.tilbakestilteBehandlinger.forEach {
            val overstyrt = beregningService.hentOverstyrt(it)
            when (overstyrt.status) {
                HttpStatusCode.NoContent -> return@forEach
                HttpStatusCode.OK -> throw KanIkkeRegulereSakMedAapenBehandlingOverstyrtBeregning()
                else -> throw KanIkkeBekrefteAtSakIkkeHarOverstyrtBeregning()
            }
        }

        packet.setEventNameForHendelseType(ReguleringHendelseType.UTFORT_SJEKK_AAPEN_OVERSTYRT)
        context.publish(packet.toJson())
        logger.info("Publiserte utført sjekk om sak har åpen behandling med overstyrt beregning")
    }
}

class KanIkkeRegulereSakMedAapenBehandlingOverstyrtBeregning :
    Exception("Kan ikke regulere sak med åpen behandling som er overstyrt beregning")

class KanIkkeBekrefteAtSakIkkeHarOverstyrtBeregning :
    Exception("Fikk ikke bekreftet at sak ikke har åpen behandling som er overstyrt beregning")
