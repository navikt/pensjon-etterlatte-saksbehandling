package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.MigrerSoekerRequest
import no.nav.etterlatte.libs.common.behandling.MigreringRespons
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PERSONGALLERI_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.ROLLE_KEY
import no.nav.etterlatte.rapidsandrivers.oppgaveId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.Kontekst

internal class MigrerEnEnkeltSakRiver(
    rapidsConnection: RapidsConnection,
    private val behandlinger: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.MIGRER_SAK) {
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(FNR_KEY) }
            validate { it.rejectKey(OPPLYSNING_KEY) }
        }
    }

    override fun kontekst() = Kontekst.MIGRERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt migreringshendelse")

        val hendelse: MigreringRequest = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
        if (!hendelse.kanAutomatiskGjenopprettes) {
            behandlinger.opprettOppgaveManuellGjenoppretting(hendelse)
            return
        }

        val migreringRespons: MigreringRespons =
            runBlocking {
                try {
                    behandlinger.migrer(hendelse).body<MigreringRespons>()
                } catch (e: ClientRequestException) {
                    if (e.response.status == HttpStatusCode.Conflict) {
                        logger.warn(
                            "Behandling er allerede oppretta for pesysid ${hendelse.pesysId} i Gjenny. " +
                                "Trenger ikke gjøre mer, så avbryter.",
                        )
                        packet.setEventNameForHendelseType(Migreringshendelser.ALLEREDE_GJENOPPRETTA)
                        context.publish(packet.toJson())
                        return@runBlocking null
                    }
                    throw e
                }
            } ?: return

        packet.behandlingId = migreringRespons.behandlingId
        packet.sakId = migreringRespons.sakId
        packet.oppgaveId = migreringRespons.oppgaveId

        packet[SAK_TYPE_KEY] = SakType.BARNEPENSJON
        packet[ROLLE_KEY] = PersonRolle.AVDOED
        packet[MIGRERING_GRUNNLAG_KEY] =
            MigrerSoekerRequest(
                soeker = hendelse.soeker.value,
            )
        packet[PERSONGALLERI_KEY] = hendelse.opprettPersongalleri()
        packet.setEventNameForHendelseType(Migreringshendelser.LAGRE_KOPLING)

        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert migreringshendelse")
    }
}
