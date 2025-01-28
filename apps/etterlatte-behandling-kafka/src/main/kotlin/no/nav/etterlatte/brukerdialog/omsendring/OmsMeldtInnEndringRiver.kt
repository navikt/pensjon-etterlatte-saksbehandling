package no.nav.etterlatte.brukerdialog.omsendring

import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brukerdialog.omsendring.OmsMeldtInnEndringHendelseKeys.HENDELSE_KEY
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class OmsMeldtInnEndringRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
    private val journalfoerService: JournalfoerOmsMeldtInnEndringService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmsMeldtInnEndringHendelsetype.EVENT_NAME) {
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
                        oppgaveKilde = OppgaveKilde.HENDELSE, // TODO
                        oppgaveType = OppgaveType.GENERELL_OPPGAVE,
                        merknad = "",
                        referanse = journalpostResponse.journalpostId,
                        frist = null,
                    ),
            )
        } catch (e: Exception) {
            // Selvbetjening-backend vil fortsette å sende nye meldinger til dette ikke feiler
            logger.error(
                "Journalføring eller opprettelse av oppgave for meldt inn endring for omstillingstønad med id =${endringer.id}",
                e,
            )
        }
    }

    private fun JsonMessage.omsMeldtInnEndringer(): OmsMeldtInnEndring =
        objectMapper.treeToValue<OmsMeldtInnEndring>(this[OmsMeldtInnEndringHendelseKeys.INNHOLD_KEY])
}

object OmsMeldtInnEndringHendelseKeys {
    const val HENDELSE_KEY = "oms_meldt_inn_endring"
    const val INNHOLD_KEY = "@oms_meldt_inn_endring_innhold"
}

enum class OmsMeldtInnEndringHendelsetype(
    val eventname: String,
) : EventnameHendelseType {
    EVENT_NAME(HENDELSE_KEY),
    ;

    override fun lagEventnameForType(): String = this.eventname
}

//  TODO i lib
data class OmsMeldtInnEndring(
    val id: UUID = UUID.randomUUID(),
    val fnr: String,
    val type: OmsEndringType,
    val endringer: String,
    val tidspunkt: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS),
)

enum class OmsEndringType {
    INNTEKT,
    INNTEKT_OG_AKTIVITET,
    ANNET,
}
