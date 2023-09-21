package migrering

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.common.rapidsandrivers.feilmelding
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.migrering.ListenerMedLogging

internal class FeilendeMigreringLytter(rapidsConnection: RapidsConnection, private val repository: PesysRepository) :
    ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(EventNames.FEILA)
            validate { it.requireKey(FEILENDE_STEG) }
            validate { it.requireKey(KILDE_KEY) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
            validate { it.requireKey(PESYS_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(FEILMELDING) }
            validate { it.rejectValue(FEILENDE_STEG, Migreringshendelser.VERIFISER) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.warn("Migrering av pesyssak ${packet.pesysId} feila")

        val feil = packet.feilmelding
        repository.lagreFeilkjoering(packet.hendelseData, feil = feil.toJson(), feilendeSteg = packet.feilendeSteg)
        repository.oppdaterStatus(packet.pesysId, Migreringsstatus.MIGRERING_FEILA)
    }
}
