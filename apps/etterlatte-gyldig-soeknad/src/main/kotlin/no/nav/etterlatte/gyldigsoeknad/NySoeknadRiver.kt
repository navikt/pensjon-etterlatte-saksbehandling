package no.nav.etterlatte.gyldigsoeknad

import com.fasterxml.jackson.databind.JsonMappingException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.sikkerLogg
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class NySoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(NySoeknadRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT) {
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoTypeKey) }
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.requireKey(SoeknadInnsendt.templateKey) }
            validate { it.requireKey(SoeknadInnsendt.lagretSoeknadIdKey) }
            validate { it.requireKey(SoeknadInnsendt.hendelseGyldigTilKey) }
            validate { it.requireKey(SoeknadInnsendt.adressebeskyttelseKey) }
            validate { it.requireKey(SoeknadInnsendt.fnrSoekerKey) }
            validate { it.rejectKey(SoeknadInnsendt.dokarkivReturKey) }
            validate { it.rejectKey(GyldigSoeknadVurdert.sakIdKey) }
            validate { it.rejectKey(FordelerFordelt.soeknadFordeltKey) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            logger.info("Ny søknad mottatt (id=${packet.soeknadId()})")

            val soekerFnr = packet[SoeknadInnsendt.fnrSoekerKey].textValue()
            val soeknadType = SoeknadType.valueOf(packet[GyldigSoeknadVurdert.skjemaInfoTypeKey].textValue())

            val sakType =
                when (soeknadType) {
                    SoeknadType.BARNEPENSJON -> SakType.BARNEPENSJON
                    SoeknadType.OMSTILLINGSSTOENAD -> SakType.OMSTILLINGSSTOENAD
                }

            logger.info("Soknad ${packet.soeknadId()} er gyldig for fordeling, henter sakId for Gjenny")
            hentSakId(soekerFnr, sakType)?.let { sakId ->
                packet.leggPaaSakId(sakId)
                context.publish(packet.leggPaaFordeltStatus(true).toJson())
            }
        } catch (e: JsonMappingException) {
            sikkerLogg.error("Feil under deserialisering", e)
            logger.error(
                "Feil under deserialisering av søknad, soeknadId: ${packet.soeknadId()}" +
                    ". Sjekk sikkerlogg for detaljer.",
            )
            throw e
        } catch (e: Exception) {
            logger.error("Uhåndtert feilsituasjon soeknadId: ${packet.soeknadId()}", e)
            throw e
        }
    }

    private fun hentSakId(
        fnr: String,
        sakType: SakType,
    ): Long? {
        return try {
            // Denne har ansvaret for å sette gradering
            runBlocking {
                behandlingKlient.finnEllerOpprettSak(fnr, sakType).id
            }
        } catch (e: ResponseException) {
            logger.error("Avbrutt fordeling - kunne ikke hente sakId: ${e.message}")

            // Svelg slik at Innsendt søknad vil retrye
            null
        }
    }

    private fun JsonMessage.leggPaaFordeltStatus(fordelt: Boolean): JsonMessage {
        this[FordelerFordelt.soeknadFordeltKey] = fordelt
        correlationId = getCorrelationId()
        return this
    }

    private fun JsonMessage.leggPaaSakId(sakId: Long): JsonMessage =
        this.apply {
            this[GyldigSoeknadVurdert.sakIdKey] = sakId
        }

    private fun JsonMessage.soeknadId(): Long {
        val longValue = get(SoeknadInnsendt.lagretSoeknadIdKey).longValue()
        return if (longValue != 0L) {
            longValue
        } else {
            System.currentTimeMillis() // Slik at det gjøres en ny fordeling for testdata-søknader
        }
    }
}
