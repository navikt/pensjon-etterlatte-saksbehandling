package no.nav.etterlatte.gyldigsoeknad

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.JournalfoerSoeknadService
import no.nav.etterlatte.gyldigsoeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.sikkerLogg
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class NySoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
    private val journalfoerSoeknadService: JournalfoerSoeknadService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(NySoeknadRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT) {
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoTypeKey) }
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.requireKey(SoeknadInnsendt.templateKey) }
            validate { it.requireKey(SoeknadInnsendt.lagretSoeknadIdKey) }
            validate { it.requireKey(SoeknadInnsendt.hendelseGyldigTilKey) }
            validate { it.requireKey(SoeknadInnsendt.fnrSoekerKey) }
            validate { it.rejectKey(SoeknadInnsendt.adressebeskyttelseKey) }
            validate { it.rejectKey(SoeknadInnsendt.dokarkivReturKey) }
            validate { it.rejectKey(GyldigSoeknadVurdert.sakIdKey) }
            validate { it.rejectKey(FordelerFordelt.soeknadFordeltKey) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val soeknadId = packet.soeknadId()

        try {
            logger.info("Ny søknad mottatt (id=$soeknadId)")

            val soeknad = packet.soeknad()
            val soekerFnr = packet[SoeknadInnsendt.fnrSoekerKey].textValue()

            val sakType =
                when (soeknad.type) {
                    SoeknadType.BARNEPENSJON -> SakType.BARNEPENSJON
                    SoeknadType.OMSTILLINGSSTOENAD -> SakType.OMSTILLINGSSTOENAD
                }

            if (!Folkeregisteridentifikator.isValid(soekerFnr)) {
                logger.info("Søkeren på søknad=$soeknadId har ugyldig fødselsnummer – sendes til manuell behandling")
                journalfoerSoeknadService.opprettJournalpostForUkjent(soeknadId, sakType, soeknad)?.also {
                    context.publish(packet.oppdaterMed(null, it).toJson())
                }
                return
            }

            val sak = finnEllerOpprettSak(soekerFnr, sakType)

            if (sak == null) {
                logger.warn("Kan ikke journalføre søknad (id=$soeknadId) uten sak. Retry kjøres automatisk...")
                return
            }

            val journalpostResponse = journalfoerSoeknadService.opprettJournalpost(soeknadId, sak, soeknad)

            if (journalpostResponse == null) {
                logger.warn("Kan ikke fortsette uten respons fra dokarkiv. Retry kjøres automatisk...")
                return
            } else {
                context.publish(packet.oppdaterMed(sak.id, journalpostResponse).toJson())
            }
        } catch (e: JsonMappingException) {
            sikkerLogg.error("Feil under deserialisering", e)
            logger.error("Feil under deserialisering av søknad (id=$soeknadId). Se sikkerlogg for detaljer.")
            throw e
        } catch (e: Exception) {
            logger.error("Uhåndtert feilsituasjon soeknadId: $soeknadId", e)
            throw e
        }
    }

    private fun finnEllerOpprettSak(
        fnr: String,
        sakType: SakType,
    ): Sak? =
        try {
            logger.info("Henter/oppretter sak ($sakType) i Gjenny")

            runBlocking {
                behandlingKlient.finnEllerOpprettSak(fnr, sakType)
            }
        } catch (e: ResponseException) {
            logger.error("Avbrutt fordeling - kunne ikke hente eller opprette sak: ${e.message}")

            // Svelg slik at Innsendt søknad vil retrye
            null
        }

    private fun JsonMessage.oppdaterMed(
        sakId: Long?,
        journalpostResponse: OpprettJournalpostResponse,
    ): JsonMessage =
        this.apply {
            correlationId = getCorrelationId()

            if (sakId != null) {
                this[GyldigSoeknadVurdert.sakIdKey] = sakId
            }

            this[FordelerFordelt.soeknadFordeltKey] = sakId != null
            this[GyldigSoeknadVurdert.dokarkivReturKey] = journalpostResponse
        }

    private fun JsonMessage.soeknad() = objectMapper.treeToValue<InnsendtSoeknad>(this[SoeknadInnsendt.skjemaInfoKey])

    private fun JsonMessage.soeknadId(): Long {
        val longValue = get(SoeknadInnsendt.lagretSoeknadIdKey).longValue()
        return if (longValue != 0L) {
            longValue
        } else {
            System.currentTimeMillis() // Slik at det gjøres en ny fordeling for testdata-søknader
        }
    }
}
