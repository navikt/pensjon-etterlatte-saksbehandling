package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.common.rapidsandrivers.feilmelding
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLogging

internal class FeilendeMigreringLytter(rapidsConnection: RapidsConnection, private val repository: PesysRepository) :
    ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(EventNames.FEILA)
            validate { it.interestedIn(FEILENDE_STEG) }
            validate { it.requireKey(KILDE_KEY) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
            validate { it.interestedIn("vedtak.behandling.id") }
            validate { it.interestedIn(BEHANDLING_ID_KEY) }
            validate { it.interestedIn(PESYS_ID_KEY) }
            validate { it.interestedIn(HENDELSE_DATA_KEY) }
            validate { it.requireKey(FEILMELDING_KEY) }
            validate { it.rejectValue(FEILENDE_STEG, Migreringshendelser.VERIFISER) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val pesysId = finnPesysId(packet)

        logger.warn("Migrering av pesyssak $pesysId feila")
        repository.lagreFeilkjoering(
            request =
                packet.takeIf { it.harVerdi(HENDELSE_DATA_KEY) }?.hendelseData?.toJson()
                    ?: "Har ikke requestobjektet tilgjengelig for logging",
            feilendeSteg = packet.feilendeSteg,
            feil = packet.feilmelding,
            pesysId = pesysId,
        )
        repository.oppdaterStatus(pesysId, Migreringsstatus.MIGRERING_FEILA)
    }

    private fun finnPesysId(packet: JsonMessage): PesysId =
        if (packet.harVerdi(PESYS_ID_KEY)) {
            packet.pesysId
        } else if (packet.harVerdi(BEHANDLING_ID_KEY)) {
            repository.hentPesysId(packet.behandlingId)!!.pesysId
        } else if (packet.harVerdi("vedtak.behandling.id")) {
            repository.hentPesysId(packet["vedtak.behandling.id"].asText().toUUID())!!.pesysId
        } else {
            throw IllegalArgumentException("Manglar pesys-identifikator, kan ikke kjøre feilhåndtering")
        }
}

private fun JsonMessage.harVerdi(key: String) = this[key].toString().isNotEmpty()
