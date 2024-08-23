package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendt
import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendtHendelseType
import no.nav.etterlatte.libs.common.inntektsjustering.Inntektsjustering
import no.nav.etterlatte.libs.common.objectMapper
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
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        val inntektsjustering = packet.inntektsjustering()
        try {
            logger.info("Mottatt inntektsjustering (id=${inntektsjustering.id})")

            val fnr = packet[InntektsjusteringInnsendt.fnrBruker].textValue()
            val sak =
                runBlocking {
                    behandlingKlient.finnEllerOpprettSak(fnr, SakType.OMSTILLINGSSTOENAD)
                }

            val inntektsaar = packet[InntektsjusteringInnsendt.inntektsaar].textValue()

            val journalpostResponse =
                journalfoerInntektsjusteringService.opprettJournalpost(
                    sak,
                    inntektsaar,
                    inntektsjustering,
                )

            if (journalpostResponse == null) {
                logger.warn("Kan ikke fortsette uten respons fra dokarkiv. Retry kjøres automatisk...")
                return
            } else {
                behandlingKlient.opprettOppgave(
                    sak.id,
                    NyOppgaveDto(
                        OppgaveKilde.BRUKERDIALOG,
                        OppgaveType.GENERELL_OPPGAVE,
                        merknad = "Mottatt inntektsjustering",
                        referanse = journalpostResponse.journalpostId,
                    ),
                )
            }
        } catch (e: JsonMappingException) {
            sikkerLogg.error("Feil under deserialisering", e)
            logger.error("Feil under deserialisering av inntektsjustering (id=${inntektsjustering.id}). Se sikkerlogg for detaljer.")
            throw e
        } catch (e: Exception) {
            logger.error("Uhåndtert feilsituasjon TODO : $", e)
            throw e
        }
    }

    private fun JsonMessage.inntektsjustering(): Inntektsjustering =
        objectMapper.readValue<Inntektsjustering>(this[InntektsjusteringInnsendt.inntektsjusteringInnhold].textValue())
}
