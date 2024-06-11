package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.common.rapidsandrivers.feilmelding
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.KONTEKST_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory
import java.util.UUID

internal class FeilendeMigreringLytterRiver(
    rapidsConnection: RapidsConnection,
    private val repository: PesysRepository,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.FEILA) {
            validate { it.interestedIn(FEILENDE_STEG) }
            validate { it.requireKey(KILDE_KEY) }
            validate { it.requireAny(KONTEKST_KEY, listOf(Kontekst.MIGRERING.name, Kontekst.VENT.name)) }
            validate { it.requireAny(KILDE_KEY, listOf(Vedtaksloesning.PESYS.name, Vedtaksloesning.GJENOPPRETTA.name)) }
            validate { it.interestedIn("vedtak.behandlingId") }
            validate { it.interestedIn(BEHANDLING_ID_KEY) }
            validate { it.interestedIn(PESYS_ID_KEY) }
            validate { it.interestedIn(HENDELSE_DATA_KEY) }
            validate { it.requireKey(FEILMELDING_KEY) }
            validate {
                it.rejectValues(
                    FEILENDE_STEG,
                    listOf(
                        "AvbrytBehandlingHvisMigreringFeilaRiver",
                    ),
                )
            }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val pesyskopling = finnPesysId(packet)

        logger.warn("Migrering av pesyssak $pesyskopling feila")

        if (pesyskopling.first == null) {
            return
        }
        repository.lagreFeilkjoering(
            request = "Har ikke requestobjektet tilgjengelig for logging",
            feilendeSteg = packet.feilendeSteg,
            feil = packet.feilmelding,
            pesysId = pesyskopling.first!!,
        )

        val nyStatus =
            if (packet.feilendeSteg == "MigreringTrygdetidHendelserRiver") {
                Migreringsstatus.TRYGDETID_FEILA
            } else {
                Migreringsstatus.MIGRERING_FEILA
            }
        repository.oppdaterStatus(pesyskopling.first!!, nyStatus)
        packet.setEventNameForHendelseType(Migreringshendelser.AVBRYT_BEHANDLING)
        pesyskopling.second?.let {
            packet.behandlingId = it
            context.publish(packet.toJson())
        }
    }

    private fun finnPesysId(packet: JsonMessage): Pair<PesysId?, UUID?> =
        if (packet.harVerdi(PESYS_ID_KEY)) {
            repository.hentKoplingTilBehandling(packet.pesysId)?.let { Pair(it.pesysId, it.behandlingId) } ?: Pair(
                packet.pesysId,
                null,
            )
        } else if (packet.harVerdi(BEHANDLING_ID_KEY)) {
            repository.hentPesysId(packet.behandlingId)!!.let { Pair(it.pesysId, it.behandlingId) }
        } else if (packet.harVerdi("vedtak.behandlingId")) {
            val id = packet["vedtak.behandlingId"].asText().toUUID()
            repository
                .hentPesysId(id)
                ?.let { Pair(it.pesysId, it.behandlingId) }
                ?: Pair(null, id).also { logger.warn("Mangler pesys-identifikator for behandling $id") }
        } else {
            throw IllegalArgumentException("Manglar pesys-identifikator, kan ikke kjøre feilhåndtering")
        }
}

private fun JsonMessage.harVerdi(key: String) = this[key].toString().isNotEmpty()
