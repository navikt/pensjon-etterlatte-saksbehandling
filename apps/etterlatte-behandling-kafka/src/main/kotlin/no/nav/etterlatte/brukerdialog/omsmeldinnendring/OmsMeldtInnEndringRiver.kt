package no.nav.etterlatte.brukerdialog.omsmeldinnendring

import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brukerdialog.omsmeldinnendring.OmsMeldtInnEndringHendelseKeys.HENDELSE_KEY
import no.nav.etterlatte.brukerdialog.omsmeldinnendring.OmsMeldtInnEndringHendelseKeys.MOTTAK_FULLFOERT_KEY
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.omsmeldinnendring.OmsEndring
import no.nav.etterlatte.libs.common.omsmeldinnendring.OmsMeldtInnEndring
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory
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
                    behandlingKlient.finnEllerOpprettSak(endringer.fnr.value, SakType.OMSTILLINGSSTOENAD)
                }

            val journalpostResponse =
                journalfoerService.opprettJournalpost(sak, endringer)
                    ?: run {
                        logger.warn("Kan ikke fortsette uten respons fra dokarkiv. Retry kjøres automatisk...")
                        return
                    }

            val oppgaverForJournalpost = behandlingKlient.finnOppgaverForReferanse(journalpostResponse.journalpostId)
            if (oppgaverForJournalpost.any { it.type == OppgaveType.MELDT_INN_ENDRING }) {
                logger.error(
                    "Vi har allerede opprettet en meld inn endring-oppgave for journalposten til endringen, " +
                        "lager ikke en ny oppgave. Gjelder journalpost=${journalpostResponse.journalpostId} i sak=${sak.id}. " +
                        "Sjekk om det er riktig match mellom eventname på mottatt-hendelse, og prøv en redeploy av " +
                        "selvbetjening-backend og behandling-kafka (løste problemet en annen gang).",
                )
            } else {
                behandlingKlient.opprettOppgave(
                    sakId = sak.id,
                    oppgave =
                        NyOppgaveDto(
                            oppgaveKilde = OppgaveKilde.BRUKERDIALOG_SELVBETJENING,
                            oppgaveType = OppgaveType.MELDT_INN_ENDRING,
                            merknad = mapEndringTilLesbarString(omsEndring = endringer.endring),
                            referanse = journalpostResponse.journalpostId,
                            frist = null,
                        ),
                )
            }
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
        meldtInnEndringId: UUID,
    ) {
        logger.info("Mottakk av meldt inn endring fullført, sender melding til selvbetjening sak=$sakId")
        val correlationId = getCorrelationId()
        val hendelsetype = OmsMeldtInnEndringHendelsetype.MOTTAK_FULLOERT.lagEventnameForType()

        rapidsConnection
            .publish(
                "mottak-meld-inn-endring-fullfoert-$sakId",
                JsonMessage
                    .newMessage(
                        hendelsetype,
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            "meldt_inn_endring_id" to meldtInnEndringId,
                        ),
                    ).toJson(),
            ).also {
                logger.info("Publiserte $hendelsetype for $sakId")
            }
    }

    private fun JsonMessage.omsMeldtInnEndringer(): OmsMeldtInnEndring =
        objectMapper.treeToValue<OmsMeldtInnEndring>(this[OmsMeldtInnEndringHendelseKeys.INNHOLD_KEY])

    private fun mapEndringTilLesbarString(omsEndring: OmsEndring): String =
        when (omsEndring) {
            OmsEndring.INNTEKT -> "Inntekt"
            OmsEndring.AKTIVITET_OG_INNTEKT -> "Aktivitet og inntekt"
            OmsEndring.SVAR_PAA_ETTEROPPGJOER -> "Svar på etteroppgjør"
            OmsEndring.FORVENTET_INNTEKT_TIL_NESTE_AAR -> "Forventet inntekt til neste år"
            OmsEndring.ANNET -> "Annet"
        }
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
    MOTTAK_FULLOERT(MOTTAK_FULLFOERT_KEY),
    ;

    override fun lagEventnameForType(): String = this.eventname
}
