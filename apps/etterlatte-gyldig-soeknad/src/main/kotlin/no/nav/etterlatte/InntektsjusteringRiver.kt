package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonMappingException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendt
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendtHendelseType
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class InntektsjusteringRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
    private val journalfoerInntektsjusteringService: JournalfoerInntektsjusteringService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(InntektsjusteringRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringInnsendtHendelseType.EVENT_NAME_INNSENDT) {
            validate { it.requireKey(InntektsjusteringInnsendt.inntektsaar) }
            validate { it.requireKey(InntektsjusteringInnsendt.fnrBruker) }
            validate { it.requireKey(InntektsjusteringInnsendt.inntektsjusteringInnhold) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            logger.info("Mottatt inntektsjustering (id=)")

            val fnr = packet[InntektsjusteringInnsendt.fnrBruker].textValue()
            val sak =
                runBlocking {
                    behandlingKlient.finnEllerOpprettSak(fnr, SakType.OMSTILLINGSSTOENAD)
                }

            val inntektsjustering = packet[InntektsjusteringInnsendt.inntektsjusteringInnhold].textValue()

            journalfoerInntektsjusteringService.opprettJournalpost(
                sak,
                inntektsjustering,
            )

            behandlingKlient.opprettOppgave(
                sak.id,
                NyOppgaveDto(
                    OppgaveKilde.BRUKERDIALOG,
                    OppgaveType.GENERELL_OPPGAVE,
                    merknad = "Mottatt inntektsjustering",
                ),
            )
        } catch (e: JsonMappingException) {
            sikkerLogg.error("Feil under deserialisering", e)
            logger.error("Feil under deserialisering av inntektsjustering (id=$ TODO). Se sikkerlogg for detaljer.")
            throw e
        } catch (e: Exception) {
            logger.error("Uhåndtert feilsituasjon TODO : $", e)
            throw e
        }
    }
}
