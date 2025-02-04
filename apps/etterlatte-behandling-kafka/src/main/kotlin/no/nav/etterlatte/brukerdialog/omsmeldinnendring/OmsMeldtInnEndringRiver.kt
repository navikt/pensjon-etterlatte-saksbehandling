package no.nav.etterlatte.brukerdialog.omsmeldinnendring

import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brukerdialog.omsmeldinnendring.OmsMeldtInnEndringHendelseKeys.HENDELSE_KEY
import no.nav.etterlatte.brukerdialog.omsmeldinnendring.OmsMeldtInnEndringHendelseKeys.MOTTAK_FULLFOERT_KEY
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class OmsMeldtInnEndringRiver(
    val rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
    private val journalfoerService: JournalfoerOmsMeldtInnEndringService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmsMeldtInnEndringHendelsetype.MOTTATT) {
            validate { it.requireKey(OmsMeldtInnEndringHendelseKeys.INNHOLD_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val endringer = packet.omsMeldtInnEndringer()

        try {
            val sak =
                runBlocking {
                    behandlingKlient.finnEllerOpprettSak(endringer.fnr, SakType.OMSTILLINGSSTOENAD)
                }

            val journalpostResponse =
                journalfoerService.opprettJournalpost(sak, endringer)
                    ?: run {
                        logger.warn("Kan ikke fortsette uten respons fra dokarkiv. Retry kjøres automatisk...")
                        return
                    }

            behandlingKlient.opprettOppgave(
                sakId = sak.id,
                oppgave =
                    NyOppgaveDto(
                        oppgaveKilde = OppgaveKilde.BRUKERDIALOG_SELVBETJENING,
                        oppgaveType = OppgaveType.MELDT_INN_ENDRING,
                        merknad = "Endring meldt inn fra selvbetjening skjema",
                        referanse = journalpostResponse.journalpostId,
                        frist = null,
                    ),
            )

            mottatMeldtInnEndringFullfoert(sak.id, endringer.id)
        } catch (e: Exception) {
            // Selvbetjening-backend vil fortsette å sende nye meldinger til dette ikke feiler
            logger.error(
                "Journalføring eller opprettelse av oppgave for meldt inn endring for omstillingstønad med id =${endringer.id}",
                e,
            )
        }
    }

    private fun mottatMeldtInnEndringFullfoert(
        sakId: SakId,
        meldtInnEndringId: UUID
    ) {
        logger.info("Mottakk av meldt inn endring fullført, sender melding til selvbetjening sak=$sakId")
        val correlationId = getCorrelationId()
        val hendelsetype = OmsMeldtInnEndringHendelsetype.MOTTAK_FULLOERT.lagEventnameForType()

        rapidsConnection.publish(
            "mottak-meld-inn-endring-fullfoert-$sakId",
            JsonMessage
                .newMessage(
                    hendelsetype,
                    mapOf(
                        CORRELATION_ID_KEY to correlationId,
                        TEKNISK_TID_KEY to Tidspunkt.now(),
                        "meldt_inn_endring_id" to meldtInnEndringId
                    )
                ).toJson()
        ).also {
            logger.info("Publiserte $hendelsetype for $sakId")
        }
    }

    private fun JsonMessage.omsMeldtInnEndringer(): OmsMeldtInnEndring =
        objectMapper.treeToValue<OmsMeldtInnEndring>(this[OmsMeldtInnEndringHendelseKeys.INNHOLD_KEY])
}

object OmsMeldtInnEndringHendelseKeys {
    const val HENDELSE_KEY = "oms_meldt_inn_endring"
    const val INNHOLD_KEY = "@oms_meldt_inn_endring_innhold"
    const val MOTTAK_FULLFOERT_KEY = "oms_meldt_inn_endring_mottak_fullført"
}

enum class OmsMeldtInnEndringHendelsetype(
    val eventname: String,
) : EventnameHendelseType {
    MOTTATT(HENDELSE_KEY),
    MOTTAK_FULLOERT(MOTTAK_FULLFOERT_KEY)
    ;

    override fun lagEventnameForType(): String = this.eventname
}

//  TODO i lib
data class OmsMeldtInnEndring(
    val id: UUID = UUID.randomUUID(),
    val fnr: String,
    val endring: OmsEndring,
    val beskrivelse: String,
    val tidspunkt: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS),
)

enum class OmsEndring {
    INNTEKT,
    AKTIVITET_OG_INNTEKT,
    ANNET,
}
